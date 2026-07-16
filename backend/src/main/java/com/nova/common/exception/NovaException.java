package com.nova.common.exception;

/**
 * Base runtime exception for Nova domain errors. Carries a stable {@link ErrorCode}.
 */
public class NovaException extends RuntimeException {

    private final ErrorCode errorCode;

    public NovaException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public NovaException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public NovaException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
