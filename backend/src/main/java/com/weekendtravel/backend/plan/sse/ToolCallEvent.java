package com.weekendtravel.backend.plan.sse;

public record ToolCallEvent(
        String type,
        String planId,
        String tool,
        String status,
        String inputSummary,
        long timestamp
) {
}
