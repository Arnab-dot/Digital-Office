package com.digitaloffice.communication;

/**
 * Thrown when a downstream service (e.g. Organization Service) is unreachable.
 * Maps to HTTP 503 via {@link GlobalExceptionHandler}.
 */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) { super(message); }
    public ServiceUnavailableException(String message, Throwable cause) { super(message, cause); }
}
