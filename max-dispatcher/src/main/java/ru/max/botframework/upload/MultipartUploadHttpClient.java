package ru.max.botframework.upload;

import java.util.concurrent.CompletionStage;

/**
 * Raw HTTP client for multipart upload step.
 */
@FunctionalInterface
public interface MultipartUploadHttpClient {
    CompletionStage<MultipartUploadResponse> upload(MultipartUploadRequest request);
}
