package com.digitaloffice.work;

/**
 * Thrown when a downstream internal service is unreachable or returns an error.
 * Maps to HTTP 503 via {@link GlobalExceptionHandler}.
 */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
