package com.weekendtravel.backend.b2.tool;

public record MessageActionSummary(
        String actionId,
        String actionType,
        String description,
        String status,
        String confirmationNo
) {
}
