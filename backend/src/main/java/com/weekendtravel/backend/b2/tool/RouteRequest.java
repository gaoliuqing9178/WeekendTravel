package com.weekendtravel.backend.b2.tool;

public record RouteRequest(
        String fromPoiId,
        String toPoiId
) {
}
