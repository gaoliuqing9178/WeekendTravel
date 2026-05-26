package com.weekendtravel.backend.plan.sse;

public record HeartbeatEvent(
        String type,
        String planId,
        long timestamp
) {
}
