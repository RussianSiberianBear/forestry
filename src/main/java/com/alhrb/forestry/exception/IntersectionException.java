package com.alhrb.forestry.exception;

public class IntersectionException extends RuntimeException {

    public IntersectionException(String message) {
        super(message);
    }

    public IntersectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
