package com.digitaloffice.client;

/**
 * Thrown when a downstream service call fails due to connection failure or timeout.
 * Maps to HTTP 503 via {@link GlobalExceptionHandler}.
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
