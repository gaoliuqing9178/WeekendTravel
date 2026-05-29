package com.weekendtravel.backend.plan.sse;

public record ErrorEvent(
        String type,
        String planId,
        String code,
        String message,
        long timestamp
) {
}
