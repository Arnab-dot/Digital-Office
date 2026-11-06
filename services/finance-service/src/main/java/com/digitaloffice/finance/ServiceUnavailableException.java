package com.digitaloffice.finance;

/** Raised when an authorization dependency cannot be contacted. */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
