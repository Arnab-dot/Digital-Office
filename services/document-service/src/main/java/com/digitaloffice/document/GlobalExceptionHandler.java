package com.digitaloffice.document;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

/**
 * Centralised exception-to-HTTP-status mapping for document-service.
 *
 * <ul>
 *   <li>{@link ServiceUnavailableException}  → 503 Service Unavailable</li>
 *   <li>{@link NoSuchElementException}       → 404 Not Found</li>
 *   <li>{@link AccessDeniedException}        → 403 Forbidden</li>
 *   <li>{@link IllegalStateException}        → 409 Conflict</li>
 *   <li>{@link IllegalArgumentException}     → 422 Unprocessable Entity</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleServiceUnavailable(ServiceUnavailableException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(NoSuchElementException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleForbidden(AccessDeniedException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflict(IllegalStateException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleUnprocessable(IllegalArgumentException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    public record ErrorResponse(String message) {}
}
