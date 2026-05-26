package com.weekendtravel.backend.plan;

import com.weekendtravel.backend.b2.tool.AvailabilityRequest;
import com.weekendtravel.backend.b2.tool.AvailabilityResult;
import com.weekendtravel.backend.b2.tool.AvailabilityTool;
import com.weekendtravel.backend.b2.tool.MessageActionSummary;
import com.weekendtravel.backend.b2.tool.MessagePlanPayload;
import com.weekendtravel.backend.b2.tool.MessageRequest;
import com.weekendtravel.backend.b2.tool.MessageResult;
import com.weekendtravel.backend.b2.tool.MessageTimeSlot;
import com.weekendtravel.backend.b2.tool.MessageTool;
import com.weekendtravel.backend.b2.tool.RouteRequest;
import com.weekendtravel.backend.b2.tool.RouteResult;
import com.weekendtravel.backend.b2.tool.RouteTool;
import com.weekendtravel.backend.b2.tool.SearchCandidate;
import com.weekendtravel.backend.b2.tool.SearchRequest;
import com.weekendtravel.backend.b2.tool.SearchResult;
import com.weekendtravel.backend.b2.tool.SearchTool;
import com.weekendtravel.backend.plan.api.CreatePlanRequest;
import com.weekendtravel.backend.plan.sse.PlanReadyEvent;
import com.weekendtravel.backend.plan.sse.ReplanEvent;
import com.weekendtravel.backend.plan.sse.StateChangeEvent;
import com.weekendtravel.backend.plan.sse.ToolCallEvent;
import com.weekendtravel.backend.plan.sse.ToolResultEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PlanStateMachineService {

    private static final int FAMILY_MIN_AGE = 5;
    private static final int FAMILY_GROUP_SIZE = 3;
    private static final int MAX_DISTANCE_MINUTES = 20;
    private static final int ROUTE_TOO_FAR_MINUTES = 25;
    private static final String SLOT = "weekendAfternoon";
    private static final String STATUS_CONFIRM = "CONFIRM";

    private final SearchTool searchTool;
    private final RouteTool routeTool;
    private final AvailabilityTool availabilityTool;
    private final MessageTool messageTool;
    private final Map<String, PlanContext> plans = new ConcurrentHashMap<>();

    public PlanStateMachineService(
            SearchTool searchTool,
            RouteTool routeTool,
            AvailabilityTool availabilityTool,
            MessageTool messageTool
    ) {
        this.searchTool = searchTool;
        this.routeTool = routeTool;
        this.availabilityTool = availabilityTool;
        this.messageTool = messageTool;
    }

    public PlanContext createPlan(String planId, CreatePlanRequest request) {
        PlanContext context = new PlanContext(planId, request, 0, null);
        plans.put(planId, context);
        return context;
    }

    public void streamPlan(String planId, SseEmitter emitter) throws IOException {
        PlanContext context = requirePlan(planId);
        StateCursor cursor = new StateCursor(PlanState.START);

        transition(cursor, emitter, planId, PlanState.INTENT);
        transition(cursor, emitter, planId, PlanState.SKELETON);
        transition(cursor, emitter, planId, PlanState.RECALL);

        SearchSelection selection = selectCandidates(planId, context.request(), emitter);

        transition(cursor, emitter, planId, PlanState.VALIDATE);
        ValidationDecision decision = validateSelection(planId, selection, emitter);
        if (!decision.valid()) {
            context = incrementReplan(planId);
            sendReplan(emitter, new ReplanEvent(
                    "replan",
                    planId,
                    decision.reason(),
                    context.replanCount(),
                    System.currentTimeMillis()
            ));
            transition(cursor, emitter, planId, PlanState.REPLAN);
            transition(cursor, emitter, planId, PlanState.RECALL);
            selection = selection.withRestaurant(searchFallbackRestaurant(selection));
            transition(cursor, emitter, planId, PlanState.VALIDATE);
            decision = validateSelection(planId, selection, emitter);
        }

        transition(cursor, emitter, planId, PlanState.PACK);
        MessagePlanPayload packedPlan = buildPackedPlan(planId, context.request(), selection, decision);
        plans.computeIfPresent(planId, (key, current) -> current.withPackedPlan(packedPlan));
        emitter.send(SseEmitter.event().name("plan_ready").data(new PlanReadyEvent("plan_ready", planId, packedPlan)));
    }

    private SearchSelection selectCandidates(String planId, CreatePlanRequest request, SseEmitter emitter) throws IOException {
        SearchResult activities = callSearch(planId, emitter, new SearchRequest(
                request.scenario(),
                List.of("activity"),
                familyActivityKeyword(request.text()),
                MAX_DISTANCE_MINUTES,
                FAMILY_MIN_AGE,
                FAMILY_GROUP_SIZE,
                4
        ), "搜索家庭活动候选");
        SearchResult restaurants = callSearch(planId, emitter, new SearchRequest(
                request.scenario(),
                List.of("restaurant"),
                familyRestaurantKeyword(request.text()),
                MAX_DISTANCE_MINUTES,
                FAMILY_MIN_AGE,
                FAMILY_GROUP_SIZE,
                6
        ), "搜索家庭餐厅候选");

        SearchCandidate activity = firstCandidate(activities, "activity candidates are required");
        SearchCandidate restaurant = firstCandidate(restaurants, "restaurant candidates are required");
        return new SearchSelection(activity, restaurant, restaurants.candidates());
    }

    private ValidationDecision validateSelection(String planId, SearchSelection selection, SseEmitter emitter) throws IOException {
        RouteResult route = callRoute(planId, emitter, new RouteRequest(selection.activity().poi().id(), selection.restaurant().poi().id()));
        AvailabilityResult activityAvailability = callAvailability(planId, emitter, new AvailabilityRequest(
                selection.activity().poi().id(),
                SLOT,
                FAMILY_MIN_AGE,
                FAMILY_GROUP_SIZE
        ));
        AvailabilityResult restaurantAvailability = callAvailability(planId, emitter, new AvailabilityRequest(
                selection.restaurant().poi().id(),
                SLOT,
                FAMILY_MIN_AGE,
                FAMILY_GROUP_SIZE
        ));

        if (!activityAvailability.available()) {
            return new ValidationDecision(false, "活动地点当前不可用", route, activityAvailability, restaurantAvailability);
        }
        if (!restaurantAvailability.available() || restaurantAvailability.waitMinutes() > 45) {
            return new ValidationDecision(false, "原餐厅排队预计 " + restaurantAvailability.waitMinutes() + " 分钟，已切换到可用餐厅", route, activityAvailability, restaurantAvailability);
        }
        if (route.distanceMinutes() > ROUTE_TOO_FAR_MINUTES || restaurantAvailability.scenarioFlags().routeTooFar()) {
            return new ValidationDecision(false, "原路线过远，已切换到更近餐厅", route, activityAvailability, restaurantAvailability);
        }
        return new ValidationDecision(true, null, route, activityAvailability, restaurantAvailability);
    }

    private SearchCandidate searchFallbackRestaurant(SearchSelection selection) {
        return selection.restaurantCandidates().stream()
                .filter(candidate -> !candidate.poi().id().equals(selection.restaurant().poi().id()))
                .min(Comparator.comparingInt(candidate -> candidate.poi().distanceMinutesFromCenter()))
                .orElse(selection.restaurant());
    }

    private MessagePlanPayload buildPackedPlan(
            String planId,
            CreatePlanRequest request,
            SearchSelection selection,
            ValidationDecision decision
    ) {
        boolean isPlanB = decision.reason() != null;
        List<MessageTimeSlot> timeline = List.of(
                new MessageTimeSlot(
                        1,
                        "activity",
                        selection.activity().poi().name(),
                        selection.activity().poi().name(),
                        "14:00",
                        "16:00",
                        selection.activity().poi().distanceMinutesFromCenter(),
                        List.of("适合 5 岁儿童", "室内活动，天气影响小")
                ),
                new MessageTimeSlot(
                        2,
                        "restaurant",
                        selection.restaurant().poi().name(),
                        selection.restaurant().poi().name(),
                        "16:30",
                        "17:45",
                        decision.route().distanceMinutes(),
                        restaurantNotes(decision, isPlanB)
                )
        );
        List<MessageActionSummary> actions = List.of(
                new MessageActionSummary(
                        "act_" + planId + "_reserve",
                        "reserve_table",
                        "预约 16:30 的家庭座位",
                        "pending",
                        null
                ),
                new MessageActionSummary(
                        "act_" + planId + "_message",
                        "send_message",
                        "生成家庭出行消息",
                        "pending",
                        null
                )
        );
        String summary = isPlanB
                ? "亲子活动后改去更近且可用的家庭餐厅"
                : "亲子活动搭配家庭友好晚餐";
        MessageResult message = messageTool.composeShareMessage(new MessageRequest(
                planId,
                request.scenario(),
                STATUS_CONFIRM,
                isPlanB,
                decision.reason(),
                summary,
                timeline,
                actions,
                3.8,
                isPlanB ? 1 : 0,
                OffsetDateTime.now().toString()
        ));
        return message.plan();
    }

    private List<String> restaurantNotes(ValidationDecision decision, boolean isPlanB) {
        if (isPlanB) {
            return List.of(decision.reason(), "已优先保留活动地点");
        }
        return List.of("餐厅当前可用", "路线较短，适合带孩子转场");
    }

    private SearchResult callSearch(String planId, SseEmitter emitter, SearchRequest request, String summary) throws IOException {
        sendToolCall(emitter, new ToolCallEvent("tool_call", planId, "searchLocalPlaces", "start", summary, System.currentTimeMillis()));
        SearchResult result = searchTool.searchLocalPlaces(request);
        sendToolResult(emitter, new ToolResultEvent(
                "tool_result",
                planId,
                "searchLocalPlaces",
                "success",
                "找到 " + result.candidates().size() + " 个候选",
                result.latencyMs(),
                System.currentTimeMillis()
        ));
        return result;
    }

    private RouteResult callRoute(String planId, SseEmitter emitter, RouteRequest request) throws IOException {
        sendToolCall(emitter, new ToolCallEvent("tool_call", planId, "calculateRouteTime", "start", "校验活动到餐厅路线", System.currentTimeMillis()));
        RouteResult result = routeTool.calculateRouteTime(request);
        sendToolResult(emitter, new ToolResultEvent(
                "tool_result",
                planId,
                "calculateRouteTime",
                "success",
                result.summary(),
                result.latencyMs(),
                System.currentTimeMillis()
        ));
        return result;
    }

    private AvailabilityResult callAvailability(String planId, SseEmitter emitter, AvailabilityRequest request) throws IOException {
        sendToolCall(emitter, new ToolCallEvent("tool_call", planId, "checkAvailability", "start", "检查地点余位和等待时间", System.currentTimeMillis()));
        AvailabilityResult result = availabilityTool.checkAvailability(request);
        sendToolResult(emitter, new ToolResultEvent(
                "tool_result",
                planId,
                "checkAvailability",
                "success",
                result.poiName() + " 状态: " + result.availabilityStatus(),
                result.latencyMs(),
                System.currentTimeMillis()
        ));
        return result;
    }

    private SearchCandidate firstCandidate(SearchResult result, String message) {
        return result.candidates().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(message));
    }

    private String familyActivityKeyword(String text) {
        return containsAny(text, "室内", "亲子", "孩子") ? "亲子" : "";
    }

    private String familyRestaurantKeyword(String text) {
        return containsAny(text, "排队", "晚饭", "餐厅", "吃") ? "儿童椅" : "";
    }

    private boolean containsAny(String text, String... needles) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private PlanContext incrementReplan(String planId) {
        return plans.compute(planId, (key, current) -> current == null ? null : current.withReplanCount(current.replanCount() + 1));
    }

    private PlanContext requirePlan(String planId) {
        PlanContext context = plans.get(planId);
        if (context == null) {
            throw new IllegalArgumentException("unknown planId: " + planId);
        }
        return context;
    }

    private void transition(StateCursor cursor, SseEmitter emitter, String planId, PlanState next) throws IOException {
        PlanState from = cursor.current();
        cursor.current = next;
        emitter.send(SseEmitter.event().name("state_change").data(new StateChangeEvent(
                "state_change",
                planId,
                from.name(),
                next.name(),
                System.currentTimeMillis()
        )));
    }

    private void sendToolCall(SseEmitter emitter, ToolCallEvent event) throws IOException {
        emitter.send(SseEmitter.event().name(event.type()).data(event));
    }

    private void sendToolResult(SseEmitter emitter, ToolResultEvent event) throws IOException {
        emitter.send(SseEmitter.event().name(event.type()).data(event));
    }

    private void sendReplan(SseEmitter emitter, ReplanEvent event) throws IOException {
        emitter.send(SseEmitter.event().name(event.type()).data(event));
    }

    private static final class StateCursor {
        private PlanState current;

        private StateCursor(PlanState current) {
            this.current = current;
        }

        private PlanState current() {
            return current;
        }
    }

    private record SearchSelection(
            SearchCandidate activity,
            SearchCandidate restaurant,
            List<SearchCandidate> restaurantCandidates
    ) {
        private SearchSelection withRestaurant(SearchCandidate nextRestaurant) {
            return new SearchSelection(activity, nextRestaurant, restaurantCandidates);
        }
    }

    private record ValidationDecision(
            boolean valid,
            String reason,
            RouteResult route,
            AvailabilityResult activityAvailability,
            AvailabilityResult restaurantAvailability
    ) {
    }
}
