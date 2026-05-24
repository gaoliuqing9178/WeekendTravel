package com.weekendtravel.backend.b2.tool;

import java.util.List;

public record MessageTimeSlot(
        int order,
        String type,
        String title,
        String poiName,
        String startTime,
        String endTime,
        Integer distanceMinutes,
        List<String> notes
) {
    public MessageTimeSlot {
        notes = notes == null ? List.of() : List.copyOf(notes);
    }
}
