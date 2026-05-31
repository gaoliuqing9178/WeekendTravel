package com.weekendtravel.backend.plan.sse;

public record DoneEvent(
        String type,
        String planId,
        String summary,
        long timestamp
) {
}
