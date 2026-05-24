package com.weekendtravel.backend.b2.tool;

import java.util.List;

public record MessageRequest(
        String planId,
        String scenario,
        String status,
        Boolean isPlanB,
        String planBReason,
        String summary,
        List<MessageTimeSlot> timeline,
        List<MessageActionSummary> actions,
        Double totalDurationHours,
        Integer replanCount,
        String createdAt
) {
    public MessageRequest {
        timeline = timeline == null ? List.of() : List.copyOf(timeline);
        actions = actions == null ? List.of() : List.copyOf(actions);
    }
}
