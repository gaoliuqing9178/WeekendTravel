package com.weekendtravel.backend.plan.api;

public record ExecutePlanResponse(
        String planId,
        String status,
        String message
) {
}
