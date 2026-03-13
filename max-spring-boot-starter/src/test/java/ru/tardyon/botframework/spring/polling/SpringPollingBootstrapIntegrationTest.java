package ru.tardyon.botframework.spring.polling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.ingestion.LongPollingRunner;
import ru.tardyon.botframework.ingestion.PollingBatch;
import ru.tardyon.botframework.ingestion.PollingFetchRequest;
import ru.tardyon.botframework.ingestion.PollingUpdateSource;
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

@SpringBootTest(
        classes = SpringPollingBootstrapIntegrationTest.TestApp.class,
        properties = {
                "max.bot.token=test-token",
                "max.bot.mode=POLLING",
                "max.bot.polling.enabled=true",
                "max.bot.polling.limit=1",
                "max.bot.polling.timeout=1s"
        },
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class SpringPollingBootstrapIntegrationTest {
    private static final AtomicReference<CountDownLatch> HANDLER_LATCH = new AtomicReference<>(new CountDownLatch(1));
    private static final AtomicInteger HANDLER_CALLS = new AtomicInteger();

    @Autowired
    private SpringPollingBootstrap bootstrap;

    @Autowired
    private LongPollingRunner runner;

    @Autowired
    private Dispatcher dispatcher;

    @BeforeEach
    void reset() {
        HANDLER_CALLS.set(0);
        HANDLER_LATCH.set(new CountDownLatch(1));
    }

    @Test
    void startupWiringStartsPollingRunner() {
        assertNotNull(dispatcher);
        assertNotNull(bootstrap.longPollingRunner().orElse(null));
        assertTrue(waitUntil(bootstrap::isRunning, 2000));
        assertTrue(runner.isRunning());
    }

    @Test
    void handlerInvocationThroughPollingPath() throws InterruptedException {
        boolean invoked = HANDLER_LATCH.get().await(2, TimeUnit.SECONDS);
        assertTrue(invoked);
        assertTrue(HANDLER_CALLS.get() > 0);
    }

    @Test
    void gracefulShutdownStopsRunner() {
        AtomicReference<LongPollingRunner> runnerRef = new AtomicReference<>();
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(TestApp.class)
                .properties(
                        "max.bot.token=test-token",
                        "max.bot.mode=POLLING",
                        "max.bot.polling.enabled=true",
                        "max.bot.polling.limit=1",
                        "max.bot.polling.timeout=1s"
                )
                .web(org.springframework.boot.WebApplicationType.NONE)
                .run()) {
            runnerRef.set(context.getBean(LongPollingRunner.class));
            assertTrue(waitUntil(() -> runnerRef.get().isRunning(), 2000));
        }

        assertNotNull(runnerRef.get());
        assertFalse(runnerRef.get().isRunning());
    }

    private static boolean waitUntil(java.util.function.BooleanSupplier condition, long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            try {
                Thread.sleep(25);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return condition.getAsBoolean();
    }

    @Configuration
    @EnableAutoConfiguration
    static class TestApp {
        @Bean
        PollingUpdateSource pollingUpdateSource() {
            return new PollingUpdateSource() {
                @Override
                public PollingBatch poll(PollingFetchRequest request) {
                    try {
                        Thread.sleep(25);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    return new PollingBatch(List.of(sampleUpdate("from-polling")), 1L);
                }
            };
        }

        @Bean
        Router pollingRouter() {
            Router router = new Router("polling");
            router.message((message, context) -> {
                HANDLER_CALLS.incrementAndGet();
                HANDLER_LATCH.get().countDown();
                return CompletableFuture.completedFuture(null);
            });
            return router;
        }

        private static Update sampleUpdate(String text) {
            return new Update(
                    new UpdateId("u-polling-1"),
                    UpdateType.MESSAGE,
                    new Message(
                            new MessageId("m-polling-1"),
                            new Chat(new ChatId("c-polling-1"), ChatType.PRIVATE, "chat", null, null),
                            new User(new UserId("u-polling-1"), "demo", "Demo", "User", "Demo User", false, "en"),
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
        }
    }
}
