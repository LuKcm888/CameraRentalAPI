package com.camerarental.backend.exceptions;

/**
 * Thrown when a request conflicts with the current state of a resource —
 * for example, attempting to hard-delete a camera that still has linked
 * inventory or physical units.
 *
 * <p>Mapped to HTTP {@code 409 Conflict} by
 * {@link MyGlobalExceptionHandler}.</p>
 */
public class ResourceConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceConflictException(String message) {
        super(message);
    }
}
