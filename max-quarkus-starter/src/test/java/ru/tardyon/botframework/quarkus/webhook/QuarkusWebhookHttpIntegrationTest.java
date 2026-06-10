package ru.tardyon.botframework.quarkus.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.client.serialization.JacksonJsonCodec;
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

@QuarkusComponentTest({
        ru.tardyon.botframework.quarkus.runtime.MaxBotProducer.class,
        QuarkusWebhookResource.class,
        QuarkusWebhookReceiverAlternative.class,
        QuarkusWebhookRouterFactory.class
})
@TestConfigProperty(key = "max.bot.token", value = "test-token")
@TestConfigProperty(key = "max.bot.mode", value = "WEBHOOK")
@TestConfigProperty(key = "max.bot.webhook.enabled", value = "true")
@TestConfigProperty(key = "max.bot.webhook.path", value = "/hooks/max")
@TestConfigProperty(key = "max.bot.webhook.secret", value = "integration-secret")
class QuarkusWebhookHttpIntegrationTest {
    private static final JacksonJsonCodec CODEC = new JacksonJsonCodec();

    @jakarta.inject.Inject
    QuarkusWebhookResource resource;

    @BeforeEach
    void resetCounter() {
        QuarkusWebhookTestState.HANDLER_CALLS.set(0);
    }

    @Test
    void validRequestReturnsOk() {
        Response response = invoke(validUpdateJson("hello"), "integration-secret");

        assertEquals(200, response.getStatus());
        assertEquals(1, QuarkusWebhookTestState.HANDLER_CALLS.get());
    }

    @Test
    void invalidSecretReturnsForbidden() {
        Response response = invoke(validUpdateJson("hello"), "wrong-secret");

        assertEquals(403, response.getStatus());
        assertEquals(0, QuarkusWebhookTestState.HANDLER_CALLS.get());
    }

    @Test
    void malformedPayloadReturnsBadRequest() {
        Response response = invoke("{\"broken\":", "integration-secret");

        assertEquals(400, response.getStatus());
        assertEquals(0, QuarkusWebhookTestState.HANDLER_CALLS.get());
    }

    @Test
    void overloadedReturnsTooManyRequests() {
        Response response = invoke("{\"scenario\":\"overloaded\"}", "integration-secret");

        assertEquals(429, response.getStatus());
        assertEquals(0, QuarkusWebhookTestState.HANDLER_CALLS.get());
    }

    @Test
    void internalErrorReturnsServerError() {
        Response response = invoke("{\"scenario\":\"internal-error\"}", "integration-secret");

        assertEquals(500, response.getStatus());
        assertEquals(0, QuarkusWebhookTestState.HANDLER_CALLS.get());
    }

    private Response invoke(String body, String secret) {
        CompletionStage<Response> response = resource.handle(body.getBytes(java.nio.charset.StandardCharsets.UTF_8), headers(secret));
        return response.toCompletableFuture().join();
    }

    private static HttpHeaders headers(String secret) {
        MultivaluedMap<String, String> requestHeaders = new MultivaluedHashMap<>();
        requestHeaders.putSingle("Content-Type", "application/json");
        requestHeaders.putSingle("X-Max-Bot-Api-Secret", secret);
        return (HttpHeaders) Proxy.newProxyInstance(
                QuarkusWebhookHttpIntegrationTest.class.getClassLoader(),
                new Class<?>[]{HttpHeaders.class},
                (proxy, method, args) -> {
                    if ("getRequestHeaders".equals(method.getName())) {
                        return requestHeaders;
                    }
                    if ("getHeaderString".equals(method.getName()) && args != null && args.length == 1) {
                        List<String> values = requestHeaders.get(args[0]);
                        return values == null || values.isEmpty() ? null : values.get(0);
                    }
                    if ("toString".equals(method.getName())) {
                        return requestHeaders.toString();
                    }
                    if ("hashCode".equals(method.getName())) {
                        return requestHeaders.hashCode();
                    }
                    if ("equals".equals(method.getName()) && args != null && args.length == 1) {
                        return proxy == args[0];
                    }
                    return null;
                }
        );
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
}
