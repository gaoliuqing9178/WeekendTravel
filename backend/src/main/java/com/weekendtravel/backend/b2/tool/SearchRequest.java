package com.weekendtravel.backend.b2.tool;

import java.util.List;

public record SearchRequest(
        String scenario,
        List<String> categories,
        String keyword,
        Integer maxDistanceMinutes,
        Integer minAge,
        Integer groupSize,
        Integer limit
) {
    public SearchRequest {
        categories = categories == null ? List.of() : List.copyOf(categories);
    }
}
