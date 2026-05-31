package com.weekendtravel.backend.b2.tool;

import java.util.List;

public record MessageTimeSlot(
        int order,
        String type,
        String title,
        String poiName,
        MessagePoiPayload poi,
        String startTime,
        String endTime,
        Integer distanceMinutes,
        List<String> notes
) {
    public MessageTimeSlot {
        poi = poi == null
                ? new MessagePoiPayload(null, poiName, type, "", 0.0, distanceMinutes == null ? 0 : distanceMinutes, List.of(), "available", null)
                : poi;
        notes = notes == null ? List.of() : List.copyOf(notes);
    }

    public MessageTimeSlot(
            int order,
            String type,
            String title,
            String poiName,
            String startTime,
            String endTime,
            Integer distanceMinutes,
            List<String> notes
    ) {
        this(order, type, title, poiName, null, startTime, endTime, distanceMinutes, notes);
    }
}
