package com.pharmasync.exception;

public class SupplierIntegrationException extends RuntimeException {

    public SupplierIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SupplierIntegrationException(String message) {
        super(message);
    }
}
