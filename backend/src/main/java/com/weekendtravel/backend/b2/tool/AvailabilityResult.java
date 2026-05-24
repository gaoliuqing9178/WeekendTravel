package com.weekendtravel.backend.b2.tool;

import com.weekendtravel.backend.b2.scenario.ScenarioFlagsState;

import java.util.List;

public record AvailabilityResult(
        String poiId,
        String poiName,
        String slot,
        boolean available,
        String availabilityStatus,
        int remaining,
        int waitMinutes,
        boolean ageMatched,
        boolean groupSizeMatched,
        List<String> reasons,
        ScenarioFlagsState scenarioFlags,
        long latencyMs
) {
    public AvailabilityResult {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }
}
