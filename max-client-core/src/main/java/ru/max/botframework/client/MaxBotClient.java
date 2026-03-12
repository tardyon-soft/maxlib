package ru.max.botframework.client;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * High-level entry point for executing typed MAX API requests.
 */
public interface MaxBotClient {

    <T> T execute(MaxRequest<T> request);

    default <T> CompletionStage<T> executeAsync(MaxRequest<T> request) {
        return CompletableFuture.supplyAsync(() -> execute(request));
    }
}
