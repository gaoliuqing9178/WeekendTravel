package com.weekendtravel.backend.plan.api;

public record ClarifyPlanResponse(
        String planId,
        String status,
        String message
) {
}
