package ru.tardyon.botframework.quarkus.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import jakarta.inject.Inject;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletionStage;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
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

@QuarkusComponentTest({ru.tardyon.botframework.quarkus.runtime.MaxBotProducer.class, QuarkusWebhookResource.class})
@TestConfigProperty(key = "max.bot.token", value = "test-token")
@TestConfigProperty(key = "max.bot.mode", value = "POLLING")
@TestConfigProperty(key = "max.bot.webhook.enabled", value = "false")
class QuarkusWebhookDisabledTest {
    private static final JacksonJsonCodec CODEC = new JacksonJsonCodec();

    @Inject
    QuarkusWebhookResource resource;

    @Test
    void disabledWebhookReturnsNotFoundAndDoesNotPublishAdapter() {
        CompletionStage<Response> response = resource.handle(validUpdateJson().getBytes(StandardCharsets.UTF_8), headers("ignored"));
        assertEquals(404, response.toCompletableFuture().join().getStatus());
    }

    private static HttpHeaders headers(String secret) {
        MultivaluedMap<String, String> requestHeaders = new MultivaluedHashMap<>();
        requestHeaders.putSingle("Content-Type", "application/json");
        requestHeaders.putSingle("X-Max-Bot-Api-Secret", secret);
        return (HttpHeaders) Proxy.newProxyInstance(
                QuarkusWebhookDisabledTest.class.getClassLoader(),
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

    private static String validUpdateJson() {
        Update update = new Update(
                new UpdateId("u-webhook-disabled-1"),
                UpdateType.MESSAGE,
                new Message(
                        new MessageId("m-webhook-disabled-1"),
                        new Chat(new ChatId("c-webhook-disabled-1"), ChatType.PRIVATE, "chat", null, null),
                        new User(new UserId("u-webhook-disabled-1"), "demo", "Demo", "User", "Demo User", false, "en"),
                        "hello",
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
