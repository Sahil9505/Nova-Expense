package com.nova.ai;

import com.nova.common.exception.ErrorCode;
import com.nova.common.exception.NovaException;

/**
 * Thrown by the AI layer when the underlying model cannot fulfil a request
 * (unavailable, timeout, rate limited, invalid response, network failure). Mapped
 * to the standard {@code ApiResponse} envelope by {@code GlobalExceptionHandler},
 * so the client always receives a friendly, structured error rather than a stack
 * trace. The exception never leaks model internals or prompts.
 */
public class AiCopilotException extends NovaException {

    public AiCopilotException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AiCopilotException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public AiCopilotException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
