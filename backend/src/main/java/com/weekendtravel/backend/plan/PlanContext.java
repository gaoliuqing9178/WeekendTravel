package com.weekendtravel.backend.plan;

import com.weekendtravel.backend.b2.tool.MessagePlanPayload;
import com.weekendtravel.backend.plan.api.CreatePlanRequest;

public record PlanContext(
        String planId,
        CreatePlanRequest request,
        PlanState currentState,
        int replanCount,
        int clarifyCount,
        int adjustCount,
        boolean injectedPlanB,
        String latestReason,
        MessagePlanPayload packedPlan,
        PendingClarification pendingClarification,
        PlanSelectionSnapshot selectionSnapshot,
        String pendingAdjustInstruction
) {
    public PlanContext withCurrentState(PlanState nextCurrentState) {
        return new PlanContext(planId, request, nextCurrentState, replanCount, clarifyCount, adjustCount, injectedPlanB, latestReason, packedPlan, pendingClarification, selectionSnapshot, pendingAdjustInstruction);
    }

    public PlanContext withReplanCount(int nextReplanCount) {
        return new PlanContext(planId, request, currentState, nextReplanCount, clarifyCount, adjustCount, injectedPlanB, latestReason, packedPlan, pendingClarification, selectionSnapshot, pendingAdjustInstruction);
    }

    public PlanContext withClarifyCount(int nextClarifyCount) {
        return new PlanContext(planId, request, currentState, replanCount, nextClarifyCount, adjustCount, injectedPlanB, latestReason, packedPlan, pendingClarification, selectionSnapshot, pendingAdjustInstruction);
    }

    public PlanContext withAdjustCount(int nextAdjustCount) {
        return new PlanContext(planId, request, currentState, replanCount, clarifyCount, nextAdjustCount, injectedPlanB, latestReason, packedPlan, pendingClarification, selectionSnapshot, pendingAdjustInstruction);
    }

    public PlanContext withInjectedPlanB(boolean nextInjectedPlanB) {
        return new PlanContext(planId, request, currentState, replanCount, clarifyCount, adjustCount, nextInjectedPlanB, latestReason, packedPlan, pendingClarification, selectionSnapshot, pendingAdjustInstruction);
    }

    public PlanContext withLatestReason(String nextLatestReason) {
        return new PlanContext(planId, request, currentState, replanCount, clarifyCount, adjustCount, injectedPlanB, nextLatestReason, packedPlan, pendingClarification, selectionSnapshot, pendingAdjustInstruction);
    }

    public PlanContext withPackedPlan(MessagePlanPayload nextPackedPlan) {
        return new PlanContext(planId, request, currentState, replanCount, clarifyCount, adjustCount, injectedPlanB, latestReason, nextPackedPlan, pendingClarification, selectionSnapshot, pendingAdjustInstruction);
    }

    public PlanContext withPendingClarification(PendingClarification nextPendingClarification) {
        return new PlanContext(planId, request, currentState, replanCount, clarifyCount, adjustCount, injectedPlanB, latestReason, packedPlan, nextPendingClarification, selectionSnapshot, pendingAdjustInstruction);
    }

    public PlanContext clearPendingClarification() {
        return new PlanContext(planId, request, currentState, replanCount, clarifyCount, adjustCount, injectedPlanB, latestReason, packedPlan, null, selectionSnapshot, pendingAdjustInstruction);
    }

    public PlanContext withSelectionSnapshot(PlanSelectionSnapshot nextSelectionSnapshot) {
        return new PlanContext(planId, request, currentState, replanCount, clarifyCount, adjustCount, injectedPlanB, latestReason, packedPlan, pendingClarification, nextSelectionSnapshot, pendingAdjustInstruction);
    }

    public PlanContext withPendingAdjustInstruction(String nextPendingAdjustInstruction) {
        return new PlanContext(planId, request, currentState, replanCount, clarifyCount, adjustCount, injectedPlanB, latestReason, packedPlan, pendingClarification, selectionSnapshot, nextPendingAdjustInstruction);
    }
}
