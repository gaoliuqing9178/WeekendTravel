package com.weekendtravel.backend.b2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioFlagsTests {

    @Test
    void scenarioFlagsStartDisabledAndSupportHotUpdates() throws Exception {
        Object flags = newScenarioFlags();

        Object initial = invoke(flags, "current");
        assertFalse((Boolean) invoke(initial, "restaurantFull"));
        assertFalse((Boolean) invoke(initial, "routeTooFar"));
        assertFalse((Boolean) invoke(initial, "bookingFail"));
        assertFalse((Boolean) invoke(initial, "ageMismatch"));

        Object updated = update(flags, true, true, false, true);
        assertTrue((Boolean) invoke(updated, "restaurantFull"));
        assertTrue((Boolean) invoke(updated, "routeTooFar"));
        assertFalse((Boolean) invoke(updated, "bookingFail"));
        assertTrue((Boolean) invoke(updated, "ageMismatch"));

        Object partial = update(flags, null, false, true, null);
        assertTrue((Boolean) invoke(partial, "restaurantFull"));
        assertFalse((Boolean) invoke(partial, "routeTooFar"));
        assertTrue((Boolean) invoke(partial, "bookingFail"));
        assertTrue((Boolean) invoke(partial, "ageMismatch"));

        Object reset = invoke(flags, "reset");
        assertEquals(0, ((java.util.List<?>) invoke(reset, "activeNames")).size());
    }

    private Object newScenarioFlags() throws Exception {
        return Class.forName("com.weekendtravel.backend.b2.scenario.ScenarioFlags")
                .getDeclaredConstructor()
                .newInstance();
    }

    private Object update(
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
        return flags.getClass().getMethod("update", requestClass).invoke(flags, request);
    }

    private Object invoke(Object target, String methodName) throws Exception {
        return target.getClass().getMethod(methodName).invoke(target);
    }
}
