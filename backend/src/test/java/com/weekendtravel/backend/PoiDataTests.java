package com.weekendtravel.backend;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoiDataTests {

    private static final Set<String> VALID_AVAILABILITY_STATUS = Set.of("available", "limited", "full");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void poiDataCanBeParsedAndCoversRequiredCategories() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("mock/poi_data.json");
        assertNotNull(stream, "mock/poi_data.json should exist on the classpath");

        try (stream) {
            JsonNode root = objectMapper.readTree(stream);
            JsonNode pois = root.path("pois");

            assertTrue(root.path("version").isInt(), "version should be an integer");
            assertTrue(root.path("updatedAt").isTextual(), "updatedAt should be a string");
            assertTrue(root.path("city").isTextual(), "city should be a string");
            assertTrue(root.path("center").isObject(), "center should be an object");
            assertTrue(pois.isArray(), "pois should be an array");
            assertTrue(pois.size() >= 50, "final POI dataset should contain at least 50 entries");

            Set<String> ids = new HashSet<>();
            Map<String, Integer> categoryCounts = new HashMap<>();
            boolean coversFamily = false;
            boolean coversFriends = false;

            for (JsonNode poi : pois) {
                String id = requireText(poi, "id", "poi");
                assertTrue(ids.add(id), "duplicate POI id: " + id);

                requireText(poi, "name", id);
                String category = requireText(poi, "category", id);
                requireText(poi, "subCategory", id);
                requireArray(poi, "scenarios", id);
                requireText(poi, "address", id);
                requireNumber(poi, "lat", id);
                requireNumber(poi, "lng", id);
                requireNumber(poi, "rating", id);
                requireArray(poi, "tags", id);
                requireNumber(poi, "distanceMinutesFromCenter", id);
                requireNumber(poi, "pricePerPerson", id);
                requireObject(poi, "ageRequirement", id);
                requireObject(poi, "groupSize", id);
                String availabilityStatus = requireText(poi, "availabilityStatus", id);
                assertTrue(
                        VALID_AVAILABILITY_STATUS.contains(availabilityStatus),
                        id + ".availabilityStatus should be one of " + VALID_AVAILABILITY_STATUS
                );
                requireNumber(poi, "waitMinutes", id);
                JsonNode defaultAvailability = requireObject(poi, "defaultAvailability", id);
                assertAvailabilitySlot(defaultAvailability, "weekdayAfternoon", id);
                assertAvailabilitySlot(defaultAvailability, "weekendAfternoon", id);
                JsonNode scenarioFlags = requireObject(poi, "scenarioFlags", id);
                assertTrue(scenarioFlags.path("family").isBoolean(), id + ".scenarioFlags.family should be boolean");
                assertTrue(scenarioFlags.path("friends").isBoolean(), id + ".scenarioFlags.friends should be boolean");
                requireArray(poi, "actionTypes", id);

                categoryCounts.merge(category, 1, Integer::sum);
                coversFamily |= scenarioFlags.path("family").asBoolean();
                coversFriends |= scenarioFlags.path("friends").asBoolean();
            }

            assertTrue(coversFamily, "dataset should include family scenario POIs");
            assertTrue(coversFriends, "dataset should include friends scenario POIs");
            assertTrue(categoryCounts.getOrDefault("activity", 0) >= 1, "dataset should include activity POIs");
            assertTrue(categoryCounts.getOrDefault("restaurant", 0) >= 1, "dataset should include restaurant POIs");
            assertTrue(categoryCounts.getOrDefault("cafe", 0) >= 1, "dataset should include cafe POIs");
            assertTrue(categoryCounts.getOrDefault("dessert", 0) >= 1, "dataset should include dessert POIs");
            assertTrue(categoryCounts.getOrDefault("supplier", 0) >= 1, "dataset should include supplier POIs");
        }
    }

    private String requireText(JsonNode node, String field, String owner) {
        assertTrue(node.path(field).isTextual(), owner + "." + field + " should be a string");
        return node.path(field).asText();
    }

    private void requireNumber(JsonNode node, String field, String owner) {
        assertTrue(node.path(field).isNumber(), owner + "." + field + " should be a number");
    }

    private JsonNode requireObject(JsonNode node, String field, String owner) {
        JsonNode child = node.path(field);
        assertTrue(child.isObject(), owner + "." + field + " should be an object");
        return child;
    }

    private void requireArray(JsonNode node, String field, String owner) {
        assertTrue(node.path(field).isArray(), owner + "." + field + " should be an array");
    }

    private void assertAvailabilitySlot(JsonNode defaultAvailability, String slot, String owner) {
        JsonNode value = defaultAvailability.path(slot);
        assertTrue(value.isObject(), owner + ".defaultAvailability." + slot + " should be an object");
        assertTrue(value.path("available").isBoolean(), owner + "." + slot + ".available should be boolean");
        assertTrue(value.path("remaining").isInt(), owner + "." + slot + ".remaining should be an integer");
        assertTrue(value.path("waitMinutes").isInt(), owner + "." + slot + ".waitMinutes should be an integer");
    }
}
