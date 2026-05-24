package com.weekendtravel.backend.b2;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvailabilityToolTests {

    @Test
    void checkAvailabilityReflectsDefaultPoiAvailability() throws Exception {
        Object tool = newAvailabilityTool(newScenarioFlags());

        Object result = checkAvailability(
                tool,
                "poi_family_activity_001",
                "weekdayAfternoon",
                5,
                3
        );

        assertEquals("poi_family_activity_001", invoke(result, "poiId"));
        assertEquals("奇妙亲子乐园", invoke(result, "poiName"));
        assertEquals("weekdayAfternoon", invoke(result, "slot"));
        assertEquals(true, invoke(result, "available"));
        assertEquals("available", invoke(result, "availabilityStatus"));
        assertEquals(20, invoke(result, "remaining"));
        assertEquals(5, invoke(result, "waitMinutes"));
        assertEquals(true, invoke(result, "ageMatched"));
        assertEquals(true, invoke(result, "groupSizeMatched"));
        assertTrue((Long) invoke(result, "latencyMs") >= 0);
        assertTrue(((List<?>) invoke(result, "reasons")).contains("defaultAvailability:weekdayAfternoon"));
    }

    @Test
    void checkAvailabilityAppliesRestaurantFullScenarioFlag() throws Exception {
        Object flags = newScenarioFlags();
        updateFlags(flags, true, false, false, false);
        Object tool = newAvailabilityTool(flags);

        Object result = checkAvailability(
                tool,
                "poi_family_food_002",
                "weekendAfternoon",
                5,
                3
        );

        assertEquals(false, invoke(result, "available"));
        assertEquals("full", invoke(result, "availabilityStatus"));
        assertEquals(0, invoke(result, "remaining"));
        assertTrue((Integer) invoke(result, "waitMinutes") >= 70);
        assertTrue(((List<?>) invoke(result, "reasons")).contains("scenario:restaurantFull"));
        Object scenarioFlags = invoke(result, "scenarioFlags");
        assertTrue((Boolean) invoke(scenarioFlags, "restaurantFull"));
        assertFalse((Boolean) invoke(scenarioFlags, "bookingFail"));
    }

    @Test
    void checkAvailabilityAppliesAgeMismatchScenarioFlag() throws Exception {
        Object flags = newScenarioFlags();
        updateFlags(flags, false, false, false, true);
        Object tool = newAvailabilityTool(flags);

        Object result = checkAvailability(
                tool,
                "poi_family_activity_001",
                "weekendAfternoon",
                5,
                3
        );

        assertEquals(false, invoke(result, "available"));
        assertEquals(false, invoke(result, "ageMatched"));
        assertTrue(((List<?>) invoke(result, "reasons")).contains("scenario:ageMismatch"));
        Object scenarioFlags = invoke(result, "scenarioFlags");
        assertTrue((Boolean) invoke(scenarioFlags, "ageMismatch"));
    }

    @Test
    void checkAvailabilityRejectsInvalidInput() throws Exception {
        Object tool = newAvailabilityTool(newScenarioFlags());

        IllegalArgumentException missingPoiId = assertThrows(
                IllegalArgumentException.class,
                () -> checkAvailability(tool, null, "weekendAfternoon", null, null)
        );
        assertEquals("poiId is required", missingPoiId.getMessage());

        IllegalArgumentException unknownPoi = assertThrows(
                IllegalArgumentException.class,
                () -> checkAvailability(tool, "missing_poi", "weekendAfternoon", null, null)
        );
        assertEquals("unknown poiId: missing_poi", unknownPoi.getMessage());

        IllegalArgumentException invalidSlot = assertThrows(
                IllegalArgumentException.class,
                () -> checkAvailability(tool, "poi_family_activity_001", "night", null, null)
        );
        assertEquals("slot must be weekdayAfternoon or weekendAfternoon", invalidSlot.getMessage());
    }

    private Object newAvailabilityTool(Object flags) throws Exception {
        Class<?> repositoryClass = Class.forName("com.weekendtravel.backend.b2.repository.PoiRepository");
        Class<?> flagsClass = Class.forName("com.weekendtravel.backend.b2.scenario.ScenarioFlags");
        Object repository = repositoryClass.getDeclaredConstructor().newInstance();
        return Class.forName("com.weekendtravel.backend.b2.tool.AvailabilityTool")
                .getDeclaredConstructor(repositoryClass, flagsClass)
                .newInstance(repository, flags);
    }

    private Object newScenarioFlags() throws Exception {
        return Class.forName("com.weekendtravel.backend.b2.scenario.ScenarioFlags")
                .getDeclaredConstructor()
                .newInstance();
    }

    private Object checkAvailability(
            Object tool,
            String poiId,
            String slot,
            Integer minAge,
            Integer groupSize
    ) throws Exception {
        Class<?> requestClass = Class.forName("com.weekendtravel.backend.b2.tool.AvailabilityRequest");
        Object request = requestClass
                .getDeclaredConstructor(String.class, String.class, Integer.class, Integer.class)
                .newInstance(poiId, slot, minAge, groupSize);
        return invoke(tool, "checkAvailability", requestClass, request);
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
