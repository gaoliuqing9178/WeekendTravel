package com.weekendtravel.backend.b2.scenario;

import java.util.ArrayList;
import java.util.List;

public record ScenarioFlagsState(
        boolean restaurantFull,
        boolean routeTooFar,
        boolean bookingFail,
        boolean ageMismatch
) {
    public static ScenarioFlagsState disabled() {
        return new ScenarioFlagsState(false, false, false, false);
    }

    public List<String> activeNames() {
        List<String> names = new ArrayList<>();
        if (restaurantFull) {
            names.add("restaurantFull");
        }
        if (routeTooFar) {
            names.add("routeTooFar");
        }
        if (bookingFail) {
            names.add("bookingFail");
        }
        if (ageMismatch) {
            names.add("ageMismatch");
        }
        return List.copyOf(names);
    }
}
