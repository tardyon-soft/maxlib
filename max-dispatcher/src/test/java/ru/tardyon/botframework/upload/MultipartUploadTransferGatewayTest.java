package ru.tardyon.botframework.upload;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class MultipartUploadTransferGatewayTest {

    @Test
    void transfersMultipartPayloadSuccessfully() {
        AtomicReference<MultipartUploadRequest> captured = new AtomicReference<>();
        MultipartUploadTransferGateway gateway = new MultipartUploadTransferGateway(request -> {
            captured.set(request);
            return CompletableFuture.completedFuture(new MultipartUploadResponse(201, "ok"));
        });

        UploadTransferReceipt receipt = gateway.transfer(
                        new UploadPreparation(URI.create("https://upload.example.com/u-1"), UploadFlowType.MULTIPART, "u-1"),
                        bytesInput("hello".getBytes(StandardCharsets.UTF_8), "hello.txt")
                                .withContentType("text/plain")
                )
                .toCompletableFuture()
                .join();

        assertEquals(5L, receipt.bytesTransferred());
        assertEquals("hello.txt", captured.get().fileName());
        assertEquals("text/plain", captured.get().contentType());
        assertEquals(URI.create("https://upload.example.com/u-1"), captured.get().uploadUrl());
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), captured.get().content());
    }

    @Test
    void defaultsContentTypeForMultipartPayload() {
        AtomicReference<MultipartUploadRequest> captured = new AtomicReference<>();
        MultipartUploadTransferGateway gateway = new MultipartUploadTransferGateway(request -> {
            captured.set(request);
            return CompletableFuture.completedFuture(new MultipartUploadResponse(204, ""));
        });

        gateway.transfer(
                        new UploadPreparation(URI.create("https://upload.example.com/u-2"), UploadFlowType.MULTIPART, "u-2"),
                        bytesInput(new byte[]{1, 2, 3}, "file.bin")
                )
                .toCompletableFuture()
                .join();

        assertEquals("application/octet-stream", captured.get().contentType());
    }

    @Test
    void failsOnNonMultipartPreparation() {
        MultipartUploadTransferGateway gateway = new MultipartUploadTransferGateway(
                request -> CompletableFuture.completedFuture(new MultipartUploadResponse(200, "ok"))
        );

        CompletionException exception = org.junit.jupiter.api.Assertions.assertThrows(
                CompletionException.class,
                () -> gateway.transfer(
                        new UploadPreparation(URI.create("https://upload.example.com/u-3"), UploadFlowType.RESUMABLE, "u-3"),
                        bytesInput(new byte[]{1}, "a.bin")
                ).toCompletableFuture().join()
        );

        UploadTransferException transferException = assertInstanceOf(UploadTransferException.class, exception.getCause());
        assertTrue(transferException.getMessage().contains("MULTIPART"));
    }

    @Test
    void mapsUploadFailureStatusToTransferException() {
        MultipartUploadTransferGateway gateway = new MultipartUploadTransferGateway(
                request -> CompletableFuture.completedFuture(new MultipartUploadResponse(500, "upstream error"))
        );

        CompletionException exception = org.junit.jupiter.api.Assertions.assertThrows(
                CompletionException.class,
                () -> gateway.transfer(
                        new UploadPreparation(URI.create("https://upload.example.com/u-4"), UploadFlowType.MULTIPART, "u-4"),
                        bytesInput(new byte[]{1}, "a.bin")
                ).toCompletableFuture().join()
        );

        UploadTransferException transferException = assertInstanceOf(UploadTransferException.class, exception.getCause());
        assertTrue(transferException.getMessage().contains("status 500"));
    }

    @Test
    void wrapsHttpClientExecutionFailure() {
        MultipartUploadTransferGateway gateway = new MultipartUploadTransferGateway(
                request -> CompletableFuture.failedFuture(new IllegalStateException("network down"))
        );

        CompletionException exception = org.junit.jupiter.api.Assertions.assertThrows(
                CompletionException.class,
                () -> gateway.transfer(
                        new UploadPreparation(URI.create("https://upload.example.com/u-5"), UploadFlowType.MULTIPART, "u-5"),
                        bytesInput(new byte[]{1}, "a.bin")
                ).toCompletableFuture().join()
        );

        UploadTransferException transferException = assertInstanceOf(UploadTransferException.class, exception.getCause());
        assertEquals("multipart upload request failed", transferException.getMessage());
        assertInstanceOf(IllegalStateException.class, transferException.getCause());
    }

    @Test
    void wrapsInputFileReadFailure() {
        MultipartUploadTransferGateway gateway = new MultipartUploadTransferGateway(
                request -> CompletableFuture.completedFuture(new MultipartUploadResponse(200, "ok"))
        );

        CompletionException exception = org.junit.jupiter.api.Assertions.assertThrows(
                CompletionException.class,
                () -> gateway.transfer(
                        new UploadPreparation(URI.create("https://upload.example.com/u-6"), UploadFlowType.MULTIPART, "u-6"),
                        new InputFile.StreamInputFile(() -> {
                            throw new IOException("cannot read stream");
                        }, "broken.bin", Optional.empty(), null)
                ).toCompletableFuture().join()
        );

        UploadTransferException transferException = assertInstanceOf(UploadTransferException.class, exception.getCause());
        assertEquals("failed to read input file for upload", transferException.getMessage());
        assertInstanceOf(IOException.class, transferException.getCause());
    }

    @Test
    void integratesWithDefaultUploadServiceForMultipartFlow() {
        UploadService service = new DefaultUploadService(
                command -> CompletableFuture.completedFuture(
                        new UploadPreparation(URI.create("https://upload.example.com/u-7"), UploadFlowType.MULTIPART, "u-7")
                ),
                new MultipartUploadTransferGateway(request -> CompletableFuture.completedFuture(new MultipartUploadResponse(201, "ok"))),
                (preparation, receipt) -> CompletableFuture.completedFuture(
                        new UploadFinalizeResult("ref-7", receipt.bytesTransferred(), "image/png")
                ),
                new DefaultUploadResultMapper()
        );

        UploadResult result = service.upload(
                bytesInput(new byte[]{1, 2, 3, 4}, "image.png").withContentType("image/png")
        ).toCompletableFuture().join();

        assertEquals("ref-7", result.ref().value());
        assertEquals(4L, result.bytesTransferred());
        assertEquals(UploadFlowType.MULTIPART, result.flowType());
        assertEquals("image/png", result.contentTypeOptional().orElseThrow());
    }

    private static InputFile bytesInput(byte[] bytes, String fileName) {
        return new InputFile.BytesInputFile(bytes, fileName, Optional.empty());
    }
}
