package ru.max.botframework.client;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import ru.max.botframework.client.method.GetMeRequest;
import ru.max.botframework.model.BotInfo;

/**
 * High-level entry point for executing typed MAX API requests.
 */
public interface MaxBotClient {

    <T> T execute(MaxRequest<T> request);

    default BotInfo getMe() {
        return execute(GetMeRequest.INSTANCE);
    }

    default CompletionStage<BotInfo> getMeAsync() {
        return executeAsync(GetMeRequest.INSTANCE);
    }

    default <T> CompletionStage<T> executeAsync(MaxRequest<T> request) {
        return CompletableFuture.supplyAsync(() -> execute(request));
    }
}
