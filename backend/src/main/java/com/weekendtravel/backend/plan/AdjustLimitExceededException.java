package com.weekendtravel.backend.plan;

public class AdjustLimitExceededException extends RuntimeException {
    public AdjustLimitExceededException(String message) {
        super(message);
    }
}
