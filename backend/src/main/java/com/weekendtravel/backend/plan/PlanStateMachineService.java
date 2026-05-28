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
import com.weekendtravel.backend.plan.sse.ErrorEvent;
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
    private static final int FRIENDS_GROUP_SIZE = 4;
    private static final int MAX_DISTANCE_MINUTES = 20;
    private static final int ROUTE_TOO_FAR_MINUTES = 25;
    private static final int MAX_REPLAN_ATTEMPTS = 2;
    private static final String SLOT = "weekendAfternoon";
    private static final String STATUS_CONFIRM = "CONFIRM";
    private static final String ERROR_CODE_DEGRADE = "DEGRADE";

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
        PlanContext context = new PlanContext(planId, request, 0, false, null, null);
        plans.put(planId, context);
        return context;
    }

    public void ensurePlanExists(String planId) {
        requirePlan(planId);
    }

    public void streamPlan(String planId, SseEmitter emitter) throws IOException {
        PlanContext context = requirePlan(planId);
        ScenarioProfile profile = scenarioProfile(context.request().scenario());
        StateCursor cursor = new StateCursor(PlanState.START);

        transition(cursor, emitter, planId, PlanState.INTENT);
        transition(cursor, emitter, planId, PlanState.SKELETON);
        transition(cursor, emitter, planId, PlanState.RECALL);

        SearchSelection selection = selectCandidates(planId, context.request(), profile, emitter);
        ValidationDecision decision = null;

        while (true) {
            transition(cursor, emitter, planId, PlanState.VALIDATE);
            decision = validateSelection(planId, selection, profile, emitter);
            if (decision.valid()) {
                break;
            }

            SearchSelection nextSelection = selectFallbackSelection(selection, decision.reason());
            if (sameSelection(selection, nextSelection) || context.replanCount() >= MAX_REPLAN_ATTEMPTS) {
                transition(cursor, emitter, planId, PlanState.DEGRADE);
                String degradeMessage = buildDegradeMessage(context.replanCount(), decision.reason());
                plans.computeIfPresent(planId, (key, current) -> current.withLatestReason(degradeMessage));
                sendError(emitter, new ErrorEvent(
                        "error",
                        planId,
                        ERROR_CODE_DEGRADE,
                        degradeMessage,
                        System.currentTimeMillis()
                ));
                return;
            }

            context = recordReplan(planId, decision.reason(), decision.injected());
            sendReplan(emitter, new ReplanEvent(
                    "replan",
                    planId,
                    decision.reason(),
                    context.replanCount(),
                    System.currentTimeMillis()
            ));
            transition(cursor, emitter, planId, PlanState.REPLAN);
            transition(cursor, emitter, planId, PlanState.RECALL);
            selection = nextSelection;
        }

        context = requirePlan(planId);
        transition(cursor, emitter, planId, PlanState.PACK);
        MessagePlanPayload packedPlan = buildPackedPlan(context, profile, selection, decision);
        plans.computeIfPresent(planId, (key, current) -> current.withPackedPlan(packedPlan));
        emitter.send(SseEmitter.event().name("plan_ready").data(new PlanReadyEvent("plan_ready", planId, packedPlan)));
    }

    private SearchSelection selectCandidates(String planId, CreatePlanRequest request, ScenarioProfile profile, SseEmitter emitter) throws IOException {
        SearchResult activities = callSearch(planId, emitter, new SearchRequest(
                request.scenario(),
                List.of("activity"),
                profile.activityKeyword(request.text()),
                MAX_DISTANCE_MINUTES,
                profile.minAge(),
                profile.groupSize(),
                4
        ), profile.activitySearchSummary());
        SearchResult restaurants = callSearch(planId, emitter, new SearchRequest(
                request.scenario(),
                List.of("restaurant"),
                profile.restaurantKeyword(request.text()),
                MAX_DISTANCE_MINUTES,
                profile.minAge(),
                profile.groupSize(),
                6
        ), profile.restaurantSearchSummary());

        SearchCandidate activity = firstCandidate(activities, "activity candidates are required");
        SearchCandidate restaurant = firstCandidate(restaurants, "restaurant candidates are required");
        return new SearchSelection(activity, restaurant, restaurants.candidates());
    }

    private ValidationDecision validateSelection(String planId, SearchSelection selection, ScenarioProfile profile, SseEmitter emitter) throws IOException {
        RouteResult route = callRoute(planId, emitter, new RouteRequest(selection.activity().poi().id(), selection.restaurant().poi().id()));
        AvailabilityResult activityAvailability = callAvailability(planId, emitter, new AvailabilityRequest(
                selection.activity().poi().id(),
                SLOT,
                profile.minAge(),
                profile.groupSize()
        ));
        AvailabilityResult restaurantAvailability = callAvailability(planId, emitter, new AvailabilityRequest(
                selection.restaurant().poi().id(),
                SLOT,
                profile.minAge(),
                profile.groupSize()
        ));

        if (!activityAvailability.available()) {
            return new ValidationDecision(false, "活动地点当前不可用", false, route, activityAvailability, restaurantAvailability);
        }
        if (!restaurantAvailability.available() || restaurantAvailability.waitMinutes() > 45) {
            return new ValidationDecision(false, "原餐厅排队预计 " + restaurantAvailability.waitMinutes() + " 分钟，已切换到可用餐厅", restaurantAvailability.scenarioFlags().restaurantFull(), route, activityAvailability, restaurantAvailability);
        }
        if (route.distanceMinutes() > ROUTE_TOO_FAR_MINUTES || restaurantAvailability.scenarioFlags().routeTooFar()) {
            return new ValidationDecision(false, "原路线过远，已切换到更近餐厅", restaurantAvailability.scenarioFlags().routeTooFar(), route, activityAvailability, restaurantAvailability);
        }
        return new ValidationDecision(true, null, false, route, activityAvailability, restaurantAvailability);
    }

    private SearchCandidate searchFallbackRestaurant(SearchSelection selection) {
        return selection.restaurantCandidates().stream()
                .filter(candidate -> !candidate.poi().id().equals(selection.restaurant().poi().id()))
                .min(Comparator.comparingInt(candidate -> candidate.poi().distanceMinutesFromCenter()))
                .orElse(selection.restaurant());
    }

    private SearchSelection selectFallbackSelection(SearchSelection selection, String reason) {
        if (reason == null || reason.isBlank()) {
            return selection;
        }
        SearchCandidate nextRestaurant = searchFallbackRestaurant(selection);
        return selection.withRestaurant(nextRestaurant);
    }

    private boolean sameSelection(SearchSelection current, SearchSelection next) {
        return current.activity().poi().id().equals(next.activity().poi().id())
                && current.restaurant().poi().id().equals(next.restaurant().poi().id());
    }

    private String buildDegradeMessage(int replanCount, String reason) {
        if (reason == null || reason.isBlank()) {
            return replanCount + " 次重排后仍无可行方案，建议放宽距离限制或调整时间";
        }
        return replanCount + " 次重排后仍无可行方案：" + reason;
    }

    private MessagePlanPayload buildPackedPlan(
            PlanContext context,
            ScenarioProfile profile,
            SearchSelection selection,
            ValidationDecision decision
    ) {
        CreatePlanRequest request = context.request();
        boolean isPlanB = context.replanCount() > 0;
        String planBReason = isPlanB ? context.latestReason() : null;
        List<MessageTimeSlot> timeline = List.of(
                new MessageTimeSlot(
                        1,
                        "activity",
                        selection.activity().poi().name(),
                        selection.activity().poi().name(),
                        profile.activityStartTime(),
                        profile.activityEndTime(),
                        selection.activity().poi().distanceMinutesFromCenter(),
                        profile.activityNotes()
                ),
                new MessageTimeSlot(
                        2,
                        "restaurant",
                        selection.restaurant().poi().name(),
                        selection.restaurant().poi().name(),
                        profile.restaurantStartTime(),
                        profile.restaurantEndTime(),
                        decision.route().distanceMinutes(),
                        restaurantNotes(profile, planBReason, isPlanB)
                )
        );
        List<MessageActionSummary> actions = List.of(
                new MessageActionSummary(
                        "act_" + context.planId() + "_reserve",
                        "reserve_table",
                        profile.reserveActionText(),
                        "pending",
                        null
                ),
                new MessageActionSummary(
                        "act_" + context.planId() + "_message",
                        "send_message",
                        profile.messageActionText(),
                        "pending",
                        null
                )
        );
        MessageResult message = messageTool.composeShareMessage(new MessageRequest(
                context.planId(),
                request.scenario(),
                STATUS_CONFIRM,
                isPlanB,
                planBReason,
                profile.summary(isPlanB),
                timeline,
                actions,
                profile.totalDurationHours(),
                context.replanCount(),
                OffsetDateTime.now().toString()
        ));
        return message.plan();
    }

    private List<String> restaurantNotes(ScenarioProfile profile, String planBReason, boolean isPlanB) {
        if (isPlanB && planBReason != null) {
            return List.of(planBReason, profile.fallbackRestaurantNote());
        }
        return profile.defaultRestaurantNotes();
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

    private ScenarioProfile scenarioProfile(String scenario) {
        if ("friends".equals(scenario)) {
            return new ScenarioProfile(
                    null,
                    FRIENDS_GROUP_SIZE,
                    "拍照",
                    "聚餐",
                    "搜索朋友活动候选",
                    "搜索朋友餐厅候选",
                    "16:00",
                    "18:00",
                    "18:20",
                    "19:40",
                    List.of("适合朋友一起放松", "兼顾拍照和聊天氛围"),
                    List.of("餐厅当前可用", "路线较短，适合朋友继续聚会"),
                    "已优先保留活动安排",
                    "预约 18:20 的 4 人座位",
                    "生成朋友聚会消息",
                    3.7,
                    "朋友活动后改去更近且可用的聚餐地点",
                    "朋友活动搭配轻松聚餐"
            );
        }
        return new ScenarioProfile(
                FAMILY_MIN_AGE,
                FAMILY_GROUP_SIZE,
                "亲子",
                "儿童椅",
                "搜索家庭活动候选",
                "搜索家庭餐厅候选",
                "14:00",
                "16:00",
                "16:30",
                "17:45",
                List.of("适合 5 岁儿童", "室内活动，天气影响小"),
                List.of("餐厅当前可用", "路线较短，适合带孩子转场"),
                "已优先保留活动地点",
                "预约 16:30 的家庭座位",
                "生成家庭出行消息",
                3.8,
                "亲子活动后改去更近且可用的家庭餐厅",
                "亲子活动搭配家庭友好晚餐"
        );
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

    private static boolean containsKeyword(String text, String... needles) {
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

    private PlanContext recordReplan(String planId, String reason, boolean injectedPlanB) {
        return plans.compute(planId, (key, current) -> current == null
                ? null
                : current.withReplanCount(current.replanCount() + 1)
                .withInjectedPlanB(current.injectedPlanB() || injectedPlanB)
                .withLatestReason(reason));
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

    private void sendError(SseEmitter emitter, ErrorEvent event) throws IOException {
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
            boolean injected,
            RouteResult route,
            AvailabilityResult activityAvailability,
            AvailabilityResult restaurantAvailability
    ) {
    }

    private record ScenarioProfile(
            Integer minAge,
            int groupSize,
            String activityKeyword,
            String restaurantKeyword,
            String activitySearchSummary,
            String restaurantSearchSummary,
            String activityStartTime,
            String activityEndTime,
            String restaurantStartTime,
            String restaurantEndTime,
            List<String> activityNotes,
            List<String> defaultRestaurantNotes,
            String fallbackRestaurantNote,
            String reserveActionText,
            String messageActionText,
            double totalDurationHours,
            String planBSummary,
            String defaultSummary
    ) {
        private String activityKeyword(String text) {
            return containsKeyword(text, "室内", "亲子", "孩子", "拍照", "朋友", "聊天") ? activityKeyword : "";
        }

        private String restaurantKeyword(String text) {
            return containsKeyword(text, "排队", "晚饭", "餐厅", "吃", "聚餐") ? restaurantKeyword : "";
        }

        private String summary(boolean isPlanB) {
            return isPlanB ? planBSummary : defaultSummary;
        }
    }
}
