package ru.tardyon.botframework.micronaut.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.runtime.server.EmbeddedServer;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.client.serialization.JacksonJsonCodec;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.ingestion.WebhookReceiveResult;
import ru.tardyon.botframework.ingestion.WebhookReceiver;
import ru.tardyon.botframework.model.Chat;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.ChatType;
import ru.tardyon.botframework.model.Message;
import ru.tardyon.botframework.model.MessageId;
import ru.tardyon.botframework.model.Update;
import ru.tardyon.botframework.model.UpdateId;
import ru.tardyon.botframework.model.UpdateType;
import ru.tardyon.botframework.model.User;
import ru.tardyon.botframework.model.UserId;

class MicronautWebhookControllerIntegrationTest {
    private static final AtomicInteger HANDLER_CALLS = new AtomicInteger();
    private static final JacksonJsonCodec CODEC = new JacksonJsonCodec();

    @BeforeEach
    void resetCounter() {
        HANDLER_CALLS.set(0);
    }

    @Test
    void validWebhookRequestReturnsOkAndInvokesHandler() {
        try (EmbeddedServer server = server("webhook-runtime", Map.of(
                "max.bot.mode", "WEBHOOK",
                "max.bot.webhook.enabled", "true",
                "max.bot.webhook.path", "/hooks/max",
                "max.bot.webhook.secret", "integration-secret"
        ))) {
            HttpResponse<String> response = client(server).toBlocking().exchange(
                    HttpRequest.POST("/hooks/max", validUpdateJson("hello"))
                            .header("X-Max-Bot-Api-Secret", "integration-secret")
                            .contentType(MediaType.APPLICATION_JSON),
                    String.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            assertEquals(1, HANDLER_CALLS.get());
        }
    }

    @Test
    void invalidSecretReturnsForbidden() {
        try (EmbeddedServer server = server("webhook-runtime", Map.of(
                "max.bot.mode", "WEBHOOK",
                "max.bot.webhook.enabled", "true",
                "max.bot.webhook.path", "/hooks/max",
                "max.bot.webhook.secret", "integration-secret"
        ))) {
            HttpResponse<String> response = exchange(server,
                    HttpRequest.POST("/hooks/max", validUpdateJson("hello"))
                            .header("X-Max-Bot-Api-Secret", "wrong-secret")
                            .contentType(MediaType.APPLICATION_JSON));

            assertEquals(HttpStatus.FORBIDDEN, response.getStatus());
            assertEquals(0, HANDLER_CALLS.get());
        }
    }

    @Test
    void malformedPayloadReturnsBadRequest() {
        try (EmbeddedServer server = server("webhook-runtime", Map.of(
                "max.bot.mode", "WEBHOOK",
                "max.bot.webhook.enabled", "true",
                "max.bot.webhook.path", "/hooks/max",
                "max.bot.webhook.secret", "integration-secret"
        ))) {
            HttpResponse<String> response = exchange(server,
                    HttpRequest.POST("/hooks/max", "{\"broken\":")
                            .header("X-Max-Bot-Api-Secret", "integration-secret")
                            .contentType(MediaType.APPLICATION_JSON));

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
            assertEquals(0, HANDLER_CALLS.get());
        }
    }

    @Test
    void webhookPathDispatchesIntoRuntimePipeline() {
        try (EmbeddedServer server = server("webhook-runtime", Map.of(
                "max.bot.mode", "WEBHOOK",
                "max.bot.webhook.enabled", "true",
                "max.bot.webhook.path", "/hooks/max",
                "max.bot.webhook.secret", "integration-secret"
        ))) {
            HttpResponse<String> response = client(server).toBlocking().exchange(
                    HttpRequest.POST("/hooks/max", validUpdateJson("invoke-through-webhook"))
                            .header("X-Max-Bot-Api-Secret", "integration-secret")
                            .contentType(MediaType.APPLICATION_JSON),
                    String.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            assertEquals(1, HANDLER_CALLS.get());
        }
    }

    @Test
    void overloadedMapsToTooManyRequests() {
        try (EmbeddedServer server = server("webhook-overloaded", Map.of(
                "max.bot.mode", "WEBHOOK",
                "max.bot.webhook.enabled", "true",
                "max.bot.webhook.path", "/hooks/max",
                "max.bot.webhook.secret", "integration-secret"
        ))) {
            HttpResponse<String> response = exchange(server,
                    HttpRequest.POST("/hooks/max", validUpdateJson("hello"))
                            .header("X-Max-Bot-Api-Secret", "integration-secret")
                            .contentType(MediaType.APPLICATION_JSON));

            assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatus());
        }
    }

    @Test
    void internalErrorMapsToServerError() {
        try (EmbeddedServer server = server("webhook-internal-error", Map.of(
                "max.bot.mode", "WEBHOOK",
                "max.bot.webhook.enabled", "true",
                "max.bot.webhook.path", "/hooks/max",
                "max.bot.webhook.secret", "integration-secret"
        ))) {
            HttpResponse<String> response = exchange(server,
                    HttpRequest.POST("/hooks/max", validUpdateJson("hello"))
                            .header("X-Max-Bot-Api-Secret", "integration-secret")
                            .contentType(MediaType.APPLICATION_JSON));

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus());
        }
    }

