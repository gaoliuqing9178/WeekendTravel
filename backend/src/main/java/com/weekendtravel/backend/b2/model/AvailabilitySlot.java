package com.weekendtravel.backend.b2.model;

public record AvailabilitySlot(
        boolean available,
        int remaining,
        int waitMinutes
) {
}
