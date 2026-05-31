package com.weekendtravel.backend.plan.sse;

public record ExecuteResultEvent(
        String type,
        String planId,
        String actionId,
        String actionType,
        String status,
        String confirmationNo,
        long timestamp
) {
}
