package com.weekendtravel.backend.dto;

public record HealthResponse(
        String status,
        String service,
        long timestamp
) {
}
