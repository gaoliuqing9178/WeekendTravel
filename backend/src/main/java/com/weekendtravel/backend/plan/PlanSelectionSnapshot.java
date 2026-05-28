package com.weekendtravel.backend.plan;

import com.weekendtravel.backend.b2.tool.SearchCandidate;

import java.util.List;

public record PlanSelectionSnapshot(
        SearchCandidate activity,
        SearchCandidate restaurant,
        List<SearchCandidate> activityCandidates,
        List<SearchCandidate> restaurantCandidates
) {
    public PlanSelectionSnapshot {
        activityCandidates = activityCandidates == null ? List.of() : List.copyOf(activityCandidates);
        restaurantCandidates = restaurantCandidates == null ? List.of() : List.copyOf(restaurantCandidates);
    }

    public PlanSelectionSnapshot withActivity(SearchCandidate nextActivity) {
        return new PlanSelectionSnapshot(nextActivity, restaurant, activityCandidates, restaurantCandidates);
    }

    public PlanSelectionSnapshot withRestaurant(SearchCandidate nextRestaurant) {
        return new PlanSelectionSnapshot(activity, nextRestaurant, activityCandidates, restaurantCandidates);
    }
}
