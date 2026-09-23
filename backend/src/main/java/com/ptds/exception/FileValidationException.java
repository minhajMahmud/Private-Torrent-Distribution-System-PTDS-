package com.ptds.exception;

/** Thrown when an uploaded file fails type, size, or content validation. */
public class FileValidationException extends RuntimeException {
    public FileValidationException(String message) {
        super(message);
    }
}
