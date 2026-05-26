package com.weekendtravel.backend.api;

import java.util.Map;

public record ApiErrorResponse(
        String error,
        String message,
        Map<String, Object> details
) {
}
