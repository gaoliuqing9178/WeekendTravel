package com.weekendtravel.backend.plan;

public class InvalidPlanStateException extends RuntimeException {
    public InvalidPlanStateException(String message) {
        super(message);
    }
}
