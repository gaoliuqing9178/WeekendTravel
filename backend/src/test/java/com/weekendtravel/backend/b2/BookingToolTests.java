package com.weekendtravel.backend.b2;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingToolTests {

    @Test
    void bookOrOrderReturnsExecuteResultAndMockConfirmationNoOnSuccess() throws Exception {
        Object tool = newBookingTool(newScenarioFlags());

        Object result = bookOrOrder(
                tool,
                "plan_family_001",
                "act_001",
                "reserve_table",
                "poi_family_food_002",
                "预约 17:30 的 2 大 1 小座位",
                "idem-reserve-001",
                null
        );

        assertEquals("execute_result", invoke(result, "type"));
        assertEquals("plan_family_001", invoke(result, "planId"));
        assertEquals("act_001", invoke(result, "actionId"));
        assertEquals("reserve_table", invoke(result, "actionType"));
        assertEquals("success", invoke(result, "status"));
        assertTrue(((String) invoke(result, "confirmationNo")).startsWith("MOCK-TBL-"));
        assertTrue((Long) invoke(result, "timestamp") > 0);
        assertTrue((Long) invoke(result, "latencyMs") >= 0);
    }

    @Test
    void bookOrOrderUsesIdempotencyKeyToAvoidDuplicateMockOrders() throws Exception {
        Object flags = newScenarioFlags();
        Object tool = newBookingTool(flags);

        Object first = bookOrOrder(
                tool,
                "plan_family_001",
                "act_001",
                "reserve_table",
                "poi_family_food_002",
                "预约座位",
                "idem-repeat-001",
                null
        );
        updateFlags(flags, false, false, true, false);
        Object repeated = bookOrOrder(
                tool,
                "plan_family_001",
                "act_001",
                "reserve_table",
                "poi_family_food_002",
                "预约座位",
                "idem-repeat-001",
                null
        );

        assertEquals("success", invoke(repeated, "status"));
        assertEquals(invoke(first, "confirmationNo"), invoke(repeated, "confirmationNo"));
        assertEquals(invoke(first, "timestamp"), invoke(repeated, "timestamp"));
    }

    @Test
    void bookingFailScenarioReturnsFailedActionWithoutConfirmationNo() throws Exception {
        Object flags = newScenarioFlags();
        updateFlags(flags, false, false, true, false);
        Object tool = newBookingTool(flags);

        Object result = bookOrOrder(
                tool,
                "plan_family_001",
                "act_002",
                "buy_ticket",
                "poi_family_activity_001",
                "购买亲子乐园门票",
                "idem-ticket-fail-001",
                null
        );

        assertEquals("execute_result", invoke(result, "type"));
        assertEquals("buy_ticket", invoke(result, "actionType"));
        assertEquals("failed", invoke(result, "status"));
        assertNull(invoke(result, "confirmationNo"));
        assertTrue(((String) invoke(result, "message")).contains("bookingFail"));
    }

    @Test
    void cancelBookingIsSupportedAsReverseOperation() throws Exception {
        Object flags = newScenarioFlags();
        updateFlags(flags, false, false, true, false);
        Object tool = newBookingTool(flags);

        Object result = bookOrOrder(
                tool,
                "plan_family_001",
                "act_cancel_001",
                "cancel_booking",
                null,
                "取消餐厅预约",
                "idem-cancel-001",
                "MOCK-TBL-12345"
        );

        assertEquals("execute_result", invoke(result, "type"));
        assertEquals("cancel_booking", invoke(result, "actionType"));
        assertEquals("success", invoke(result, "status"));
        assertTrue(((String) invoke(result, "confirmationNo")).startsWith("MOCK-CXL-"));
        assertTrue(((String) invoke(result, "message")).contains("MOCK-TBL-12345"));
    }

    @Test
    void bookOrOrderRejectsInvalidRequests() throws Exception {
        Object tool = newBookingTool(newScenarioFlags());

        IllegalArgumentException missingKey = assertThrows(
                IllegalArgumentException.class,
                () -> bookOrOrder(
                        tool,
                        "plan_family_001",
                        "act_001",
                        "reserve_table",
                        "poi_family_food_002",
                        "预约座位",
                        null,
                        null
                )
        );
        assertEquals("idempotencyKey is required", missingKey.getMessage());

        IllegalArgumentException unsupportedAction = assertThrows(
                IllegalArgumentException.class,
                () -> bookOrOrder(
                        tool,
                        "plan_family_001",
                        "act_002",
                        "send_message",
                        "poi_family_food_002",
                        "发送消息",
                        "idem-send-001",
                        null
                )
        );
        assertEquals("unsupported actionType: send_message", unsupportedAction.getMessage());

        IllegalArgumentException unsupportedByPoi = assertThrows(
                IllegalArgumentException.class,
                () -> bookOrOrder(
                        tool,
                        "plan_family_001",
                        "act_003",
                        "schedule_delivery",
                        "poi_family_food_002",
                        "配送餐食",
                        "idem-delivery-001",
                        null
                )
        );
        assertEquals(
                "actionType schedule_delivery is not supported by poi: poi_family_food_002",
                unsupportedByPoi.getMessage()
        );
    }

    private Object newBookingTool(Object flags) throws Exception {
        Class<?> repositoryClass = Class.forName("com.weekendtravel.backend.b2.repository.PoiRepository");
        Class<?> flagsClass = Class.forName("com.weekendtravel.backend.b2.scenario.ScenarioFlags");
        Object repository = repositoryClass.getDeclaredConstructor().newInstance();
        return Class.forName("com.weekendtravel.backend.b2.tool.BookingTool")
                .getDeclaredConstructor(repositoryClass, flagsClass)
                .newInstance(repository, flags);
    }

    private Object newScenarioFlags() throws Exception {
        return Class.forName("com.weekendtravel.backend.b2.scenario.ScenarioFlags")
                .getDeclaredConstructor()
                .newInstance();
    }

    private Object bookOrOrder(
            Object tool,
            String planId,
            String actionId,
            String actionType,
            String targetPoiId,
            String description,
            String idempotencyKey,
            String previousConfirmationNo
    ) throws Exception {
        Class<?> requestClass = Class.forName("com.weekendtravel.backend.b2.tool.BookingRequest");
        Object request = requestClass
                .getDeclaredConstructor(
                        String.class,
                        String.class,
                        String.class,
                        String.class,
                        String.class,
                        String.class,
                        String.class
                )
                .newInstance(
                        planId,
                        actionId,
                        actionType,
                        targetPoiId,
                        description,
                        idempotencyKey,
                        previousConfirmationNo
                );
        return invoke(tool, "bookOrOrder", requestClass, request);
    }

    private void updateFlags(
            Object flags,
            Boolean restaurantFull,
            Boolean routeTooFar,
            Boolean bookingFail,
            Boolean ageMismatch
    ) throws Exception {
        Class<?> requestClass = Class.forName("com.weekendtravel.backend.b2.scenario.ScenarioFlagsUpdateRequest");
        Object request = requestClass
                .getDeclaredConstructor(Boolean.class, Boolean.class, Boolean.class, Boolean.class)
                .newInstance(restaurantFull, routeTooFar, bookingFail, ageMismatch);
        invoke(flags, "update", requestClass, request);
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
