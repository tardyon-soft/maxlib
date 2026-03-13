package ru.max.botframework.upload;

/**
 * Raw multipart upload HTTP response.
 */
public record MultipartUploadResponse(int statusCode, String body) {
}
