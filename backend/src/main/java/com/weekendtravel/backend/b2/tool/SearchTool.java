package com.weekendtravel.backend.b2.tool;

import com.weekendtravel.backend.b2.model.Poi;
import com.weekendtravel.backend.b2.repository.PoiRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class SearchTool {

    private static final int DEFAULT_LIMIT = 8;
    private static final int DEFAULT_MAX_DISTANCE_MINUTES = 40;
    private static final Set<String> VALID_SCENARIOS = Set.of("family", "friends");

    private final PoiRepository poiRepository;

    public SearchTool(PoiRepository poiRepository) {
        this.poiRepository = poiRepository;
    }

    public SearchResult execute(SearchRequest request) {
        return searchLocalPlaces(request);
    }

    public SearchResult searchLocalPlaces(SearchRequest request) {
        long start = System.currentTimeMillis();
        SearchRequest normalized = normalize(request);

        List<SearchCandidate> candidates = poiRepository.findAll().stream()
                .filter(poi -> poi.supportsScenario(normalized.scenario()))
                .filter(poi -> matchesCategories(poi, normalized.categories()))
                .filter(poi -> matchesKeyword(poi, normalized.keyword()))
                .filter(poi -> poi.distanceMinutesFromCenter() <= normalized.maxDistanceMinutes())
                .filter(poi -> poi.supportsAge(normalized.minAge()))
                .filter(poi -> poi.supportsGroupSize(normalized.groupSize()))
                .map(poi -> toCandidate(poi, normalized))
                .sorted(Comparator.comparingDouble(SearchCandidate::score).reversed()
                        .thenComparing(candidate -> candidate.poi().distanceMinutesFromCenter())
                        .thenComparing(candidate -> candidate.poi().id()))
                .limit(normalized.limit())
                .toList();

        return new SearchResult(candidates, System.currentTimeMillis() - start);
    }

    private SearchRequest normalize(SearchRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("search request is required");
        }
        String scenario = requireScenario(request.scenario());
        int maxDistanceMinutes = request.maxDistanceMinutes() == null
                ? DEFAULT_MAX_DISTANCE_MINUTES
                : request.maxDistanceMinutes();
        if (maxDistanceMinutes < 1) {
            throw new IllegalArgumentException("maxDistanceMinutes must be positive");
        }
        int limit = request.limit() == null ? DEFAULT_LIMIT : request.limit();
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be positive");
        }

        return new SearchRequest(
                scenario,
                normalizeCategories(request.categories()),
                normalizeKeyword(request.keyword()),
                maxDistanceMinutes,
                request.minAge(),
                request.groupSize(),
                limit
        );
    }

    private String requireScenario(String scenario) {
        if (scenario == null || scenario.isBlank()) {
            throw new IllegalArgumentException("scenario is required");
        }
        String normalized = scenario.trim().toLowerCase(Locale.ROOT);
        if (!VALID_SCENARIOS.contains(normalized)) {
            throw new IllegalArgumentException("scenario must be family or friends");
        }
        return normalized;
    }

    private List<String> normalizeCategories(List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new HashSet<>();
        for (String category : categories) {
            if (category != null && !category.isBlank()) {
                normalized.add(category.trim().toLowerCase(Locale.ROOT));
            }
        }
        return normalized.stream().sorted().toList();
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "";
        }
        return keyword.trim().toLowerCase(Locale.ROOT);
    }

    private boolean matchesCategories(Poi poi, List<String> categories) {
        return categories.isEmpty() || categories.contains(poi.category().toLowerCase(Locale.ROOT));
    }

    private boolean matchesKeyword(Poi poi, String keyword) {
        if (keyword.isBlank()) {
            return true;
        }
        return searchableText(poi).contains(keyword);
    }

    private SearchCandidate toCandidate(Poi poi, SearchRequest request) {
        List<String> reasons = new ArrayList<>();
        if (poi.supportsScenario(request.scenario())) {
            reasons.add("scenario:" + request.scenario());
        }
        if (!request.categories().isEmpty() && request.categories().contains(poi.category())) {
            reasons.add("category:" + poi.category());
        }
        if (!request.keyword().isBlank() && searchableText(poi).contains(request.keyword())) {
            reasons.add("keyword:" + request.keyword());
        }
        if (poi.distanceMinutesFromCenter() <= request.maxDistanceMinutes()) {
            reasons.add("distance<=" + request.maxDistanceMinutes());
        }

        return new SearchCandidate(poi, round(score(poi, request)), reasons);
    }

    private double score(Poi poi, SearchRequest request) {
        double relevance = relevanceScore(poi, request);
        double distance = distanceScore(poi.distanceMinutesFromCenter(), request.maxDistanceMinutes());
        double rating = Math.min(1.0, poi.rating() / 5.0);
        double availability = availabilityScore(poi.availabilityStatus());
        return 0.4 * relevance + 0.3 * distance + 0.2 * rating + 0.1 * availability;
    }

    private double relevanceScore(Poi poi, SearchRequest request) {
        double score = 0.45;
        if (poi.supportsScenario(request.scenario())) {
            score += 0.25;
        }
        if (!request.categories().isEmpty() && request.categories().contains(poi.category())) {
            score += 0.15;
        }
        if (!request.keyword().isBlank() && searchableText(poi).contains(request.keyword())) {
            score += 0.15;
        }
        return Math.min(1.0, score);
    }

    private double distanceScore(int distanceMinutes, int maxDistanceMinutes) {
        double bounded = Math.min(distanceMinutes, maxDistanceMinutes);
        return Math.max(0.0, 1.0 - bounded / maxDistanceMinutes);
    }

    private double availabilityScore(String availabilityStatus) {
        return switch (availabilityStatus) {
            case "available" -> 1.0;
            case "limited" -> 0.55;
            case "full" -> 0.0;
            default -> 0.25;
        };
    }

    private String searchableText(Poi poi) {
        return (poi.name()
                + " " + poi.category()
                + " " + poi.subCategory()
                + " " + poi.address()
                + " " + String.join(" ", poi.tags()))
                .toLowerCase(Locale.ROOT);
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
