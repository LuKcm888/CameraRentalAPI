package com.camerarental.backend.exceptions;

public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final String resourceName;
    private final String field;
    private final Object fieldValue;

    public ResourceNotFoundException(String resourceName, String field, Object fieldValue) {
        super(String.format("%s not found with %s: %s", resourceName, field, fieldValue));
        this.resourceName = resourceName;
        this.field = field;
        this.fieldValue = fieldValue;
    }

    public String getResourceName() { return resourceName; }
    public String getField() { return field; }
    public Object getFieldValue() { return fieldValue; }
}
