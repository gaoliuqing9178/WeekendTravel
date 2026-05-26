package com.weekendtravel.backend.b2.repository;

import com.weekendtravel.backend.b2.model.AvailabilitySlot;
import com.weekendtravel.backend.b2.model.DefaultAvailability;
import com.weekendtravel.backend.b2.model.GeoPoint;
import com.weekendtravel.backend.b2.model.Poi;
import com.weekendtravel.backend.b2.model.PoiCatalog;
import com.weekendtravel.backend.b2.model.PoiScenarioFlags;
import com.weekendtravel.backend.b2.model.Range;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class PoiRepository {

    private static final String RESOURCE_PATH = "mock/poi_data.json";

    private final PoiCatalog catalog;

    public PoiRepository() {
        this.catalog = loadCatalog();
    }

    public PoiCatalog catalog() {
        return catalog;
    }

    public GeoPoint center() {
        return catalog.center();
    }

    public List<Poi> findAll() {
        return catalog.pois();
    }

    public Optional<Poi> findById(String poiId) {
        if (poiId == null || poiId.isBlank()) {
            return Optional.empty();
        }
        return catalog.pois().stream()
                .filter(poi -> poi.id().equals(poiId))
                .findFirst();
    }

    private PoiCatalog loadCatalog() {
        ObjectMapper objectMapper = new ObjectMapper();
        InputStream stream = getClass().getClassLoader().getResourceAsStream(RESOURCE_PATH);
        if (stream == null) {
            throw new IllegalStateException(RESOURCE_PATH + " was not found on the classpath");
        }

        try (stream) {
            JsonNode root = objectMapper.readTree(stream);
            JsonNode center = requireObject(root, "center", "catalog");
            List<Poi> pois = new ArrayList<>();
            for (JsonNode poi : requireArray(root, "pois", "catalog")) {
                pois.add(toPoi(poi));
            }

            return new PoiCatalog(
                    requireInt(root, "version", "catalog"),
                    requireText(root, "updatedAt", "catalog"),
                    requireText(root, "city", "catalog"),
                    new GeoPoint(
                            requireText(center, "name", "center"),
                            requireDouble(center, "lat", "center"),
                            requireDouble(center, "lng", "center")
                    ),
                    pois
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load " + RESOURCE_PATH, exception);
        }
    }

    private Poi toPoi(JsonNode node) {
        String id = requireText(node, "id", "poi");
        JsonNode defaultAvailability = requireObject(node, "defaultAvailability", id);
        JsonNode scenarioFlags = requireObject(node, "scenarioFlags", id);

        return new Poi(
                id,
                requireText(node, "name", id),
                requireText(node, "category", id),
                requireText(node, "subCategory", id),
                toStringList(requireArray(node, "scenarios", id)),
                requireText(node, "address", id),
                requireDouble(node, "lat", id),
                requireDouble(node, "lng", id),
                requireDouble(node, "rating", id),
                toStringList(requireArray(node, "tags", id)),
                requireInt(node, "distanceMinutesFromCenter", id),
                requireInt(node, "pricePerPerson", id),
                toRange(requireObject(node, "ageRequirement", id), id + ".ageRequirement"),
                toRange(requireObject(node, "groupSize", id), id + ".groupSize"),
                requireText(node, "availabilityStatus", id),
                requireInt(node, "waitMinutes", id),
                new DefaultAvailability(
                        toAvailabilitySlot(requireObject(defaultAvailability, "weekdayAfternoon", id + ".defaultAvailability")),
                        toAvailabilitySlot(requireObject(defaultAvailability, "weekendAfternoon", id + ".defaultAvailability"))
                ),
                new PoiScenarioFlags(
                        requireBoolean(scenarioFlags, "family", id + ".scenarioFlags"),
                        requireBoolean(scenarioFlags, "friends", id + ".scenarioFlags")
                ),
                toStringList(requireArray(node, "actionTypes", id))
        );
    }

    private Range toRange(JsonNode node, String owner) {
        return new Range(requireInt(node, "min", owner), requireInt(node, "max", owner));
    }

    private AvailabilitySlot toAvailabilitySlot(JsonNode node) {
        return new AvailabilitySlot(
                requireBoolean(node, "available", "availability"),
                requireInt(node, "remaining", "availability"),
                requireInt(node, "waitMinutes", "availability")
        );
    }

    private List<String> toStringList(JsonNode array) {
        List<String> values = new ArrayList<>();
        for (JsonNode value : array) {
            if (value.isTextual()) {
                values.add(value.asText());
            }
        }
        return values;
    }

    private JsonNode requireObject(JsonNode node, String field, String owner) {
        JsonNode child = node.path(field);
        if (!child.isObject()) {
            throw new IllegalStateException(owner + "." + field + " should be an object");
        }
        return child;
    }

    private JsonNode requireArray(JsonNode node, String field, String owner) {
        JsonNode child = node.path(field);
        if (!child.isArray()) {
            throw new IllegalStateException(owner + "." + field + " should be an array");
        }
        return child;
    }

    private String requireText(JsonNode node, String field, String owner) {
        JsonNode child = node.path(field);
        if (!child.isTextual()) {
            throw new IllegalStateException(owner + "." + field + " should be a string");
        }
        return child.asText();
    }

    private int requireInt(JsonNode node, String field, String owner) {
        JsonNode child = node.path(field);
        if (!child.isNumber()) {
            throw new IllegalStateException(owner + "." + field + " should be a number");
        }
        return child.asInt();
    }

    private double requireDouble(JsonNode node, String field, String owner) {
        JsonNode child = node.path(field);
        if (!child.isNumber()) {
            throw new IllegalStateException(owner + "." + field + " should be a number");
        }
        return child.asDouble();
    }

    private boolean requireBoolean(JsonNode node, String field, String owner) {
        JsonNode child = node.path(field);
        if (!child.isBoolean()) {
            throw new IllegalStateException(owner + "." + field + " should be a boolean");
        }
        return child.asBoolean();
    }
}
