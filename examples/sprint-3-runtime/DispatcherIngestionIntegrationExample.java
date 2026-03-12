package ru.max.botframework.examples.sprint3;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import ru.max.botframework.client.MaxBotClient;
import ru.max.botframework.client.serialization.JacksonJsonCodec;
import ru.max.botframework.dispatcher.Dispatcher;
import ru.max.botframework.dispatcher.Router;
import ru.max.botframework.ingestion.DefaultLongPollingRunner;
import ru.max.botframework.ingestion.DefaultWebhookReceiver;
import ru.max.botframework.ingestion.DefaultWebhookSecretValidator;
import ru.max.botframework.ingestion.LongPollingRunnerConfig;
import ru.max.botframework.ingestion.PollingFetchRequest;
import ru.max.botframework.ingestion.SdkPollingUpdateSource;
import ru.max.botframework.ingestion.WebhookReceiveResult;
import ru.max.botframework.ingestion.WebhookReceiveStatus;
import ru.max.botframework.ingestion.WebhookRequest;
import ru.max.botframework.model.UpdateEventType;

public final class DispatcherIngestionIntegrationExample {

    private DispatcherIngestionIntegrationExample() {
    }

    public static DefaultLongPollingRunner createPollingRunner(MaxBotClient botClient) {
        Dispatcher dispatcher = runtimeDispatcher();
        return new DefaultLongPollingRunner(
                new SdkPollingUpdateSource(botClient),
                dispatcher,
                LongPollingRunnerConfig.builder()
                        .request(new PollingFetchRequest(
                                null,
                                30,
                                100,
                                List.of(UpdateEventType.MESSAGE_CREATED, UpdateEventType.MESSAGE_CALLBACK)
                        ))
                        .idleDelay(Duration.ofMillis(200))
                        .shutdownTimeout(Duration.ofSeconds(2))
                        .build()
        );
    }

    public static int handleWebhook(byte[] rawBody, String secretHeader) {
        Dispatcher dispatcher = runtimeDispatcher();
        DefaultWebhookReceiver receiver = new DefaultWebhookReceiver(
                new DefaultWebhookSecretValidator("my-webhook-secret"),
                new JacksonJsonCodec(),
                dispatcher
        );

        WebhookRequest request = new WebhookRequest(
                rawBody,
                Map.of(DefaultWebhookSecretValidator.SECRET_HEADER_NAME, List.of(secretHeader))
        );
        WebhookReceiveResult result = receiver.receive(request).toCompletableFuture().join();
        return toHttpStatus(result.status());
    }

    private static Dispatcher runtimeDispatcher() {
        Router root = new Router("root")
                .message(message -> {
                    System.out.println("Message update: " + message.text());
                    return CompletableFuture.completedFuture(null);
                })
                .callback(callback -> {
                    System.out.println("Callback update: " + callback.data());
                    return CompletableFuture.completedFuture(null);
                });

        return new Dispatcher().includeRouter(root);
    }

    private static int toHttpStatus(WebhookReceiveStatus status) {
        return switch (status) {
            case ACCEPTED -> 200;
            case INVALID_SECRET -> 403;
            case BAD_PAYLOAD -> 400;
            case OVERLOADED -> 429;
            case INTERNAL_ERROR -> 500;
        };
    }
}

