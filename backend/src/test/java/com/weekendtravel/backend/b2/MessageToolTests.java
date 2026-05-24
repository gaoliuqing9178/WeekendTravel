package com.weekendtravel.backend.b2;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageToolTests {

    @Test
    void composeShareMessageGeneratesDeterministicFamilyFallbackAndPlanShareMessage() throws Exception {
        Object tool = newMessageTool();
        Object request = newRequest(
                "plan_family_001",
                "family",
                "CONFIRM",
                false,
                null,
                "亲子室内活动、低负担晚餐和回家路线",
                List.of(
                        newSlot(1, "activity", "奇妙亲子乐园", "奇妙亲子乐园", "14:00", "16:30", 15,
                                List.of("适合 5 岁儿童", "室内活动，天气影响小")),
                        newSlot(2, "restaurant", "轻食家庭餐厅", "轻食家庭餐厅", "17:00", "18:10", 12,
                                List.of("儿童椅", "低卡", "少油少糖"))
                ),
                List.of(
                        newAction("act_family_001", "reserve_table", "预约 17:00 的 2 大 1 小座位", "pending", null),
                        newAction("act_family_002", "add_note", "备注儿童椅和少油少糖", "pending", null),
                        newAction("act_family_003", "send_message", "生成家庭出行消息", "pending", null)
                ),
                4.2,
                0,
                "2026-05-24T14:00:00+08:00"
        );

        Object first = compose(tool, request);
        Object second = compose(tool, request);
        String shareMessage = (String) invoke(first, "shareMessage");

        assertEquals(shareMessage, invoke(second, "shareMessage"));
        assertTrue(shareMessage.contains("14:00 先去奇妙亲子乐园"));
        assertTrue(shareMessage.contains("17:00 去轻食家庭餐厅"));
        assertTrue(shareMessage.contains("预约 17:00 的 2 大 1 小座位"));
        assertTrue(shareMessage.contains("备注儿童椅和少油少糖"));
        assertFalse(shareMessage.contains("生成家庭出行消息"));
        assertTrue(shareMessage.contains("低负担饮食"));
        assertTrue(shareMessage.contains("全程约4.2小时"));
        assertEquals(false, invoke(first, "llmUsed"));
        assertEquals("fallback-v1", invoke(first, "templateVersion"));
        assertTrue((Long) invoke(first, "latencyMs") >= 0);

        Object plan = invoke(first, "plan");
        assertEquals("plan_family_001", invoke(plan, "planId"));
        assertEquals(shareMessage, invoke(plan, "shareMessage"));
        assertEquals(0, invoke(plan, "replanCount"));
    }

    @Test
    void composeShareMessageSortsFriendsTimelineAndMentionsPhotoFriendlyPlan() throws Exception {
        Object tool = newMessageTool();
        Object request = newRequest(
                "plan_friends_001",
                "friends",
                "CONFIRM",
                false,
                null,
                "展览拍照、咖啡休息和晚餐聚会",
                List.of(
                        newSlot(3, "restaurant", "暮色烤肉", "暮色烤肉", "17:30", "19:00", 11,
                                List.of("可订 4 人桌", "氛围好")),
                        newSlot(1, "activity", "城市影像展", "城市影像展", "14:30", "16:00", 14,
                                List.of("拍照点多", "适合 4 人同行")),
                        newSlot(2, "cafe", "街角咖啡", "街角咖啡", "16:15", "17:00", 8,
                                List.of("适合中途休息和拍照"))
                ),
                List.of(newAction("act_friends_001", "reserve_table", "预约 17:30 的 4 人桌", "pending", null)),
                4.5,
                0,
                "2026-05-24T14:00:00+08:00"
        );

        Object result = compose(tool, request);
        String shareMessage = (String) invoke(result, "shareMessage");

        assertTrue(shareMessage.indexOf("城市影像展") < shareMessage.indexOf("街角咖啡"));
        assertTrue(shareMessage.indexOf("街角咖啡") < shareMessage.indexOf("暮色烤肉"));
        assertTrue(shareMessage.contains("玩、吃、拍照和聊天"));
        assertTrue(shareMessage.contains("全程约4.5小时"));
    }

    @Test
    void composeShareMessageIncludesPlanBReason() throws Exception {
        Object tool = newMessageTool();
        Object request = newRequest(
                "plan_family_plan_b",
                "family",
                "CONFIRM",
                true,
                "原餐厅排队预计 70 分钟，已切换到可订位低卡餐厅",
                "亲子活动和 Plan B 晚餐",
                List.of(
                        newSlot(1, "activity", "奇妙亲子乐园", "奇妙亲子乐园", "14:00", "16:30", 15,
                                List.of("室内", "不太累")),
                        newSlot(2, "restaurant", "轻食家庭餐厅", "轻食家庭餐厅", "17:00", "18:10", 12,
                                List.of("低卡", "可订位"))
                ),
                List.of(),
                4.2,
                1,
                "2026-05-24T14:00:00+08:00"
        );

        Object result = compose(tool, request);
        String shareMessage = (String) invoke(result, "shareMessage");
        Object plan = invoke(result, "plan");

        assertTrue(shareMessage.contains("Plan B 说明：原餐厅排队预计 70 分钟"));
        assertEquals(true, invoke(plan, "isPlanB"));
        assertEquals(1, invoke(plan, "replanCount"));
    }

    @Test
    void composeShareMessageRejectsInvalidRequests() throws Exception {
        Object tool = newMessageTool();
        List<Object> timeline = List.of(newSlot(1, "activity", "奇妙亲子乐园", "奇妙亲子乐园", "14:00", "16:30", 15, List.of()));

        IllegalArgumentException missingPlanId = assertThrows(
                IllegalArgumentException.class,
                () -> compose(tool, newRequest(null, "family", "CONFIRM", false, null, null, timeline, List.of(), 4.0, 0, null))
        );
        assertEquals("planId is required", missingPlanId.getMessage());

        IllegalArgumentException invalidScenario = assertThrows(
                IllegalArgumentException.class,
                () -> compose(tool, newRequest("plan_001", "couple", "CONFIRM", false, null, null, timeline, List.of(), 4.0, 0, null))
        );
        assertEquals("scenario must be family or friends", invalidScenario.getMessage());

        IllegalArgumentException missingTimeline = assertThrows(
                IllegalArgumentException.class,
                () -> compose(tool, newRequest("plan_001", "family", "CONFIRM", false, null, null, List.of(), List.of(), 4.0, 0, null))
        );
        assertEquals("timeline is required", missingTimeline.getMessage());
    }

    private Object newMessageTool() throws Exception {
        return Class.forName("com.weekendtravel.backend.b2.tool.MessageTool")
                .getDeclaredConstructor()
                .newInstance();
    }

    private Object newSlot(
            int order,
            String type,
            String title,
            String poiName,
            String startTime,
            String endTime,
            Integer distanceMinutes,
            List<String> notes
    ) throws Exception {
        return Class.forName("com.weekendtravel.backend.b2.tool.MessageTimeSlot")
                .getDeclaredConstructor(
                        int.class,
                        String.class,
                        String.class,
                        String.class,
                        String.class,
                        String.class,
                        Integer.class,
                        List.class
                )
                .newInstance(order, type, title, poiName, startTime, endTime, distanceMinutes, notes);
    }

    private Object newAction(
            String actionId,
            String actionType,
            String description,
            String status,
            String confirmationNo
    ) throws Exception {
        return Class.forName("com.weekendtravel.backend.b2.tool.MessageActionSummary")
                .getDeclaredConstructor(String.class, String.class, String.class, String.class, String.class)
                .newInstance(actionId, actionType, description, status, confirmationNo);
    }

    private Object newRequest(
            String planId,
            String scenario,
            String status,
            Boolean isPlanB,
            String planBReason,
            String summary,
            List<?> timeline,
            List<?> actions,
            Double totalDurationHours,
            Integer replanCount,
            String createdAt
    ) throws Exception {
        return Class.forName("com.weekendtravel.backend.b2.tool.MessageRequest")
                .getDeclaredConstructor(
                        String.class,
                        String.class,
                        String.class,
                        Boolean.class,
                        String.class,
                        String.class,
                        List.class,
                        List.class,
                        Double.class,
                        Integer.class,
                        String.class
                )
                .newInstance(
                        planId,
                        scenario,
                        status,
                        isPlanB,
                        planBReason,
                        summary,
                        timeline,
                        actions,
                        totalDurationHours,
                        replanCount,
                        createdAt
                );
    }

    private Object compose(Object tool, Object request) throws Exception {
        Class<?> requestClass = Class.forName("com.weekendtravel.backend.b2.tool.MessageRequest");
        return invoke(tool, "composeShareMessage", requestClass, request);
    }

    private Object invoke(Object target, String methodName) throws Exception {
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (InvocationTargetException exception) {
            throw unwrap(exception);
        }
    }

    private Object invoke(Object target, String methodName, Class<?> argumentClass, Object argument) throws Exception {
        try {
            return target.getClass().getMethod(methodName, argumentClass).invoke(target, argument);
        } catch (InvocationTargetException exception) {
            throw unwrap(exception);
        }
    }

    private Exception unwrap(InvocationTargetException exception) throws Exception {
        Throwable cause = exception.getCause();
        if (cause instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (cause instanceof Error error) {
            throw error;
        }
        throw exception;
    }
}
