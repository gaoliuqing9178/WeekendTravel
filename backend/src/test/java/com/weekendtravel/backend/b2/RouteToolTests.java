package com.weekendtravel.backend.b2;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteToolTests {

    private final Object routeTool = newRouteTool();

    @Test
    void calculateRouteTimeReturnsCenterToPoiDistanceAndSummary() throws Exception {
        Object result = route(
                null,
                "poi_family_activity_001"
        );

        assertEquals("当前位置", invoke(result, "fromName"));
        assertEquals("奇妙亲子乐园", invoke(result, "toName"));
        assertEquals(15, invoke(result, "distanceMinutes"));
        assertTrue(((String) invoke(result, "summary")).contains("约15分钟"));
        assertTrue((Long) invoke(result, "latencyMs") >= 0);
    }

    @Test
    void calculateRouteTimeReturnsPoiToPoiRoute() throws Exception {
        Object result = route(
                "poi_family_activity_001",
                "poi_family_food_002"
        );

        assertEquals("奇妙亲子乐园", invoke(result, "fromName"));
        assertEquals("轻食家庭餐厅", invoke(result, "toName"));
        assertTrue((Integer) invoke(result, "distanceMinutes") >= 3);
        assertTrue(((String) invoke(result, "summary")).contains("奇妙亲子乐园"));
        assertTrue(((String) invoke(result, "summary")).contains("轻食家庭餐厅"));
        assertTrue((Long) invoke(result, "latencyMs") >= 0);
    }

    @Test
    void calculateRouteTimeReturnsZeroForSamePoi() throws Exception {
        Object result = route(
                "poi_family_activity_001",
                "poi_family_activity_001"
        );

        assertEquals(0, invoke(result, "distanceMinutes"));
        assertTrue(((String) invoke(result, "summary")).contains("无需移动"));
        assertTrue((Long) invoke(result, "latencyMs") >= 0);
    }

    @Test
    void calculateRouteTimeRejectsUnknownPoi() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> route(
                        "poi_family_activity_001",
                        "missing_poi"
                )
        );

        assertEquals("unknown toPoiId: missing_poi", exception.getMessage());
    }

    @Test
    void calculateRouteTimeRejectsMissingTarget() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> route(
                        "poi_family_activity_001",
                        null
                )
        );

        assertEquals("toPoiId is required", exception.getMessage());
    }

    private static Object newRouteTool() {
        try {
            Class<?> repositoryClass = Class.forName("com.weekendtravel.backend.b2.repository.PoiRepository");
            Object repository = repositoryClass.getDeclaredConstructor().newInstance();
            return Class.forName("com.weekendtravel.backend.b2.tool.RouteTool")
                    .getDeclaredConstructor(repositoryClass)
                    .newInstance(repository);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("RouteTool should be available to tests", exception);
        }
    }

    private Object route(String fromPoiId, String toPoiId) throws Exception {
        Class<?> requestClass = Class.forName("com.weekendtravel.backend.b2.tool.RouteRequest");
        Object request = requestClass
                .getDeclaredConstructor(String.class, String.class)
                .newInstance(fromPoiId, toPoiId);
        return invoke(routeTool, "calculateRouteTime", requestClass, request);
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
