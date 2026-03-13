package ru.max.botframework.upload;

/**
 * Runtime options for resumable transfer flow.
 */
public record ResumableUploadOptions(int chunkSizeBytes, int maxRetriesPerChunk) {
    private static final int DEFAULT_CHUNK_SIZE = 256 * 1024;

    public ResumableUploadOptions {
        if (chunkSizeBytes <= 0) {
            throw new IllegalArgumentException("chunkSizeBytes must be positive");
        }
        if (maxRetriesPerChunk < 0) {
            throw new IllegalArgumentException("maxRetriesPerChunk must be >= 0");
        }
    }

    public static ResumableUploadOptions defaults() {
        return new ResumableUploadOptions(DEFAULT_CHUNK_SIZE, 1);
    }
}
