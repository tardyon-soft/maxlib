package ru.tardyon.botframework.quarkus.webhook;

import jakarta.enterprise.inject.Produces;
import java.util.concurrent.CompletableFuture;
import jakarta.inject.Singleton;
import ru.tardyon.botframework.dispatcher.Router;

@Singleton
public final class QuarkusWebhookRouterFactory {
    @Produces
    Router webhookRouter() {
        Router router = new Router("webhook");
        router.message((message, context) -> {
            QuarkusWebhookTestState.HANDLER_CALLS.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        });
        return router;
    }
}
