package com.weekendtravel.backend.b2.model;

import java.util.List;

public record PoiCatalog(
        int version,
        String updatedAt,
        String city,
        GeoPoint center,
        List<Poi> pois
) {
    public PoiCatalog {
        pois = pois == null ? List.of() : List.copyOf(pois);
    }
}
