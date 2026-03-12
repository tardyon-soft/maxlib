package ru.max.botframework.ingestion;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import ru.max.botframework.model.Update;

/**
 * Backward-compatible alias for {@link UpdateConsumer}.
 *
 * <p>Prefer {@link UpdateConsumer} in new code.</p>
 */
@Deprecated(forRemoval = false)
@FunctionalInterface
public interface UpdateSink extends UpdateConsumer {

    static UpdateSink sync(Consumer<Update> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        UpdateConsumer delegate = UpdateConsumer.sync(consumer);
        return delegate::handle;
    }
}
