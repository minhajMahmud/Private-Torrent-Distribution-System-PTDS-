package com.ptds.exception;

/** Thrown when reading from or writing to the file storage backend fails. */
public class StorageException extends RuntimeException {
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
    public StorageException(String message) {
        super(message);
    }
}
