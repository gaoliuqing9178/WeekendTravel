package com.weekendtravel.backend.b2.tool;

public record MessageResult(
        String planId,
        String scenario,
        String shareMessage,
        MessagePlanPayload plan,
        boolean llmUsed,
        String templateVersion,
        long latencyMs
) {
}
