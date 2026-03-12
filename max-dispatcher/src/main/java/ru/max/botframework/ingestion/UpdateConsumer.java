package ru.max.botframework.ingestion;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import ru.max.botframework.model.Update;

/**
 * Preferred contract for consuming normalized updates in ingestion flow.
 */
@FunctionalInterface
public interface UpdateConsumer {

    CompletionStage<UpdateHandlingResult> handle(Update update);

    static UpdateConsumer sync(Consumer<Update> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        return update -> {
            try {
                consumer.accept(Objects.requireNonNull(update, "update"));
                return CompletableFuture.completedFuture(UpdateHandlingResult.success());
            } catch (RuntimeException exception) {
                return CompletableFuture.completedFuture(UpdateHandlingResult.failure(exception));
            }
        };
    }
}
