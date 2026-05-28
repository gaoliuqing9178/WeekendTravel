package com.weekendtravel.backend.plan;

import com.weekendtravel.backend.b2.tool.MessagePlanPayload;
import com.weekendtravel.backend.plan.api.CreatePlanRequest;

public record PlanContext(
        String planId,
        CreatePlanRequest request,
        int replanCount,
        boolean injectedPlanB,
        String latestReason,
        MessagePlanPayload packedPlan
) {
    public PlanContext withReplanCount(int nextReplanCount) {
        return new PlanContext(planId, request, nextReplanCount, injectedPlanB, latestReason, packedPlan);
    }

    public PlanContext withInjectedPlanB(boolean nextInjectedPlanB) {
        return new PlanContext(planId, request, replanCount, nextInjectedPlanB, latestReason, packedPlan);
    }

    public PlanContext withLatestReason(String nextLatestReason) {
        return new PlanContext(planId, request, replanCount, injectedPlanB, nextLatestReason, packedPlan);
    }

    public PlanContext withPackedPlan(MessagePlanPayload nextPackedPlan) {
        return new PlanContext(planId, request, replanCount, injectedPlanB, latestReason, nextPackedPlan);
    }
}
