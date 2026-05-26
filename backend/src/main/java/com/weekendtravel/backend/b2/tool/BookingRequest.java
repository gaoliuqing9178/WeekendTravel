package com.weekendtravel.backend.b2.tool;

public record BookingRequest(
        String planId,
        String actionId,
        String actionType,
        String targetPoiId,
        String description,
        String idempotencyKey,
        String previousConfirmationNo
) {
}
