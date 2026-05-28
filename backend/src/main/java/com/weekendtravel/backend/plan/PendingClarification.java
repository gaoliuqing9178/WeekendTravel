package com.weekendtravel.backend.plan;

import java.util.List;

public record PendingClarification(
        String field,
        String question,
        List<String> options,
        String reply
) {
    public PendingClarification {
        options = options == null ? List.of() : List.copyOf(options);
    }

    public PendingClarification withReply(String nextReply) {
        return new PendingClarification(field, question, options, nextReply);
    }
}
