package com.weekendtravel.backend.plan.sse;

import com.weekendtravel.backend.b2.tool.MessagePlanPayload;

import java.util.List;

public record AdjustResultEvent(
        String type,
        String planId,
        List<String> affectedSlots,
        String summary,
        MessagePlanPayload plan
) {
    public AdjustResultEvent {
        affectedSlots = affectedSlots == null ? List.of() : List.copyOf(affectedSlots);
    }
}
