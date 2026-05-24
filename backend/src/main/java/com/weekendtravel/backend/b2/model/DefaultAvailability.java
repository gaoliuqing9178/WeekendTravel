package com.weekendtravel.backend.b2.model;

public record DefaultAvailability(
        AvailabilitySlot weekdayAfternoon,
        AvailabilitySlot weekendAfternoon
) {
}
