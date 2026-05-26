package com.weekendtravel.backend.plan;

import com.weekendtravel.backend.b2.tool.MessagePlanPayload;
import com.weekendtravel.backend.plan.api.CreatePlanRequest;

public record PlanContext(
        String planId,
        CreatePlanRequest request,
        int replanCount,
        MessagePlanPayload packedPlan
) {
    public PlanContext withReplanCount(int nextReplanCount) {
        return new PlanContext(planId, request, nextReplanCount, packedPlan);
    }

    public PlanContext withPackedPlan(MessagePlanPayload nextPackedPlan) {
        return new PlanContext(planId, request, replanCount, nextPackedPlan);
    }
}
