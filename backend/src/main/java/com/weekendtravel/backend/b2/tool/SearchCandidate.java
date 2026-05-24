package com.weekendtravel.backend.b2.tool;

import com.weekendtravel.backend.b2.model.Poi;

import java.util.List;

public record SearchCandidate(
        Poi poi,
        double score,
        List<String> matchedReasons
) {
    public SearchCandidate {
        matchedReasons = matchedReasons == null ? List.of() : List.copyOf(matchedReasons);
    }
}
