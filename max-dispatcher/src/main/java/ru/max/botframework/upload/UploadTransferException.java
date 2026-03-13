package ru.max.botframework.upload;

/**
 * Upload transfer stage failure.
 */
public final class UploadTransferException extends RuntimeException {
    public UploadTransferException(String message) {
        super(message);
    }

    public UploadTransferException(String message, Throwable cause) {
        super(message, cause);
    }
}
