package com.nova.common.exception;

import com.nova.common.api.ApiResponse;
import com.nova.common.validation.ValidationError;
import com.nova.finance.receipt.ReceiptProcessingException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates exceptions into the standard {@link ApiResponse} envelope.
 *
 * <p>All error responses keep {@code success=false} and embed an {@link ApiError}
 * payload in {@code data} so clients can rely on a single shape.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleValidation(MethodArgumentNotValidException ex) {
        List<ValidationError> errors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.add(new ValidationError(fieldError.getField(), fieldError.getDefaultMessage()));
        }
        ApiError body = ApiError.of(ErrorCode.VALIDATION_ERROR, "Validation failed.", toFieldErrors(errors));
        return build(ErrorCode.VALIDATION_ERROR.getStatus(), ApiResponse.error("Validation failed.", body));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleHandlerValidation(HandlerMethodValidationException ex) {
        ApiError body = ApiError.of(ErrorCode.VALIDATION_ERROR, "Validation failed.");
        return build(ErrorCode.VALIDATION_ERROR.getStatus(), ApiResponse.error("Validation failed.", body));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleConstraint(ConstraintViolationException ex) {
        List<ApiError.FieldError> errors = ex.getConstraintViolations().stream()
                .map(v -> new ApiError.FieldError(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        ApiError body = ApiError.of(ErrorCode.VALIDATION_ERROR, "Validation failed.", errors);
        return build(ErrorCode.VALIDATION_ERROR.getStatus(), ApiResponse.error("Validation failed.", body));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleNotFound(NovaException ex) {
        ApiError body = ApiError.of(ex.getErrorCode(), ex.getMessage());
        return build(ex.getErrorCode().getStatus(), ApiResponse.error(ex.getMessage(), body));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleConflict(NovaException ex) {
        ApiError body = ApiError.of(ex.getErrorCode(), ex.getMessage());
        return build(ex.getErrorCode().getStatus(), ApiResponse.error(ex.getMessage(), body));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleBadRequest(NovaException ex) {
        ApiError body = ApiError.of(ex.getErrorCode(), ex.getMessage());
        return build(ex.getErrorCode().getStatus(), ApiResponse.error(ex.getMessage(), body));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleInvalidCredentials(NovaException ex) {
        ApiError body = ApiError.of(ex.getErrorCode(), ex.getMessage());
        return build(ex.getErrorCode().getStatus(), ApiResponse.error(ex.getMessage(), body));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleInvalidToken(NovaException ex) {
        ApiError body = ApiError.of(ex.getErrorCode(), ex.getMessage());
        return build(ex.getErrorCode().getStatus(), ApiResponse.error(ex.getMessage(), body));
    }

    @ExceptionHandler(ReceiptProcessingException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleReceipt(NovaException ex) {
        ApiError body = ApiError.of(ex.getErrorCode(), ex.getMessage());
        return build(ex.getErrorCode().getStatus(), ApiResponse.error(ex.getMessage(), body));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleAccessDenied(AccessDeniedException ex) {
        ApiError body = ApiError.of(ErrorCode.FORBIDDEN, ex.getMessage());
        return build(HttpStatus.FORBIDDEN, ApiResponse.error("Access denied.", body));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleNotReadable(HttpMessageNotReadableException ex) {
        ApiError body = ApiError.of(ErrorCode.BAD_REQUEST, "Request body could not be read.");
        return build(HttpStatus.BAD_REQUEST, ApiResponse.error("Malformed request body.", body));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ApiError>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        ApiError body = ApiError.of(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage());
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ApiResponse.error(ErrorCode.INTERNAL_ERROR.getDefaultMessage(), body));
    }

    private List<ApiError.FieldError> toFieldErrors(List<ValidationError> source) {
        return source.stream()
                .map(e -> new ApiError.FieldError(e.field(), e.message()))
                .toList();
    }

    private <T> ResponseEntity<ApiResponse<T>> build(HttpStatus status, ApiResponse<T> body) {
        return ResponseEntity.status(status).body(body);
    }
}
