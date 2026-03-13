package ru.max.botframework.upload;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

/**
 * Multipart transfer implementation for prepared upload URLs.
 */
public final class MultipartUploadTransferGateway implements UploadTransferGateway {
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final MultipartUploadHttpClient httpClient;

    public MultipartUploadTransferGateway(MultipartUploadHttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    @Override
    public CompletionStage<UploadTransferReceipt> transfer(UploadPreparation preparation, InputFile inputFile) {
        Objects.requireNonNull(preparation, "preparation");
        Objects.requireNonNull(inputFile, "inputFile");

        if (preparation.flowType() != UploadFlowType.MULTIPART) {
            return CompletableFuture.failedFuture(
                    new UploadTransferException("multipart transfer gateway requires MULTIPART preparation flow")
            );
        }

        final byte[] payload;
        try (InputStream stream = inputFile.openStream()) {
            payload = stream.readAllBytes();
        } catch (IOException ioException) {
            return CompletableFuture.failedFuture(new UploadTransferException("failed to read input file for upload", ioException));
        }

        MultipartUploadRequest request = new MultipartUploadRequest(
                preparation.uploadUrl(),
                inputFile.fileName(),
                inputFile.contentType().orElse(DEFAULT_CONTENT_TYPE),
                payload
        );

        CompletableFuture<UploadTransferReceipt> result = new CompletableFuture<>();
        httpClient.upload(request).whenComplete((response, throwable) -> {
            if (throwable != null) {
                result.completeExceptionally(new UploadTransferException("multipart upload request failed", unwrapCompletion(throwable)));
                return;
            }

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                result.complete(new UploadTransferReceipt(payload.length, null));
                return;
            }

            result.completeExceptionally(new UploadTransferException(
                    "multipart upload failed with status " + response.statusCode()
                            + (response.body() == null || response.body().isBlank() ? "" : ": " + response.body())
            ));
        });

        return result;
    }

    private static Throwable unwrapCompletion(Throwable throwable) {
        if (throwable instanceof CompletionException completionException && completionException.getCause() != null) {
            return completionException.getCause();
        }
        return throwable;
    }
}
