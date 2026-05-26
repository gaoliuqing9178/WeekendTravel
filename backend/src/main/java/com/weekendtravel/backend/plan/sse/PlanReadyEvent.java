package com.weekendtravel.backend.plan.sse;

import com.weekendtravel.backend.b2.tool.MessagePlanPayload;

public record PlanReadyEvent(
        String type,
        String planId,
        MessagePlanPayload plan
) {
}
