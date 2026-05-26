package com.weekendtravel.backend.plan.api;

public record CreatePlanRequest(
        String text,
        String scenario,
        String origin
) {
}
