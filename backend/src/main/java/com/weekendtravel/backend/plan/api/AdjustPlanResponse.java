package com.weekendtravel.backend.plan.api;

public record AdjustPlanResponse(
        String planId,
        String status,
        String message
) {
}
