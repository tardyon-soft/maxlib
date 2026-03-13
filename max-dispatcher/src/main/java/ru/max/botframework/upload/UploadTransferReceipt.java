package ru.max.botframework.upload;

/**
 * Raw upload transfer execution result.
 */
public record UploadTransferReceipt(long bytesTransferred, String checksum) {
    public UploadTransferReceipt {
        if (bytesTransferred < 0) {
            throw new IllegalArgumentException("bytesTransferred must be non-negative");
        }
        if (checksum != null && checksum.isBlank()) {
            throw new IllegalArgumentException("checksum must not be blank");
        }
    }
}
