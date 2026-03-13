package ru.tardyon.botframework.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.tardyon.botframework.client.MaxBotClient;
import ru.tardyon.botframework.client.serialization.JacksonJsonCodec;
import ru.tardyon.botframework.dispatcher.DispatchStatus;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.model.request.GetUpdatesRequest;
import ru.tardyon.botframework.model.response.GetUpdatesResponse;

class DispatcherIngestionIntegrationTest {

    @Test
    void pollingRunnerDeliversUpdatesToDispatcherHandlers() throws Exception {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        GetUpdatesResponse multi = IngestionFixtures.read("polling-multi-updates-response.json", GetUpdatesResponse.class);
        GetUpdatesResponse empty = IngestionFixtures.read("polling-empty-response.json", GetUpdatesResponse.class);
        when(client.getUpdates(any(GetUpdatesRequest.class))).thenAnswer(new org.mockito.stubbing.Answer<GetUpdatesResponse>() {
            private int call;

            @Override
            public GetUpdatesResponse answer(org.mockito.invocation.InvocationOnMock invocation) {
                call++;
                return call == 1 ? multi : empty;
            }
        });

        CountDownLatch handledLatch = new CountDownLatch(2);
        AtomicInteger messageHandled = new AtomicInteger();
        AtomicInteger callbackHandled = new AtomicInteger();
        Router router = new Router("main")
                .message(message -> {
                    messageHandled.incrementAndGet();
                    handledLatch.countDown();
                    return java.util.concurrent.CompletableFuture.completedFuture(null);
                })
                .callback(callback -> {
                    callbackHandled.incrementAndGet();
                    handledLatch.countDown();
                    return java.util.concurrent.CompletableFuture.completedFuture(null);
                });
        Dispatcher dispatcher = new Dispatcher().includeRouter(router);

        DefaultLongPollingRunner runner = new DefaultLongPollingRunner(
                new SdkPollingUpdateSource(client),
                dispatcher,
                LongPollingRunnerConfig.builder()
                        .idleDelay(Duration.ofMillis(10))
                        .shutdownTimeout(Duration.ofMillis(200))
                        .build()
        );

        runner.start();
        assertTrue(handledLatch.await(1, TimeUnit.SECONDS));
        runner.stop();
        runner.shutdown();

        assertEquals(1, messageHandled.get());
        assertEquals(1, callbackHandled.get());
    }

    @Test
    void webhookReceiverDeliversUpdateToDispatcherHandler() {
        CountDownLatch handledLatch = new CountDownLatch(1);
        AtomicInteger messageHandled = new AtomicInteger();
        Router router = new Router("main")
                .message(message -> {
                    messageHandled.incrementAndGet();
                    handledLatch.countDown();
                    return java.util.concurrent.CompletableFuture.completedFuture(null);
                });
        Dispatcher dispatcher = new Dispatcher().includeRouter(router);

        DefaultWebhookReceiver receiver = new DefaultWebhookReceiver(
                new DefaultWebhookSecretValidator("secret-1"),
                new JacksonJsonCodec(),
                dispatcher
        );

        WebhookReceiveResult result = receiver.receive(new WebhookRequest(
                        IngestionFixtures.raw("webhook-valid-payload.json").getBytes(StandardCharsets.UTF_8),
                        Map.of(DefaultWebhookSecretValidator.SECRET_HEADER_NAME, List.of("secret-1"))
                ))
                .toCompletableFuture()
                .join();

        assertEquals(WebhookReceiveStatus.ACCEPTED, result.status());
        assertTrue(result.isAccepted());
        assertTrue(await(handledLatch));
        assertEquals(1, messageHandled.get());
    }

    @Test
    void webhookReceiverReturnsInternalErrorWhenDispatcherHandlerFails() {
        Router router = new Router("main")
                .message(message -> java.util.concurrent.CompletableFuture.failedFuture(new IllegalStateException("handler failure")));
        Dispatcher dispatcher = new Dispatcher().includeRouter(router);

        DefaultWebhookReceiver receiver = new DefaultWebhookReceiver(
                new DefaultWebhookSecretValidator("secret-1"),
                new JacksonJsonCodec(),
                dispatcher
        );

        WebhookReceiveResult result = receiver.receive(new WebhookRequest(
                        IngestionFixtures.raw("webhook-valid-payload.json").getBytes(StandardCharsets.UTF_8),
                        Map.of(DefaultWebhookSecretValidator.SECRET_HEADER_NAME, List.of("secret-1"))
                ))
                .toCompletableFuture()
                .join();

        assertEquals(WebhookReceiveStatus.INTERNAL_ERROR, result.status());
    }

    @Test
    void dispatcherAsUpdateSinkAdapterWorksForLegacySinkPath() {
        Router router = new Router("main")
                .message(message -> java.util.concurrent.CompletableFuture.completedFuture(null));
        Dispatcher dispatcher = new Dispatcher().includeRouter(router);

        var result = dispatcher.asUpdateSink()
                .handle(IngestionFixtures.read("webhook-valid-payload.json", ru.tardyon.botframework.model.Update.class))
                .toCompletableFuture()
                .join();

        assertTrue(result.isSuccess());
        assertEquals(DispatchStatus.HANDLED, dispatcher.feedUpdate(
                IngestionFixtures.read("webhook-valid-payload.json", ru.tardyon.botframework.model.Update.class)
        ).toCompletableFuture().join().status());
    }

    private static boolean await(CountDownLatch latch) {
        try {
            return latch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
