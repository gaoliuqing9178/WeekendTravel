package com.weekendtravel.backend.b2;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchToolTests {

    private final Object searchTool = newSearchTool();

    @Test
    void searchLocalPlacesReturnsFamilyActivityCandidatesFromLocalJson() throws Exception {
        Object result = search(
                "family",
                List.of("activity"),
                "室内",
                20,
                5,
                3,
                5
        );

        List<?> candidates = candidates(result);
        assertTrue((Long) invoke(result, "latencyMs") >= 0);
        assertFalse(candidates.isEmpty());
        assertTrue(candidates.size() <= 5);

        boolean includesKnownFamilyActivity = false;
        for (Object candidate : candidates) {
            Object poi = invoke(candidate, "poi");
            assertEquals("activity", invoke(poi, "category"));
            assertTrue((Boolean) invoke(poi, "supportsScenario", String.class, "family"));
            assertTrue((Boolean) invoke(poi, "supportsAge", Integer.class, 5));
            includesKnownFamilyActivity |= "poi_family_activity_001".equals(invoke(poi, "id"));
        }
        assertTrue(includesKnownFamilyActivity);
    }

    @Test
    void searchLocalPlacesCanReturnNoCandidatesWithLatency() throws Exception {
        Object result = search(
                "friends",
                List.of("supplier"),
                "不存在的关键词",
                5,
                null,
                4,
                5
        );

        assertTrue((Long) invoke(result, "latencyMs") >= 0);
        assertTrue(candidates(result).isEmpty());
    }

    @Test
    void searchLocalPlacesRejectsInvalidScenario() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> search(
                        "couple",
                        List.of("activity"),
                        null,
                        20,
                        null,
                        null,
                        5
                )
        );

        assertEquals("scenario must be family or friends", exception.getMessage());
    }

    @Test
    void searchLocalPlacesRejectsInvalidLimit() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> search(
                        "family",
                        List.of("activity"),
                        null,
                        20,
                        null,
                        null,
                        0
                )
        );

        assertEquals("limit must be positive", exception.getMessage());
    }

    private static Object newSearchTool() {
        try {
            Class<?> repositoryClass = Class.forName("com.weekendtravel.backend.b2.repository.PoiRepository");
            Object repository = repositoryClass.getDeclaredConstructor().newInstance();
            return Class.forName("com.weekendtravel.backend.b2.tool.SearchTool")
                    .getDeclaredConstructor(repositoryClass)
                    .newInstance(repository);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("SearchTool should be available to tests", exception);
        }
    }

    private Object search(
            String scenario,
            List<String> categories,
            String keyword,
            Integer maxDistanceMinutes,
            Integer minAge,
            Integer groupSize,
            Integer limit
    ) throws Exception {
        Class<?> requestClass = Class.forName("com.weekendtravel.backend.b2.tool.SearchRequest");
        Object request = requestClass
                .getDeclaredConstructor(
                        String.class,
                        List.class,
                        String.class,
                        Integer.class,
                        Integer.class,
                        Integer.class,
                        Integer.class
                )
                .newInstance(scenario, categories, keyword, maxDistanceMinutes, minAge, groupSize, limit);
        return invoke(searchTool, "searchLocalPlaces", requestClass, request);
    }

    private List<?> candidates(Object result) throws Exception {
        return (List<?>) invoke(result, "candidates");
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
