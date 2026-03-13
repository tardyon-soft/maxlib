package ru.tardyon.botframework.upload;

import java.util.concurrent.CompletionStage;

/**
 * Raw execution boundary for resumable chunk upload requests.
 */
@FunctionalInterface
public interface ResumableChunkUploadClient {
    CompletionStage<ResumableChunkUploadResponse> uploadChunk(ResumableChunkUploadRequest request);
}
