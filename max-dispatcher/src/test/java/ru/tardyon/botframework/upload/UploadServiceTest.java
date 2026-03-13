package ru.tardyon.botframework.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class UploadServiceTest {

    @Test
    void orchestratesPrepareTransferFinalizeAndMapsResult() {
        AtomicReference<UploadPrepareCommand> capturedCommand = new AtomicReference<>();
        InputFile input = InputFile.fromBytes("hello".getBytes(StandardCharsets.UTF_8), "hello.txt")
                .withContentType("text/plain");

        UploadService service = new DefaultUploadService(
                command -> {
                    capturedCommand.set(command);
                    return CompletableFuture.completedFuture(
                            new UploadPreparation(URI.create("https://upload.example.com/u-1"), UploadFlowType.MULTIPART, "u-1")
                    );
                },
                (preparation, file) -> CompletableFuture.completedFuture(new UploadTransferReceipt(5L, "md5-1")),
                (preparation, receipt) -> CompletableFuture.completedFuture(new UploadFinalizeResult("ref-1", 5L, "text/plain")),
                new DefaultUploadResultMapper()
        );

        UploadResult result = service.upload(
                input,
                UploadRequest.defaults()
                        .withPreferredFlow(UploadFlowType.MULTIPART)
                        .withMediaTypeHint("document")
        ).toCompletableFuture().join();

        assertEquals("hello.txt", capturedCommand.get().fileName());
        assertEquals("text/plain", capturedCommand.get().contentType());
        assertEquals(5L, capturedCommand.get().size());
        assertEquals(UploadFlowType.MULTIPART, capturedCommand.get().preferredFlowType());
        assertEquals("document", capturedCommand.get().mediaTypeHint());

        assertEquals("ref-1", result.ref().value());
        assertEquals(UploadFlowType.MULTIPART, result.flowType());
        assertEquals(5L, result.bytesTransferred());
        assertEquals("text/plain", result.contentTypeOptional().orElseThrow());
    }

    @Test
    void uploadServiceFactoryProvidesWorkingDefaultWiring() {
        UploadService service = UploadService.of(
                command -> CompletableFuture.completedFuture(
                        new UploadPreparation(URI.create("https://upload.example.com/u-2"), UploadFlowType.RESUMABLE, "u-2")
                ),
                (preparation, file) -> CompletableFuture.completedFuture(new UploadTransferReceipt(3L, null)),
                (preparation, receipt) -> CompletableFuture.completedFuture(new UploadFinalizeResult("ref-2", 3L, "application/octet-stream"))
        );

        UploadResult result = service.upload(InputFile.fromBytes(new byte[]{1, 2, 3}, "bin.dat"))
                .toCompletableFuture()
                .join();

        assertEquals("ref-2", result.ref().value());
        assertEquals(UploadFlowType.RESUMABLE, result.flowType());
        assertEquals(3L, result.bytesTransferred());
    }

    @Test
    void stopsPipelineWhenPrepareFails() {
        AtomicBoolean transferCalled = new AtomicBoolean(false);
        AtomicBoolean finalizeCalled = new AtomicBoolean(false);

        UploadService service = UploadService.of(
                command -> CompletableFuture.failedFuture(new IllegalStateException("prepare failed")),
                (preparation, file) -> {
                    transferCalled.set(true);
                    return CompletableFuture.completedFuture(new UploadTransferReceipt(1L, null));
                },
                (preparation, receipt) -> {
                    finalizeCalled.set(true);
                    return CompletableFuture.completedFuture(new UploadFinalizeResult("ref", 1L, null));
                }
        );

        CompletionException exception = assertThrows(
                CompletionException.class,
                () -> service.upload(InputFile.fromBytes(new byte[]{1}, "a.bin")).toCompletableFuture().join()
        );

        assertTrue(exception.getCause() instanceof IllegalStateException);
        assertEquals("prepare failed", exception.getCause().getMessage());
        assertTrue(!transferCalled.get());
        assertTrue(!finalizeCalled.get());
    }
}
