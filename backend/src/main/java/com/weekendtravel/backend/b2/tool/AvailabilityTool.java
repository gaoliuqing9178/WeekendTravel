package com.weekendtravel.backend.b2.tool;

import com.weekendtravel.backend.b2.model.AvailabilitySlot;
import com.weekendtravel.backend.b2.model.DefaultAvailability;
import com.weekendtravel.backend.b2.model.Poi;
import com.weekendtravel.backend.b2.repository.PoiRepository;
import com.weekendtravel.backend.b2.scenario.ScenarioFlags;
import com.weekendtravel.backend.b2.scenario.ScenarioFlagsState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AvailabilityTool {

    private static final String DEFAULT_SLOT = "weekendAfternoon";
    private static final String WEEKDAY_AFTERNOON = "weekdayAfternoon";
    private static final String WEEKEND_AFTERNOON = "weekendAfternoon";
    private static final int RESTAURANT_FULL_WAIT_MINUTES = 70;

    private final PoiRepository poiRepository;
    private final ScenarioFlags scenarioFlags;

    public AvailabilityTool(PoiRepository poiRepository) {
        this(poiRepository, new ScenarioFlags());
    }

    @Autowired
    public AvailabilityTool(PoiRepository poiRepository, ScenarioFlags scenarioFlags) {
        this.poiRepository = poiRepository;
        this.scenarioFlags = scenarioFlags;
    }

    public AvailabilityResult execute(AvailabilityRequest request) {
        return checkAvailability(request);
    }

    public AvailabilityResult checkAvailability(AvailabilityRequest request) {
        long start = System.currentTimeMillis();
        AvailabilityRequest normalized = normalize(request);
        Poi poi = poiRepository.findById(normalized.poiId())
                .orElseThrow(() -> new IllegalArgumentException("unknown poiId: " + normalized.poiId()));
        AvailabilitySlot slot = slot(poi.defaultAvailability(), normalized.slot());
        ScenarioFlagsState flags = scenarioFlags.current();

        boolean ageMatched = poi.supportsAge(normalized.minAge());
        boolean groupSizeMatched = poi.supportsGroupSize(normalized.groupSize());
        List<String> reasons = new ArrayList<>();
        reasons.add("defaultAvailability:" + normalized.slot());

        if (flags.ageMismatch() && normalized.minAge() != null) {
            ageMatched = false;
            reasons.add("scenario:ageMismatch");
        }
        if (!ageMatched) {
            reasons.add("ageMismatch");
        }
        if (!groupSizeMatched) {
            reasons.add("groupSizeMismatch");
        }

        int remaining = slot.remaining();
        int waitMinutes = slot.waitMinutes();
        String availabilityStatus = statusFromSlot(slot);
        boolean available = slot.available() && remaining > 0 && ageMatched && groupSizeMatched;

        if (flags.restaurantFull() && "restaurant".equals(poi.category())) {
            available = false;
            remaining = 0;
            waitMinutes = Math.max(waitMinutes, RESTAURANT_FULL_WAIT_MINUTES);
            availabilityStatus = "full";
            reasons.add("scenario:restaurantFull");
        }

        return new AvailabilityResult(
                poi.id(),
                poi.name(),
                normalized.slot(),
                available,
                availabilityStatus,
                remaining,
                waitMinutes,
                ageMatched,
                groupSizeMatched,
                reasons,
                flags,
                System.currentTimeMillis() - start
        );
    }

    private AvailabilityRequest normalize(AvailabilityRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("availability request is required");
        }
        if (request.poiId() == null || request.poiId().isBlank()) {
            throw new IllegalArgumentException("poiId is required");
        }

        return new AvailabilityRequest(
                request.poiId().trim(),
                normalizeSlot(request.slot()),
                request.minAge(),
                request.groupSize()
        );
    }

    private String normalizeSlot(String slot) {
        if (slot == null || slot.isBlank()) {
            return DEFAULT_SLOT;
        }
        String normalized = slot.trim();
        if (!WEEKDAY_AFTERNOON.equals(normalized) && !WEEKEND_AFTERNOON.equals(normalized)) {
            throw new IllegalArgumentException("slot must be weekdayAfternoon or weekendAfternoon");
        }
        return normalized;
    }

    private AvailabilitySlot slot(DefaultAvailability defaultAvailability, String slot) {
        return switch (slot) {
            case WEEKDAY_AFTERNOON -> defaultAvailability.weekdayAfternoon();
            case WEEKEND_AFTERNOON -> defaultAvailability.weekendAfternoon();
            default -> throw new IllegalArgumentException("slot must be weekdayAfternoon or weekendAfternoon");
        };
    }

    private String statusFromSlot(AvailabilitySlot slot) {
        if (!slot.available() || slot.remaining() <= 0) {
            return "full";
        }
        if (slot.remaining() <= 5 || slot.waitMinutes() >= 15) {
            return "limited";
        }
        return "available";
    }
}
