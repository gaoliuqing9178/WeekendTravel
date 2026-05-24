package com.weekendtravel.backend.b2.tool;

public record AvailabilityRequest(
        String poiId,
        String slot,
        Integer minAge,
        Integer groupSize
) {
}
