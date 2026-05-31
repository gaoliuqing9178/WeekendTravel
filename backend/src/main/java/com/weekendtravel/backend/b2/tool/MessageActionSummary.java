package com.weekendtravel.backend.b2.tool;

public record MessageActionSummary(
        String actionId,
        String actionType,
        String targetPoiId,
        String description,
        String status,
        String confirmationNo
) {
    public MessageActionSummary(
            String actionId,
            String actionType,
            String description,
            String status,
            String confirmationNo
    ) {
        this(actionId, actionType, null, description, status, confirmationNo);
    }
}
