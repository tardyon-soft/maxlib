package ru.max.botframework.spring.webhook;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.max.botframework.client.serialization.JacksonJsonCodec;
import ru.max.botframework.dispatcher.Router;
import ru.max.botframework.model.Chat;
import ru.max.botframework.model.ChatId;
import ru.max.botframework.model.ChatType;
import ru.max.botframework.model.Message;
import ru.max.botframework.model.MessageId;
import ru.max.botframework.model.Update;
import ru.max.botframework.model.UpdateId;
import ru.max.botframework.model.UpdateType;
import ru.max.botframework.model.User;
import ru.max.botframework.model.UserId;

@SpringBootTest(
        classes = SpringWebhookControllerIntegrationTest.TestApp.class,
        properties = {
                "max.bot.token=test-token",
                "max.bot.mode=WEBHOOK",
                "max.bot.webhook.enabled=true",
                "max.bot.webhook.path=/hooks/max",
                "max.bot.webhook.secret=integration-secret"
        }
)
@AutoConfigureMockMvc
class SpringWebhookControllerIntegrationTest {
    private static final AtomicInteger HANDLER_CALLS = new AtomicInteger();
    private static final JacksonJsonCodec CODEC = new JacksonJsonCodec();

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void resetCounter() {
        HANDLER_CALLS.set(0);
    }

    @Test
    void validWebhookRequestReturnsOkAndInvokesHandler() throws Exception {
        mockMvc.perform(post("/hooks/max")
                        .header("X-Max-Bot-Api-Secret", "integration-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateJson("hello")))
                .andExpect(status().isOk());

        org.junit.jupiter.api.Assertions.assertEquals(1, HANDLER_CALLS.get());
    }

    @Test
    void invalidSecretReturnsForbidden() throws Exception {
        mockMvc.perform(post("/hooks/max")
                        .header("X-Max-Bot-Api-Secret", "wrong-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateJson("hello")))
                .andExpect(status().isForbidden());

        org.junit.jupiter.api.Assertions.assertEquals(0, HANDLER_CALLS.get());
    }

    @Test
    void malformedPayloadReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/hooks/max")
                        .header("X-Max-Bot-Api-Secret", "integration-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"broken\":"))
                .andExpect(status().isBadRequest());

        org.junit.jupiter.api.Assertions.assertEquals(0, HANDLER_CALLS.get());
    }

    @Test
    void webhookPathDispatchesIntoRuntimePipeline() throws Exception {
        mockMvc.perform(post("/hooks/max")
                        .header("X-Max-Bot-Api-Secret", "integration-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateJson("invoke-through-webhook")))
                .andExpect(status().isOk());

        org.junit.jupiter.api.Assertions.assertEquals(1, HANDLER_CALLS.get());
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

    @Configuration
    @EnableAutoConfiguration
    static class TestApp {
        @Bean
        Router webhookRouter() {
            Router router = new Router("webhook");
            router.message((message, context) -> {
                HANDLER_CALLS.incrementAndGet();
                return CompletableFuture.completedFuture(null);
            });
            return router;
        }
    }
}
