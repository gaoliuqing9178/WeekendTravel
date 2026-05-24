package com.weekendtravel.backend.b2.tool;

import com.weekendtravel.backend.b2.model.GeoPoint;
import com.weekendtravel.backend.b2.model.Poi;
import com.weekendtravel.backend.b2.repository.PoiRepository;
import org.springframework.stereotype.Service;

@Service
public class RouteTool {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final PoiRepository poiRepository;

    public RouteTool(PoiRepository poiRepository) {
        this.poiRepository = poiRepository;
    }

    public RouteResult execute(RouteRequest request) {
        return calculateRouteTime(request);
    }

    public RouteResult calculateRouteTime(RouteRequest request) {
        long start = System.currentTimeMillis();
        if (request == null || request.toPoiId() == null || request.toPoiId().isBlank()) {
            throw new IllegalArgumentException("toPoiId is required");
        }

        Poi toPoi = poiRepository.findById(request.toPoiId())
                .orElseThrow(() -> new IllegalArgumentException("unknown toPoiId: " + request.toPoiId()));

        Location from = resolveFrom(request.fromPoiId());
        Location to = new Location(toPoi.id(), toPoi.name(), toPoi.lat(), toPoi.lng(), false);
        int distanceMinutes = calculateMinutes(from, to, toPoi);
        String summary = buildSummary(from.name(), to.name(), distanceMinutes);

        return new RouteResult(
                from.name(),
                to.name(),
                distanceMinutes,
                summary,
                System.currentTimeMillis() - start
        );
    }

    private Location resolveFrom(String fromPoiId) {
        if (fromPoiId == null
                || fromPoiId.isBlank()
                || "origin".equalsIgnoreCase(fromPoiId)
                || "current".equalsIgnoreCase(fromPoiId)
                || "center".equalsIgnoreCase(fromPoiId)) {
            GeoPoint center = poiRepository.center();
            return new Location("origin", center.name(), center.lat(), center.lng(), true);
        }

        Poi poi = poiRepository.findById(fromPoiId)
                .orElseThrow(() -> new IllegalArgumentException("unknown fromPoiId: " + fromPoiId));
        return new Location(poi.id(), poi.name(), poi.lat(), poi.lng(), false);
    }

    private int calculateMinutes(Location from, Location to, Poi toPoi) {
        if (from.id().equals(to.id())) {
            return 0;
        }
        if (from.center()) {
            return Math.max(1, toPoi.distanceMinutesFromCenter());
        }

        double km = haversineKm(from.lat(), from.lng(), to.lat(), to.lng());
        return Math.max(3, (int) Math.ceil(5 + km * 6.5));
    }

    private double haversineKm(double fromLat, double fromLng, double toLat, double toLng) {
        double dLat = Math.toRadians(toLat - fromLat);
        double dLng = Math.toRadians(toLng - fromLng);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(fromLat))
                * Math.cos(Math.toRadians(toLat))
                * Math.pow(Math.sin(dLng / 2), 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private String buildSummary(String fromName, String toName, int distanceMinutes) {
        if (distanceMinutes == 0) {
            return "从" + fromName + "到" + toName + "为同一地点，无需移动。";
        }
        String mode = distanceMinutes <= 12 ? "短途移动" : distanceMinutes <= 25 ? "打车或公交短驳" : "路线偏远，建议后续校验";
        return "从" + fromName + "到" + toName + "约" + distanceMinutes + "分钟，建议" + mode + "。";
    }

    private record Location(
            String id,
            String name,
            double lat,
            double lng,
            boolean center
    ) {
    }
}
