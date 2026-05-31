package com.weekendtravel.backend.b2.tool;

import java.util.List;

public record MessagePoiPayload(
        String id,
        String name,
        String category,
        String address,
        double rating,
        int distanceMinutes,
        List<String> tags,
        String availabilityStatus,
        Integer waitMinutes
) {
    public MessagePoiPayload {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
