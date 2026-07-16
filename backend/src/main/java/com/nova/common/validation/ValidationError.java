package com.nova.common.validation;

/**
 * A single field-level validation failure.
 *
 * @param field   the property path that failed validation
 * @param message a human-readable reason
 */
public record ValidationError(String field, String message) {
}
