package ru.max.botframework.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ResumableUploadTransferGatewayTest {

    @Test
    void uploadsInputInChunksSuccessfully() {
        List<ResumableChunkUploadRequest> requests = new ArrayList<>();

        ResumableUploadTransferGateway gateway = new ResumableUploadTransferGateway(
                request -> {
                    requests.add(request);
                    long committed = request.chunkOffset() + request.chunk().length;
                    return CompletableFuture.completedFuture(new ResumableChunkUploadResponse(200, committed));
                },
                new ResumableUploadOptions(4, 0)
        );

        UploadTransferReceipt receipt = gateway.transfer(
                        new UploadPreparation(URI.create("https://upload.example.com/r-1"), UploadFlowType.RESUMABLE, "r-1"),
                        InputFile.fromBytes("abcdefghij".getBytes(StandardCharsets.UTF_8), "data.bin")
                                .withContentType("application/custom")
                )
                .toCompletableFuture()
                .join();

        assertEquals(10L, receipt.bytesTransferred());
        assertEquals(3, requests.size());

        assertEquals(0L, requests.get(0).chunkOffset());
        assertEquals(4, requests.get(0).chunk().length);
        assertEquals(1, requests.get(0).attempt());
        assertTrue(!requests.get(0).lastChunk());

        assertEquals(4L, requests.get(1).chunkOffset());
        assertEquals(4, requests.get(1).chunk().length);

        assertEquals(8L, requests.get(2).chunkOffset());
        assertEquals(2, requests.get(2).chunk().length);
        assertTrue(requests.get(2).lastChunk());
        assertEquals("application/custom", requests.get(2).contentType());
    }

    @Test
    void retriesFailedChunkAndContinues() {
        Map<Long, AtomicInteger> attemptsPerOffset = new ConcurrentHashMap<>();

        ResumableUploadTransferGateway gateway = new ResumableUploadTransferGateway(
                request -> {
                    int attempt = attemptsPerOffset
                            .computeIfAbsent(request.chunkOffset(), key -> new AtomicInteger())
                            .incrementAndGet();

                    if (request.chunkOffset() == 4L && attempt == 1) {
                        return CompletableFuture.completedFuture(new ResumableChunkUploadResponse(503, null));
                    }

                    long committed = request.chunkOffset() + request.chunk().length;
                    return CompletableFuture.completedFuture(new ResumableChunkUploadResponse(200, committed));
                },
                new ResumableUploadOptions(4, 1)
        );

        UploadTransferReceipt receipt = gateway.transfer(
                        new UploadPreparation(URI.create("https://upload.example.com/r-2"), UploadFlowType.RESUMABLE, "r-2"),
                        InputFile.fromBytes("abcdefghij".getBytes(StandardCharsets.UTF_8), "data.bin")
                )
                .toCompletableFuture()
                .join();

        assertEquals(10L, receipt.bytesTransferred());
        assertEquals(2, attemptsPerOffset.get(4L).get());
    }

    @Test
    void failsWhenChunkStillFailsAfterRetries() {
        ResumableUploadTransferGateway gateway = new ResumableUploadTransferGateway(
                request -> CompletableFuture.completedFuture(new ResumableChunkUploadResponse(503, null)),
                new ResumableUploadOptions(4, 1)
        );

        CompletionException exception = org.junit.jupiter.api.Assertions.assertThrows(
                CompletionException.class,
                () -> gateway.transfer(
                        new UploadPreparation(URI.create("https://upload.example.com/r-3"), UploadFlowType.RESUMABLE, "r-3"),
                        InputFile.fromBytes("abcdefgh".getBytes(StandardCharsets.UTF_8), "data.bin")
                ).toCompletableFuture().join()
        );

        UploadTransferException transferException = assertInstanceOf(UploadTransferException.class, exception.getCause());
        assertTrue(transferException.getMessage().contains("status 503"));
    }

    @Test
    void failsOnCommittedOffsetRegression() {
        ResumableUploadTransferGateway gateway = new ResumableUploadTransferGateway(
                request -> CompletableFuture.completedFuture(new ResumableChunkUploadResponse(200, request.chunkOffset())),
                new ResumableUploadOptions(4, 0)
        );

        CompletionException exception = org.junit.jupiter.api.Assertions.assertThrows(
                CompletionException.class,
                () -> gateway.transfer(
                        new UploadPreparation(URI.create("https://upload.example.com/r-4"), UploadFlowType.RESUMABLE, "r-4"),
                        InputFile.fromBytes("abcdef".getBytes(StandardCharsets.UTF_8), "data.bin")
                ).toCompletableFuture().join()
        );

        UploadTransferException transferException = assertInstanceOf(UploadTransferException.class, exception.getCause());
        assertTrue(transferException.getMessage().contains("offset regression"));
    }

    @Test
    void integratesWithDefaultUploadServiceForResumableFlow() {
        UploadService service = new DefaultUploadService(
                command -> CompletableFuture.completedFuture(
                        new UploadPreparation(URI.create("https://upload.example.com/r-5"), UploadFlowType.RESUMABLE, "r-5")
                ),
                new ResumableUploadTransferGateway(
                        request -> CompletableFuture.completedFuture(
                                new ResumableChunkUploadResponse(200, request.chunkOffset() + request.chunk().length)
                        ),
                        new ResumableUploadOptions(3, 0)
                ),
                (preparation, receipt) -> CompletableFuture.completedFuture(
                        new UploadFinalizeResult("ref-r-5", receipt.bytesTransferred(), "video/mp4")
                ),
                new DefaultUploadResultMapper()
        );

        UploadResult result = service.upload(
                InputFile.fromBytes("abcdefghij".getBytes(StandardCharsets.UTF_8), "clip.mp4")
                        .withContentType("video/mp4")
        ).toCompletableFuture().join();

        assertEquals("ref-r-5", result.ref().value());
        assertEquals(10L, result.bytesTransferred());
        assertEquals(UploadFlowType.RESUMABLE, result.flowType());
    }
}
