package com.hfenelsoftllc.foodcatalogue.exception;

public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

