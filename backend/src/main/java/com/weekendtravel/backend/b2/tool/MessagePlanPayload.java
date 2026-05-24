package com.weekendtravel.backend.b2.tool;

import java.util.List;

public record MessagePlanPayload(
        String planId,
        String scenario,
        String status,
        boolean isPlanB,
        String planBReason,
        String summary,
        List<MessageTimeSlot> timeline,
        List<MessageActionSummary> actions,
        String shareMessage,
        double totalDurationHours,
        int replanCount,
        String createdAt
) {
    public MessagePlanPayload {
        timeline = timeline == null ? List.of() : List.copyOf(timeline);
        actions = actions == null ? List.of() : List.copyOf(actions);
    }
}
