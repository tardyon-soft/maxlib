package ru.tardyon.botframework.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.model.Update;
import ru.tardyon.botframework.model.UpdateId;
import ru.tardyon.botframework.model.UpdateType;

class UpdateSinkTest {

    @Test
    void syncAdapterReturnsSuccessForHandledUpdate() {
        AtomicReference<Update> captured = new AtomicReference<>();
        UpdateSink sink = UpdateSink.sync(captured::set);
        Update update = sampleUpdate();

        UpdateHandlingResult result = sink.handle(update).toCompletableFuture().join();

        assertTrue(result.isSuccess());
        assertSame(update, captured.get());
    }

    @Test
    void syncAdapterMapsRuntimeErrorToFailureResult() {
        RuntimeException expected = new RuntimeException("failed");
        UpdateSink sink = UpdateSink.sync(update -> {
            throw expected;
        });

        UpdateHandlingResult result = sink.handle(sampleUpdate()).toCompletableFuture().join();

        assertFalse(result.isSuccess());
        assertEquals(UpdateHandlingStatus.FAILURE, result.status());
        assertSame(expected, result.error().orElseThrow());
    }

    @Test
    void syncAdapterRequiresConsumer() {
        assertThrows(NullPointerException.class, () -> UpdateSink.sync(null));
    }

    @Test
    void syncAdapterRequiresUpdate() {
        UpdateSink sink = UpdateSink.sync(update -> {
        });
        UpdateHandlingResult result = sink.handle(null).toCompletableFuture().join();
        assertFalse(result.isSuccess());
        assertEquals(UpdateHandlingStatus.FAILURE, result.status());
        assertTrue(result.error().orElseThrow() instanceof NullPointerException);
    }

    private static Update sampleUpdate() {
        return new Update(
                new UpdateId("upd-1"),
                UpdateType.MESSAGE,
                null,
                null,
                null,
                Instant.parse("2026-01-01T00:00:00Z")
        );
    }
}
