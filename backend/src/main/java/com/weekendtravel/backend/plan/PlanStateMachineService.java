package com.weekendtravel.backend.plan;

import com.weekendtravel.backend.b2.tool.AvailabilityRequest;
import com.weekendtravel.backend.b2.tool.AvailabilityResult;
import com.weekendtravel.backend.b2.tool.AvailabilityTool;
import com.weekendtravel.backend.b2.tool.BookingRequest;
import com.weekendtravel.backend.b2.tool.BookingResult;
import com.weekendtravel.backend.b2.tool.BookingTool;
import com.weekendtravel.backend.b2.tool.MessageActionSummary;
import com.weekendtravel.backend.b2.tool.MessagePlanPayload;
import com.weekendtravel.backend.b2.tool.MessagePoiPayload;
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
import com.weekendtravel.backend.plan.api.AdjustPlanRequest;
import com.weekendtravel.backend.plan.api.AdjustPlanResponse;
import com.weekendtravel.backend.plan.api.ClarifyPlanRequest;
import com.weekendtravel.backend.plan.api.ClarifyPlanResponse;
import com.weekendtravel.backend.plan.api.CreatePlanRequest;
import com.weekendtravel.backend.plan.api.ExecutePlanRequest;
import com.weekendtravel.backend.plan.api.ExecutePlanResponse;
import com.weekendtravel.backend.plan.sse.AdjustResultEvent;
import com.weekendtravel.backend.plan.sse.ClarificationRequestEvent;
import com.weekendtravel.backend.plan.sse.DoneEvent;
import com.weekendtravel.backend.plan.sse.ErrorEvent;
import com.weekendtravel.backend.plan.sse.ExecuteResultEvent;
import com.weekendtravel.backend.plan.sse.PlanReadyEvent;
import com.weekendtravel.backend.plan.sse.ReplanEvent;
import com.weekendtravel.backend.plan.sse.StateChangeEvent;
import com.weekendtravel.backend.plan.sse.ToolCallEvent;
import com.weekendtravel.backend.plan.sse.ToolResultEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
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
    private static final int MAX_CLARIFY_ATTEMPTS = 2;
    private static final int MAX_ADJUST_ATTEMPTS = 3;
    private static final String SLOT = "weekendAfternoon";
    private static final String STATUS_CONFIRM = "CONFIRM";
    private static final String STATUS_DONE = "DONE";
    private static final String ACTION_RESERVE_TABLE = "reserve_table";
    private static final String ACTION_SEND_MESSAGE = "send_message";
    private static final String ERROR_CODE_DEGRADE = "DEGRADE";
    private static final String CLARIFY_FIELD_DURATION_HOURS = "durationHours";

    private final SearchTool searchTool;
    private final RouteTool routeTool;
    private final AvailabilityTool availabilityTool;
    private final BookingTool bookingTool;
    private final MessageTool messageTool;
    private final Map<String, PlanContext> plans = new ConcurrentHashMap<>();

    public PlanStateMachineService(
            SearchTool searchTool,
            RouteTool routeTool,
            AvailabilityTool availabilityTool,
            BookingTool bookingTool,
            MessageTool messageTool
    ) {
        this.searchTool = searchTool;
        this.routeTool = routeTool;
        this.availabilityTool = availabilityTool;
        this.bookingTool = bookingTool;
        this.messageTool = messageTool;
    }

    public PlanContext createPlan(String planId, CreatePlanRequest request) {
        PlanContext context = new PlanContext(
                planId,
                request,
                PlanState.START,
                0,
                0,
                0,
                false,
                null,
                null,
                null,
                null,
                null,
                false,
                false
        );
        plans.put(planId, context);
        return context;
    }

    public void ensurePlanExists(String planId) {
        requirePlan(planId);
    }

    public void streamPlan(String planId, SseEmitter emitter) throws IOException {
        PlanContext context = requirePlan(planId);
        if (context.currentState() == PlanState.CLARIFY && context.pendingClarification() != null && context.pendingClarification().reply() == null) {
            sendClarificationRequest(emitter, planId, context.pendingClarification());
            return;
        }
        if (context.currentState() == PlanState.CONFIRM && context.pendingAdjustInstruction() != null) {
            continueAdjust(planId, emitter);
            return;
        }
        if (context.currentState() == PlanState.CONFIRM && context.executionRequested()) {
            continueExecution(planId, emitter);
            return;
        }
        if (context.currentState() == PlanState.DONE && context.executionCompleted()) {
            return;
        }
        if (context.currentState() == PlanState.CONFIRM && context.packedPlan() != null) {
            emitter.send(SseEmitter.event().name("plan_ready").data(new PlanReadyEvent("plan_ready", planId, context.packedPlan())));
            return;
        }
        continuePlanning(planId, emitter);
    }

    public ClarifyPlanResponse clarifyPlan(String planId, ClarifyPlanRequest request) {
        String reply = requireText(request == null ? null : request.reply(), "reply");
        PlanContext context = requirePlan(planId);
        if (context.currentState() != PlanState.CLARIFY || context.pendingClarification() == null) {
            throw new InvalidPlanStateException("plan is not waiting for clarification");
        }
        PendingClarification updated = context.pendingClarification().withReply(reply);
        plans.computeIfPresent(planId, (key, current) -> current.withPendingClarification(updated));
        return new ClarifyPlanResponse(planId, "processing", "已收到，继续规划中");
    }

    public ExecutePlanResponse executePlan(String planId, ExecutePlanRequest request) {
        if (request == null || !Boolean.TRUE.equals(request.confirmed())) {
            throw new IllegalArgumentException("confirmed must be true");
        }
        PlanContext context = requirePlan(planId);
        if (context.currentState() != PlanState.CONFIRM || context.packedPlan() == null) {
            throw new InvalidPlanStateException("plan must be in CONFIRM state before execute");
        }
        plans.computeIfPresent(planId, (key, current) -> current
                .withExecutionRequested(true)
                .withExecutionCompleted(false));
        return new ExecutePlanResponse(planId, "executing", "开始执行，请关注右侧日志");
    }

    public AdjustPlanResponse adjustPlan(String planId, AdjustPlanRequest request) {
        String instruction = requireText(request == null ? null : request.instruction(), "instruction");
        PlanContext context = requirePlan(planId);
        if (context.adjustCount() >= MAX_ADJUST_ATTEMPTS) {
            throw new AdjustLimitExceededException("已达最大微调次数，请直接确认或重新发起规划");
        }
        if (context.currentState() != PlanState.CONFIRM || context.packedPlan() == null || context.selectionSnapshot() == null) {
            throw new InvalidPlanStateException("plan is not in CONFIRM state before adjust");
        }
        plans.computeIfPresent(planId, (key, current) -> current
                .withAdjustCount(current.adjustCount() + 1)
                .withPendingAdjustInstruction(instruction));
        return new AdjustPlanResponse(planId, "adjusting", "正在调整餐厅，请关注右侧日志");
    }

    private void continuePlanning(String planId, SseEmitter emitter) throws IOException {
        PlanContext context = requirePlan(planId);
        ScenarioProfile profile = scenarioProfile(context.request().scenario());

        if (context.currentState() == PlanState.START) {
            transition(planId, emitter, PlanState.INTENT);
            context = requirePlan(planId);
        }

        if (context.currentState() == PlanState.CLARIFY && context.pendingClarification() != null && context.pendingClarification().reply() != null) {
            plans.computeIfPresent(planId, (key, current) -> current.clearPendingClarification());
            transition(planId, emitter, PlanState.INTENT);
            context = requirePlan(planId);
        }

        if (context.currentState() == PlanState.INTENT && shouldEnterClarify(context)) {
            PendingClarification pendingClarification = new PendingClarification(
                    CLARIFY_FIELD_DURATION_HOURS,
                    "请问大概想玩几个小时？",
                    List.of("3-4小时", "4-6小时", "6小时以上"),
                    null
            );
            plans.computeIfPresent(planId, (key, current) -> current
                    .withClarifyCount(current.clarifyCount() + 1)
                    .withPendingClarification(pendingClarification));
            transition(planId, emitter, PlanState.CLARIFY);
            sendClarificationRequest(emitter, planId, pendingClarification);
            return;
        }

        if (context.currentState() == PlanState.INTENT) {
            transition(planId, emitter, PlanState.SKELETON);
            transition(planId, emitter, PlanState.RECALL);
            transition(planId, emitter, PlanState.VALIDATE);
        }

        PlanSelectionSnapshot selection = selectCandidates(planId, requirePlan(planId).request(), profile, emitter);
        plans.computeIfPresent(planId, (key, current) -> current.withSelectionSnapshot(selection));
        validateAndPack(planId, emitter, selection, profile, null, null);
    }

    private void continueAdjust(String planId, SseEmitter emitter) throws IOException {
        PlanContext context = requirePlan(planId);
        ScenarioProfile profile = scenarioProfile(context.request().scenario());
        AdjustmentSelection adjustment = applyAdjustment(context.selectionSnapshot(), context.pendingAdjustInstruction());
        transition(planId, emitter, PlanState.ADJUST);
        transition(planId, emitter, PlanState.VALIDATE);
        validateAndPack(planId, emitter, adjustment.selection(), profile, adjustment.affectedSlots(), adjustment.summary());
    }

    private void continueExecution(String planId, SseEmitter emitter) throws IOException {
        PlanContext context = requirePlan(planId);
        if (context.packedPlan() == null || context.selectionSnapshot() == null) {
            throw new InvalidPlanStateException("plan must be in CONFIRM state before execute");
        }

        transition(planId, emitter, PlanState.EXECUTE);
        context = requirePlan(planId);

        List<MessageActionSummary> executedActions = new ArrayList<>();
        for (MessageActionSummary action : context.packedPlan().actions()) {
            ExecuteResultEvent result = executeAction(context, action);
            emitter.send(SseEmitter.event().name(result.type()).data(result));
            executedActions.add(withActionResult(action, result));
        }

        MessagePlanPayload completedPlan = withPlanStatusAndActions(
                context.packedPlan(),
                STATUS_DONE,
                executedActions
        );
        plans.computeIfPresent(planId, (key, current) -> current.withPackedPlan(completedPlan));

        transition(planId, emitter, PlanState.DONE);
        plans.computeIfPresent(planId, (key, current) -> current
                .withExecutionRequested(false)
                .withExecutionCompleted(true));
        emitter.send(SseEmitter.event().name("done").data(new DoneEvent(
                "done",
                planId,
                buildDoneSummary(executedActions),
                System.currentTimeMillis()
        )));
    }

    private void validateAndPack(
            String planId,
            SseEmitter emitter,
            PlanSelectionSnapshot initialSelection,
            ScenarioProfile profile,
            List<String> affectedSlots,
            String adjustSummary
    ) throws IOException {
        PlanSelectionSnapshot selection = initialSelection;
        ValidationDecision decision;

        while (true) {
            decision = validateSelection(planId, selection, profile, emitter);
            if (decision.valid()) {
                break;
            }

            PlanSelectionSnapshot nextSelection = selectFallbackSelection(selection, decision.reason());
            if (sameSelection(selection, nextSelection) || requirePlan(planId).replanCount() >= MAX_REPLAN_ATTEMPTS) {
                transition(planId, emitter, PlanState.DEGRADE);
                String degradeMessage = buildDegradeMessage(requirePlan(planId).replanCount(), decision.reason());
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

            PlanContext context = recordReplan(planId, decision.reason(), decision.injected());
            sendReplan(emitter, new ReplanEvent(
                    "replan",
                    planId,
                    decision.reason(),
                    context.replanCount(),
                    System.currentTimeMillis()
            ));
            transition(planId, emitter, PlanState.REPLAN);
            transition(planId, emitter, PlanState.RECALL);
            selection = nextSelection;
            PlanSelectionSnapshot replanSelection = selection;
            plans.computeIfPresent(planId, (key, current) -> current.withSelectionSnapshot(replanSelection));
            transition(planId, emitter, PlanState.VALIDATE);
        }

        transition(planId, emitter, PlanState.PACK);
        PlanContext context = requirePlan(planId);
        MessagePlanPayload packedPlan = buildPackedPlan(context, profile, selection, decision);
        PlanSelectionSnapshot confirmedSelection = selection;
        plans.computeIfPresent(planId, (key, current) -> current
                .withPackedPlan(packedPlan)
                .withSelectionSnapshot(confirmedSelection)
                .clearPendingClarification()
                .withPendingAdjustInstruction(null));
        transition(planId, emitter, PlanState.CONFIRM);
        PlanContext confirmed = requirePlan(planId);
        if (affectedSlots == null || affectedSlots.isEmpty()) {
            emitter.send(SseEmitter.event().name("plan_ready").data(new PlanReadyEvent("plan_ready", planId, confirmed.packedPlan())));
            return;
        }
        String finalAdjustSummary = adjustSummary;
        if (affectedSlots != null && !affectedSlots.isEmpty()) {
            finalAdjustSummary = buildAdjustSummary(confirmed.packedPlan(), affectedSlots);
        }
        emitter.send(SseEmitter.event().name("adjust_result").data(new AdjustResultEvent(
                "adjust_result",
                planId,
                affectedSlots,
                finalAdjustSummary == null ? "已完成局部调整" : finalAdjustSummary,
                confirmed.packedPlan()
        )));
    }

    private ExecuteResultEvent executeAction(PlanContext context, MessageActionSummary action) {
        if (ACTION_SEND_MESSAGE.equals(action.actionType())) {
            long timestamp = System.currentTimeMillis();
            return new ExecuteResultEvent(
                    "execute_result",
                    context.planId(),
                    action.actionId(),
                    action.actionType(),
                    "success",
                    "MOCK-MSG-" + confirmationSuffix(context.planId() + ":" + action.actionId()),
                    timestamp
            );
        }

        BookingResult booking = bookingTool.bookOrOrder(new BookingRequest(
                context.planId(),
                action.actionId(),
                action.actionType(),
                action.targetPoiId() == null ? targetPoiId(context, action.actionType()) : action.targetPoiId(),
                action.description(),
                context.planId() + ":" + action.actionId(),
                action.confirmationNo()
        ));
        return new ExecuteResultEvent(
                booking.type(),
                booking.planId(),
                booking.actionId(),
                booking.actionType(),
                booking.status(),
                booking.confirmationNo(),
                booking.timestamp()
        );
    }

    private String targetPoiId(PlanContext context, String actionType) {
        if ("buy_ticket".equals(actionType)) {
            return context.selectionSnapshot().activity().poi().id();
        }
        return context.selectionSnapshot().restaurant().poi().id();
    }

    private MessageActionSummary withActionResult(MessageActionSummary action, ExecuteResultEvent result) {
        return new MessageActionSummary(
                action.actionId(),
                action.actionType(),
                action.targetPoiId(),
                action.description(),
                result.status(),
                result.confirmationNo()
        );
    }

    private MessagePlanPayload withPlanStatusAndActions(
            MessagePlanPayload plan,
            String status,
            List<MessageActionSummary> actions
    ) {
        return new MessagePlanPayload(
                plan.planId(),
                plan.scenario(),
                status,
                plan.isPlanB(),
                plan.planBReason(),
                plan.summary(),
                plan.timeline(),
                actions,
                plan.shareMessage(),
                plan.totalDurationHours(),
                plan.replanCount(),
                plan.createdAt()
        );
    }

    private String buildDoneSummary(List<MessageActionSummary> actions) {
        long successCount = actions.stream().filter(action -> "success".equals(action.status())).count();
        long failedCount = actions.stream().filter(action -> "failed".equals(action.status())).count();
        long manualCount = actions.stream().filter(action -> "manual".equals(action.status())).count();
        if (failedCount > 0 || manualCount > 0) {
            return successCount + " 个动作已完成，" + failedCount + " 个失败，" + manualCount + " 个需人工处理";
        }
        return successCount + " 个动作已完成，行程执行包已生成";
    }

    private String confirmationSuffix(String seed) {
        return String.format("%05d", Math.floorMod(seed.hashCode(), 100000));
    }

    private PlanSelectionSnapshot selectCandidates(String planId, CreatePlanRequest request, ScenarioProfile profile, SseEmitter emitter) throws IOException {
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
        SearchCandidate restaurant = firstReservableRestaurant(restaurants);
        return new PlanSelectionSnapshot(activity, restaurant, activities.candidates(), restaurants.candidates());
    }

    private ValidationDecision validateSelection(String planId, PlanSelectionSnapshot selection, ScenarioProfile profile, SseEmitter emitter) throws IOException {
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

    private PlanSelectionSnapshot selectFallbackSelection(PlanSelectionSnapshot selection, String reason) {
        if (reason == null || reason.isBlank()) {
            return selection;
        }
        if (reason.contains("活动")) {
            SearchCandidate nextActivity = searchFallbackActivity(selection);
            return selection.withActivity(nextActivity);
        }
        SearchCandidate nextRestaurant = searchFallbackRestaurant(selection);
        return selection.withRestaurant(nextRestaurant);
    }

    private SearchCandidate searchFallbackRestaurant(PlanSelectionSnapshot selection) {
        return selection.restaurantCandidates().stream()
                .filter(candidate -> !candidate.poi().id().equals(selection.restaurant().poi().id()))
                .filter(candidate -> candidate.poi().actionTypes().contains(ACTION_RESERVE_TABLE))
                .min(Comparator.comparingInt(candidate -> candidate.poi().distanceMinutesFromCenter()))
                .orElseGet(() -> selection.restaurantCandidates().stream()
                        .filter(candidate -> !candidate.poi().id().equals(selection.restaurant().poi().id()))
                        .min(Comparator.comparingInt(candidate -> candidate.poi().distanceMinutesFromCenter()))
                        .orElse(selection.restaurant()));
    }

    private SearchCandidate searchFallbackActivity(PlanSelectionSnapshot selection) {
        return selection.activityCandidates().stream()
                .filter(candidate -> !candidate.poi().id().equals(selection.activity().poi().id()))
                .min(Comparator.comparingInt(candidate -> candidate.poi().distanceMinutesFromCenter()))
                .orElse(selection.activity());
    }

    private boolean sameSelection(PlanSelectionSnapshot current, PlanSelectionSnapshot next) {
        return current.activity().poi().id().equals(next.activity().poi().id())
                && current.restaurant().poi().id().equals(next.restaurant().poi().id());
    }

    private AdjustmentSelection applyAdjustment(PlanSelectionSnapshot selection, String instruction) {
        if (instruction == null || instruction.isBlank()) {
            return new AdjustmentSelection(selection, List.of("restaurant"), "已保留原方案");
        }
        if (containsAny(instruction, "活动", "玩", "拍照", "乐园")) {
            SearchCandidate nextActivity = searchFallbackActivity(selection);
            return new AdjustmentSelection(
                    selection.withActivity(nextActivity),
                    List.of("activity"),
                    null
            );
        }
        SearchCandidate nextRestaurant = searchFallbackRestaurant(selection);
        return new AdjustmentSelection(
                selection.withRestaurant(nextRestaurant),
                List.of("restaurant"),
                null
        );
    }

    private boolean shouldEnterClarify(PlanContext context) {
        if (context.clarifyCount() >= MAX_CLARIFY_ATTEMPTS) {
            return false;
        }
        if (context.clarifyCount() > 0) {
            return false;
        }
        if (context.pendingClarification() != null) {
            return false;
        }
        return requiresDurationClarification(context.request().text());
    }

    private boolean requiresDurationClarification(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        if (containsAny(text, "3小时", "4小时", "5小时", "6小时", "半天", "14:00", "15:00", "16:00", "17:00")) {
            return false;
        }
        return containsAny(text, "几个小时", "多久", "下午空着", "随便逛逛");
    }

    private String buildDegradeMessage(int replanCount, String reason) {
        if (reason == null || reason.isBlank()) {
            return replanCount + " 次重排后仍无可行方案，建议放宽距离限制或调整时间";
        }
        return replanCount + " 次重排后仍无可行方案：" + reason;
    }

    private String buildAdjustSummary(MessagePlanPayload plan, List<String> affectedSlots) {
        if (plan == null || affectedSlots == null || affectedSlots.isEmpty()) {
            return "已完成局部调整";
        }
        if (affectedSlots.contains("activity") && !plan.timeline().isEmpty()) {
            String activityTitle = plan.timeline().get(0).title();
            return "已将活动调整为 " + activityTitle + "，其余安排保持不变";
        }
        if (affectedSlots.contains("restaurant") && plan.timeline().size() > 1) {
            String restaurantTitle = plan.timeline().get(1).title();
            return "已将餐厅换为 " + restaurantTitle + "，其余安排保持不变";
        }
        return "已完成局部调整";
    }

    private MessagePlanPayload buildPackedPlan(
            PlanContext context,
            ScenarioProfile profile,
            PlanSelectionSnapshot selection,
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
                        messagePoi(selection.activity(), decision.activityAvailability()),
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
                        messagePoi(selection.restaurant(), decision.restaurantAvailability()),
                        profile.restaurantStartTime(),
                        profile.restaurantEndTime(),
                        decision.route().distanceMinutes(),
                        restaurantNotes(profile, planBReason, isPlanB)
                )
        );
        List<MessageActionSummary> actions = List.of(
                new MessageActionSummary(
                        "act_" + context.planId() + "_reserve",
                        ACTION_RESERVE_TABLE,
                        selection.restaurant().poi().id(),
                        profile.reserveActionText(),
                        "pending",
                        null
                ),
                new MessageActionSummary(
                        "act_" + context.planId() + "_message",
                        "send_message",
                        null,
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

    private MessagePoiPayload messagePoi(SearchCandidate candidate, AvailabilityResult availability) {
        return new MessagePoiPayload(
                candidate.poi().id(),
                candidate.poi().name(),
                candidate.poi().category(),
                candidate.poi().address(),
                candidate.poi().rating(),
                candidate.poi().distanceMinutesFromCenter(),
                candidate.poi().tags(),
                availability.availabilityStatus(),
                availability.waitMinutes()
        );
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

    private SearchCandidate firstReservableRestaurant(SearchResult result) {
        return result.candidates().stream()
                .filter(candidate -> candidate.poi().actionTypes().contains(ACTION_RESERVE_TABLE))
                .findFirst()
                .orElseGet(() -> firstCandidate(result, "restaurant candidates are required"));
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

    private void transition(String planId, SseEmitter emitter, PlanState next) throws IOException {
        PlanContext current = requirePlan(planId);
        plans.computeIfPresent(planId, (key, value) -> value.withCurrentState(next));
        emitter.send(SseEmitter.event().name("state_change").data(new StateChangeEvent(
                "state_change",
                planId,
                current.currentState().name(),
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

    private void sendClarificationRequest(SseEmitter emitter, String planId, PendingClarification pendingClarification) throws IOException {
        emitter.send(SseEmitter.event().name("clarification_request").data(new ClarificationRequestEvent(
                "clarification_request",
                planId,
                pendingClarification.question(),
                pendingClarification.field(),
                pendingClarification.options(),
                System.currentTimeMillis()
        )));
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
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

    private record ValidationDecision(
            boolean valid,
            String reason,
            boolean injected,
            RouteResult route,
            AvailabilityResult activityAvailability,
            AvailabilityResult restaurantAvailability
    ) {
    }

    private record AdjustmentSelection(
            PlanSelectionSnapshot selection,
            List<String> affectedSlots,
            String summary
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
