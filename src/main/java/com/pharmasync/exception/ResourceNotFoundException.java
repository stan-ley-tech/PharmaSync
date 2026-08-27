package com.pharmasync.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String entityType, Object id) {
        return new ResourceNotFoundException(entityType + " not found: " + id);
    }
}
