package com.weekendtravel.backend.plan.sse;

public record ReplanEvent(
        String type,
        String planId,
        String reason,
        int replanCount,
        long timestamp
) {
}
