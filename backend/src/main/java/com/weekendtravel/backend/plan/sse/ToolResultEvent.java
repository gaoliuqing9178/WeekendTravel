package com.weekendtravel.backend.plan.sse;

public record ToolResultEvent(
        String type,
        String planId,
        String tool,
        String status,
        String outputSummary,
        long latencyMs,
        long timestamp
) {
}