    @Test
    void adapterPreservesBodyAndHeaderSemantics() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(
                "spec.name", "adapter-only",
                "max.bot.token", "test-token",
                "max.bot.mode", "WEBHOOK"
        ))) {
            MicronautWebhookAdapter adapter = context.getBean(MicronautWebhookAdapter.class);
            WebhookReceiveResult result = adapter.receive(
                    validUpdateJson("adapter"),
                    Map.of(HttpHeaders.CONTENT_TYPE, List.of(MediaType.APPLICATION_JSON), "X-Max-Bot-Api-Secret", List.of("s"))
            ).toCompletableFuture().join();

            assertEquals(ru.tardyon.botframework.ingestion.WebhookReceiveStatus.ACCEPTED, result.status());
            assertTrue(AdapterReceiverFactory.LAST_BODY.get().contains("adapter"));
            assertEquals("s", AdapterReceiverFactory.LAST_SECRET.get());
        }
    }

    private static EmbeddedServer server(String specName, Map<String, Object> properties) {
        LinkedHashMap<String, Object> config = new LinkedHashMap<>();
        config.put("spec.name", specName);
        config.put("micronaut.server.port", -1);
        config.put("max.bot.token", "test-token");
        config.put("max.bot.route-component-scan.enabled", "false");
        config.putAll(properties);
        return ApplicationContext.run(EmbeddedServer.class, config);
    }

    private static HttpClient client(EmbeddedServer server) {
        return server.getApplicationContext().createBean(HttpClient.class, server.getURL());
    }

    private static HttpResponse<String> exchange(EmbeddedServer server, HttpRequest<?> request) {
        try {
            return client(server).toBlocking().exchange(request, String.class);
        } catch (HttpClientResponseException e) {
            return e.getResponse().toMutableResponse().body(e.getResponse().getBody(String.class).orElse(null));
        }
    }

    private static String validUpdateJson(String text) {
        Update update = new Update(
                new UpdateId("u-webhook-1"),
                UpdateType.MESSAGE,
                new Message(
                        new MessageId("m-webhook-1"),
                        new Chat(new ChatId("c-webhook-1"), ChatType.PRIVATE, "chat", null, null),
                        new User(new UserId("u-webhook-1"), "demo", "Demo", "User", "Demo User", false, "en"),
                        text,
                        Instant.parse("2026-03-13T00:00:00Z"),
                        null,
                        List.of(),
                        List.of()
                ),
                null,
                null,
                Instant.parse("2026-03-13T00:00:00Z")
        );
        return CODEC.write(update);
    }

    @Factory
    @Requires(property = "spec.name", value = "webhook-runtime")
    static final class WebhookRuntimeFactory {
        @Singleton
        Router webhookRouter() {
            Router router = new Router("webhook");
            router.message((message, context) -> {
                HANDLER_CALLS.incrementAndGet();
                return CompletableFuture.completedFuture(null);
            });
            return router;
        }
    }

    @Factory
    @Requires(property = "spec.name", value = "webhook-overloaded")
    static final class OverloadedReceiverFactory {
        @Singleton
        WebhookReceiver webhookReceiver() {
            return request -> CompletableFuture.completedFuture(WebhookReceiveResult.overloaded("Webhook receiver is overloaded"));
        }
    }

    @Factory
    @Requires(property = "spec.name", value = "webhook-internal-error")
    static final class InternalErrorReceiverFactory {
        @Singleton
        WebhookReceiver webhookReceiver() {
            return request -> CompletableFuture.completedFuture(WebhookReceiveResult.internalError("Unexpected webhook receiver failure", null));
        }
    }

    @Factory
    @Requires(property = "spec.name", value = "adapter-only")
    static final class AdapterReceiverFactory {
        static final java.util.concurrent.atomic.AtomicReference<String> LAST_BODY = new java.util.concurrent.atomic.AtomicReference<>();
        static final java.util.concurrent.atomic.AtomicReference<String> LAST_SECRET = new java.util.concurrent.atomic.AtomicReference<>();

        @Singleton
        WebhookReceiver webhookReceiver() {
            return request -> {
                LAST_BODY.set(new String(request.body(), java.nio.charset.StandardCharsets.UTF_8));
                LAST_SECRET.set(request.header("X-Max-Bot-Api-Secret").orElse(null));
                return CompletableFuture.completedFuture(WebhookReceiveResult.accepted(null));
            };
        }
    }
}
