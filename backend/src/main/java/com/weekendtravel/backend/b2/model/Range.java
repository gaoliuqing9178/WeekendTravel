package com.weekendtravel.backend.b2.model;

public record Range(
        int min,
        int max
) {
    public boolean includes(int value) {
        return value >= min && value <= max;
    }
}
