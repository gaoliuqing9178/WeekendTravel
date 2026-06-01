package com.weekendtravel.backend.plan;

import com.weekendtravel.backend.b2.tool.SearchCandidate;

import java.util.List;

public record PlanSelectionSnapshot(
        SearchCandidate activity,
        SearchCandidate socialStop,
        SearchCandidate restaurant,
        List<SearchCandidate> activityCandidates,
        List<SearchCandidate> socialStopCandidates,
        List<SearchCandidate> restaurantCandidates
) {
    public PlanSelectionSnapshot {
        activityCandidates = activityCandidates == null ? List.of() : List.copyOf(activityCandidates);
        socialStopCandidates = socialStopCandidates == null ? List.of() : List.copyOf(socialStopCandidates);
        restaurantCandidates = restaurantCandidates == null ? List.of() : List.copyOf(restaurantCandidates);
    }

    public PlanSelectionSnapshot withActivity(SearchCandidate nextActivity) {
        return new PlanSelectionSnapshot(
                nextActivity,
                socialStop,
                restaurant,
                activityCandidates,
                socialStopCandidates,
                restaurantCandidates
        );
    }

    public PlanSelectionSnapshot withRestaurant(SearchCandidate nextRestaurant) {
        return new PlanSelectionSnapshot(
                activity,
                socialStop,
                nextRestaurant,
                activityCandidates,
                socialStopCandidates,
                restaurantCandidates
        );
    }
}
