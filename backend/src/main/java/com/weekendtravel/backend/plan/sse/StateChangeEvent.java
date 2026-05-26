package com.weekendtravel.backend.plan.sse;

public record StateChangeEvent(
        String type,
        String planId,
        String from,
        String to,
        long timestamp
) {
}
