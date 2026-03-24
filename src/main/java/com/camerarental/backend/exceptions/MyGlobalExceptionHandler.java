package com.camerarental.backend.exceptions;


import com.camerarental.backend.payload.base.APIResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;
/**
 * This class is used to handle all the exceptions in the application.
 * It is annotated with @RestControllerAdvice to use Spring's global exception handling.
 * 
 * The class contains two methods:
 * - handleAll: This method handles all the exceptions in the application.
 * - myAPIException: This method specifically handles API exceptions.
 * 
 * All methods use Spring Framework's ResponseEntity to return the API response.
 */
@Slf4j
@RestControllerAdvice
public class MyGlobalExceptionHandler {

    /**
     * This method is used to handle all the exceptions in the application.
     * @param e The exception to handle.
     * @param request The HTTP request.
     * @return The API response.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIResponse> handleAll(Exception e, HttpServletRequest request) {
        log.error("unhandled_exception path={} exception={}", request.getRequestURI(), e.getClass().getSimpleName(), e);
        APIResponse apiResponse = new APIResponse("Internal server error", false);
        return new ResponseEntity<>(apiResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }



    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> myMethodArgumentNotValidException(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        Map<String, String> response = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(err -> {
            String fieldName = ((FieldError) err).getField();
            String message = err.getDefaultMessage();
            response.put(fieldName, message);
        });

        log.warn("validation_error path={} errors={}", request.getRequestURI(), response);

        return new ResponseEntity<Map<String, String>>(response,
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity <Map<String, String>> myConstraintViolationException(
            ConstraintViolationException e,
            HttpServletRequest request
    ) {
        Map<String, String> response = new HashMap<>();
        e.getConstraintViolations().forEach(err -> {
            String value = String.valueOf(err.getInvalidValue());
            String message = err.getMessage();
            response.put(value, message);
        });

        log.warn("constraint_violation path={} errors={}", request.getRequestURI(), response);

        return new ResponseEntity<Map<String, String>>(response,
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<APIResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException e,
            HttpServletRequest request
    ) {
        String paramName = e.getName();
        Object rejected = e.getValue();
        String expectedType = e.getRequiredType() != null
                ? e.getRequiredType().getSimpleName()
                : "unknown";

        String message = "Invalid value '" + rejected + "' for parameter '"
                + paramName + "'. Expected type: " + expectedType;

        log.warn("type_mismatch path={} param={} value={} expected={}",
                request.getRequestURI(), paramName, rejected, expectedType);

        return new ResponseEntity<>(new APIResponse(message, false), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<APIResponse> handleUnreadableMessage(
            HttpMessageNotReadableException e,
            HttpServletRequest request
    ) {
        log.warn("malformed_request path={} message={}", request.getRequestURI(), e.getMostSpecificCause().getMessage());
        return new ResponseEntity<>(
                new APIResponse("Malformed JSON request", false),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<APIResponse> handleHandlerMethodValidationException(
            HandlerMethodValidationException e,
            HttpServletRequest request
    ) {
        String message = "Validation failure";
        // You could aggregate messages; keep it short for now
        log.warn("handler_method_validation path={} errors={}", request.getRequestURI(), e.getAllErrors());
        return new ResponseEntity<>(new APIResponse(message, false), HttpStatus.BAD_REQUEST);
    }


    /**
     * This method is used to handle API exceptions.
     * @param e The exception to handle.
     * @param request The HTTP request.
     * @return The API response.
     */
    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<APIResponse> handleAccessDenied(
            Exception e,
            HttpServletRequest request
    ) {
        log.warn("access_denied path={} user={} message={}",
                request.getRequestURI(),
                request.getRemoteUser(),
                e.getMessage());

        return new ResponseEntity<>(
                new APIResponse("You do not have permission to perform this action", false),
                HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<APIResponse> myAPIException(
            ApiException e,
            HttpServletRequest request
    ) {
        String message = e.getMessage();
        APIResponse apiResponse = new APIResponse(message, false);

        log.warn("api_exception path={} exception={} message={}",
                request.getRequestURI(),
                e.getClass().getSimpleName(),
                message);

        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<APIResponse> myResourceNotFoundException(
            ResourceNotFoundException e,
            HttpServletRequest request
    ) {
        String message = e.getMessage();
        APIResponse apiResponse = new APIResponse(message, false);

        log.warn("resource_not_found path={} exception={} message={}",
                request.getRequestURI(),
                e.getClass().getSimpleName(),
                message);

        return new ResponseEntity<>(apiResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<APIResponse> handleResourceConflict(
            ResourceConflictException e,
            HttpServletRequest request
    ) {
        log.warn("resource_conflict path={} message={}", request.getRequestURI(), e.getMessage());
        return new ResponseEntity<>(new APIResponse(e.getMessage(), false), HttpStatus.CONFLICT);
    }

    /**
     * Safety-net for any foreign-key or unique-constraint violations that
     * slip past service-layer checks. Returns {@code 409 Conflict} instead
     * of a raw {@code 500 Internal Server Error}.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<APIResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException e,
            HttpServletRequest request
    ) {
        log.error("data_integrity_violation path={} message={}", request.getRequestURI(),
                e.getMostSpecificCause().getMessage());
        return new ResponseEntity<>(
                new APIResponse("Operation conflicts with existing data. Check related records and try again.", false),
                HttpStatus.CONFLICT);
    }

}
