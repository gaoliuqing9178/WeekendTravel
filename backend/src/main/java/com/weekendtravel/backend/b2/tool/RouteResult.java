package com.weekendtravel.backend.b2.tool;

public record RouteResult(
        String fromName,
        String toName,
        int distanceMinutes,
        String summary,
        long latencyMs
) {
}
