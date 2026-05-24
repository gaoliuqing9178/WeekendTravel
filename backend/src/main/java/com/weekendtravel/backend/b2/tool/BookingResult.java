package com.weekendtravel.backend.b2.tool;

public record BookingResult(
        String type,
        String planId,
        String actionId,
        String actionType,
        String status,
        String confirmationNo,
        String message,
        long timestamp,
        long latencyMs
) {
}
