package com.techmart.exception;

/**
 * Base exception for all TechMart business logic exceptions.
 * Extends RuntimeException to avoid forcing catch blocks in EJBs
 * (which would interfere with JTA transaction rollback semantics).
 */
public class TechMartException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;

    public TechMartException(String message) {
        super(message);
        this.errorCode = "TECHMART_ERROR";
    }

    public TechMartException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public TechMartException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public TechMartException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "TECHMART_ERROR";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
