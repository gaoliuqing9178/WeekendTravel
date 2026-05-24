package com.weekendtravel.backend.b2.tool;

import java.util.List;

public record SearchResult(
        List<SearchCandidate> candidates,
        long latencyMs
) {
    public SearchResult {
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
    }
}
