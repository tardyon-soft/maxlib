package ru.tardyon.botframework.upload;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

/**
 * Resumable transfer implementation based on chunked upload requests.
 */
public final class ResumableUploadTransferGateway implements UploadTransferGateway {
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final ResumableChunkUploadClient chunkUploadClient;
    private final ResumableUploadOptions options;

    public ResumableUploadTransferGateway(ResumableChunkUploadClient chunkUploadClient) {
        this(chunkUploadClient, ResumableUploadOptions.defaults());
    }

    public ResumableUploadTransferGateway(ResumableChunkUploadClient chunkUploadClient, ResumableUploadOptions options) {
        this.chunkUploadClient = Objects.requireNonNull(chunkUploadClient, "chunkUploadClient");
        this.options = Objects.requireNonNull(options, "options");
    }

    @Override
    public CompletionStage<UploadTransferReceipt> transfer(UploadPreparation preparation, InputFile inputFile) {
        Objects.requireNonNull(preparation, "preparation");
        Objects.requireNonNull(inputFile, "inputFile");

        if (preparation.flowType() != UploadFlowType.RESUMABLE) {
            return CompletableFuture.failedFuture(
                    new UploadTransferException("resumable transfer gateway requires RESUMABLE preparation flow")
            );
        }

        final InputStream stream;
        try {
            stream = inputFile.openStream();
        } catch (IOException ioException) {
            return CompletableFuture.failedFuture(new UploadTransferException("failed to open input stream for resumable upload", ioException));
        }

        Long totalBytes = inputFile.knownSize().isPresent() ? inputFile.knownSize().getAsLong() : null;
        String contentType = inputFile.contentType().orElse(DEFAULT_CONTENT_TYPE);

        CompletableFuture<UploadTransferReceipt> result = new CompletableFuture<>();
        transferNextChunk(
                preparation,
                inputFile.fileName(),
                contentType,
                totalBytes,
                stream,
                0L,
                result
        );

        result.whenComplete((ignored, throwable) -> closeQuietly(stream));
        return result;
    }

    private void transferNextChunk(
            UploadPreparation preparation,
            String fileName,
            String contentType,
            Long totalBytes,
            InputStream stream,
            long offset,
            CompletableFuture<UploadTransferReceipt> result
    ) {
        if (result.isDone()) {
            return;
        }

        Chunk chunk;
        try {
            chunk = readChunk(stream, options.chunkSizeBytes(), totalBytes, offset);
        } catch (IOException ioException) {
            result.completeExceptionally(new UploadTransferException("failed to read chunk for resumable upload", ioException));
            return;
        }

        if (chunk == null) {
            result.complete(new UploadTransferReceipt(offset, null));
            return;
        }

        uploadChunkWithRetry(preparation, fileName, contentType, totalBytes, offset, chunk, 1)
                .whenComplete((nextOffset, throwable) -> {
                    if (throwable != null) {
                        result.completeExceptionally(unwrapUploadError(throwable));
                        return;
                    }
                    transferNextChunk(preparation, fileName, contentType, totalBytes, stream, nextOffset, result);
                });
    }

    private CompletionStage<Long> uploadChunkWithRetry(
            UploadPreparation preparation,
            String fileName,
            String contentType,
            Long totalBytes,
            long offset,
            Chunk chunk,
            int attempt
    ) {
        ResumableChunkUploadRequest request = new ResumableChunkUploadRequest(
                preparation.uploadUrl(),
                preparation.uploadId(),
                fileName,
                contentType,
                offset,
                totalBytes,
                chunk.lastChunk,
                attempt,
                chunk.bytes
        );

        CompletableFuture<Long> nextOffsetFuture = new CompletableFuture<>();
        chunkUploadClient.uploadChunk(request).whenComplete((response, throwable) -> {
            if (throwable != null) {
                if (attempt <= options.maxRetriesPerChunk()) {
                    uploadChunkWithRetry(preparation, fileName, contentType, totalBytes, offset, chunk, attempt + 1)
                            .whenComplete((nextOffset, retryThrowable) -> completeFromRetry(nextOffsetFuture, nextOffset, retryThrowable));
                    return;
                }
                nextOffsetFuture.completeExceptionally(new UploadTransferException(
                        "resumable chunk upload failed at offset " + offset + " (attempt " + attempt + ")",
                        unwrapCompletion(throwable)
                ));
                return;
            }

            if (isSuccessfulStatus(response.statusCode())) {
                long expectedMinimum = offset + chunk.bytes.length;
                long committed = response.committedBytes() == null ? expectedMinimum : response.committedBytes();
                if (committed < expectedMinimum) {
                    nextOffsetFuture.completeExceptionally(new UploadTransferException(
                            "resumable upload offset regression: expected at least " + expectedMinimum + " but got " + committed
                    ));
                    return;
                }
                nextOffsetFuture.complete(committed);
                return;
            }

            if (isRetryableStatus(response.statusCode()) && attempt <= options.maxRetriesPerChunk()) {
                uploadChunkWithRetry(preparation, fileName, contentType, totalBytes, offset, chunk, attempt + 1)
                        .whenComplete((nextOffset, retryThrowable) -> completeFromRetry(nextOffsetFuture, nextOffset, retryThrowable));
                return;
            }

            nextOffsetFuture.completeExceptionally(new UploadTransferException(
                    "resumable chunk upload failed with status " + response.statusCode() + " at offset " + offset
            ));
        });

        return nextOffsetFuture;
    }

    private static void completeFromRetry(CompletableFuture<Long> target, Long value, Throwable throwable) {
        if (throwable != null) {
            target.completeExceptionally(throwable);
            return;
        }
        target.complete(value);
    }

    private static Chunk readChunk(InputStream stream, int chunkSize, Long totalBytes, long offset) throws IOException {
        byte[] buffer = new byte[chunkSize];
        int read;
        int totalRead = 0;
        while ((read = stream.read(buffer, totalRead, chunkSize - totalRead)) != -1) {
            totalRead += read;
            if (totalRead == chunkSize) {
                break;
            }
        }

        if (totalRead == 0) {
            return null;
        }

        byte[] bytes = Arrays.copyOf(buffer, totalRead);
        boolean lastChunk = totalRead < chunkSize;
        if (!lastChunk && totalBytes != null) {
            lastChunk = offset + totalRead >= totalBytes;
        }

        return new Chunk(bytes, lastChunk);
    }

    private static boolean isSuccessfulStatus(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    private static boolean isRetryableStatus(int statusCode) {
        return statusCode == 429 || statusCode >= 500;
    }

    private static RuntimeException unwrapUploadError(Throwable throwable) {
        Throwable unwrapped = unwrapCompletion(throwable);
        if (unwrapped instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new UploadTransferException("resumable upload failed", unwrapped);
    }

    private static Throwable unwrapCompletion(Throwable throwable) {
        if (throwable instanceof CompletionException completionException && completionException.getCause() != null) {
            return completionException.getCause();
        }
        return throwable;
    }

    private static void closeQuietly(InputStream stream) {
        try {
            stream.close();
        } catch (IOException ignored) {
            // ignore close failure
        }
    }

    private record Chunk(byte[] bytes, boolean lastChunk) {
    }
}
