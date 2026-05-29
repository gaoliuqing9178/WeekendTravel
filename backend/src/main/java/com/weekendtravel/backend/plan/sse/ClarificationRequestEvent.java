package com.weekendtravel.backend.plan.sse;

import java.util.List;

public record ClarificationRequestEvent(
        String type,
        String planId,
        String question,
        String field,
        List<String> options,
        long timestamp
) {
    public ClarificationRequestEvent {
        options = options == null ? List.of() : List.copyOf(options);
    }
}
