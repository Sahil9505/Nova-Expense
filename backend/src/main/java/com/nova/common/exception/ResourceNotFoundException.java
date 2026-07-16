package com.nova.common.exception;

/**
 * Thrown when an entity referenced by a client does not exist.
 */
public class ResourceNotFoundException extends NovaException {

    public ResourceNotFoundException(String resource, String identifier) {
        super(ErrorCode.RESOURCE_NOT_FOUND, resource + " not found: " + identifier);
    }

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }
}
