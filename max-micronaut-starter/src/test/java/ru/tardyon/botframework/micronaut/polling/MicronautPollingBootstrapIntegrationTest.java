package ru.tardyon.botframework.micronaut.polling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.ingestion.LongPollingRunner;
import ru.tardyon.botframework.ingestion.LongPollingRunnerConfig;
import ru.tardyon.botframework.ingestion.PollingBatch;
import ru.tardyon.botframework.ingestion.PollingFetchRequest;
import ru.tardyon.botframework.ingestion.PollingUpdateSource;
import ru.tardyon.botframework.model.Chat;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.ChatType;
import ru.tardyon.botframework.model.Message;
import ru.tardyon.botframework.model.MessageId;
import ru.tardyon.botframework.model.Update;
import ru.tardyon.botframework.model.UpdateEventType;
import ru.tardyon.botframework.model.UpdateId;
import ru.tardyon.botframework.model.UpdateType;
import ru.tardyon.botframework.model.User;
import ru.tardyon.botframework.model.UserId;

class MicronautPollingBootstrapIntegrationTest {
    private static final AtomicReference<CountDownLatch> HANDLER_LATCH = new AtomicReference<>(new CountDownLatch(1));
    private static final AtomicInteger HANDLER_CALLS = new AtomicInteger();
    private static final AtomicReference<PollingFetchRequest> LAST_REQUEST = new AtomicReference<>();

    @BeforeEach
    void reset() {
        HANDLER_CALLS.set(0);
        HANDLER_LATCH.set(new CountDownLatch(1));
        LAST_REQUEST.set(null);
    }

    @Test
    void startupWiringStartsPollingRunner() {
        try (ApplicationContext context = context("polling-active", Map.of(
                "max.bot.mode", "POLLING",
                "max.bot.polling.enabled", "true",
                "max.bot.polling.limit", "1",
                "max.bot.polling.timeout", "1s"
        ))) {
            MicronautPollingBootstrap bootstrap = context.getBean(MicronautPollingBootstrap.class);
            LongPollingRunner runner = context.getBean(LongPollingRunner.class);
            Dispatcher dispatcher = context.getBean(Dispatcher.class);

            assertNotNull(dispatcher);
            assertNotNull(bootstrap.longPollingRunner().orElse(null));
            assertTrue(waitUntil(bootstrap::isRunning, 2000));
            assertTrue(runner.isRunning());
        }
    }

    @Test
    void handlerInvocationThroughPollingPath() throws InterruptedException {
        try (ApplicationContext context = context("polling-active", Map.of(
                "max.bot.mode", "POLLING",
                "max.bot.polling.enabled", "true",
                "max.bot.polling.limit", "1",
                "max.bot.polling.timeout", "1s"
        ))) {
            boolean invoked = HANDLER_LATCH.get().await(2, TimeUnit.SECONDS);
            assertTrue(invoked);
            assertTrue(HANDLER_CALLS.get() > 0);
            assertNotNull(context.getBean(Dispatcher.class));
        }
    }

    @Test
    void gracefulShutdownStopsRunner() {
        AtomicReference<LongPollingRunner> runnerRef = new AtomicReference<>();
        try (ApplicationContext context = context("polling-active", Map.of(
                "max.bot.mode", "POLLING",
                "max.bot.polling.enabled", "true",
                "max.bot.polling.limit", "1",
                "max.bot.polling.timeout", "1s"
        ))) {
            runnerRef.set(context.getBean(LongPollingRunner.class));
            assertTrue(waitUntil(() -> runnerRef.get().isRunning(), 2000));
        }

        assertNotNull(runnerRef.get());
        assertFalse(runnerRef.get().isRunning());
    }

    @Test
    void requestAssemblyMatchesSpringStarter() {
        try (ApplicationContext context = context("polling-active", Map.of(
                "max.bot.mode", "POLLING",
                "max.bot.polling.enabled", "true",
                "max.bot.polling.limit", "20",
                "max.bot.polling.timeout", "45s",
                "max.bot.polling.types[0]", "message_created",
                "max.bot.polling.types[1]", "message_callback"
        ))) {
            LongPollingRunnerConfig config = context.getBean(LongPollingRunnerConfig.class);

            assertEquals(null, config.request().marker());
            assertEquals(45, config.request().timeout());
            assertEquals(20, config.request().limit());
            assertEquals(List.of(UpdateEventType.MESSAGE_CREATED, UpdateEventType.MESSAGE_CALLBACK), config.request().types());
        }
    }

    @Test
    void disabledPollingDoesNotCreatePollingRuntimePieces() {
        try (ApplicationContext context = context("polling-disabled", Map.of(
                "max.bot.mode", "POLLING",
                "max.bot.polling.enabled", "false"
        ))) {
            assertTrue(context.containsBean(MicronautPollingBootstrap.class));
            assertFalse(context.containsBean(PollingUpdateSource.class));
            assertFalse(context.containsBean(LongPollingRunnerConfig.class));
            assertFalse(context.containsBean(LongPollingRunner.class));
            assertFalse(context.containsBean(MicronautPollingLifecycle.class));
            assertTrue(context.getBean(MicronautPollingBootstrap.class).longPollingRunner().isEmpty());
        }
    }

    @Test
    void webhookModeDoesNotCreatePollingRuntimePieces() {
        try (ApplicationContext context = context("polling-disabled", Map.of(
                "max.bot.mode", "WEBHOOK",
                "max.bot.polling.enabled", "true"
        ))) {
            assertTrue(context.containsBean(MicronautPollingBootstrap.class));
            assertFalse(context.containsBean(PollingUpdateSource.class));
            assertFalse(context.containsBean(LongPollingRunnerConfig.class));
            assertFalse(context.containsBean(LongPollingRunner.class));
            assertFalse(context.containsBean(MicronautPollingLifecycle.class));
            assertTrue(context.getBean(MicronautPollingBootstrap.class).longPollingRunner().isEmpty());
        }
    }

    private static ApplicationContext context(String specName, Map<String, Object> extraProperties) {
        LinkedHashMap<String, Object> properties = new LinkedHashMap<>();
        properties.put("spec.name", specName);
        properties.put("max.bot.token", "test-token");
        properties.put("max.bot.route-component-scan.enabled", "false");
        properties.putAll(extraProperties);
        return ApplicationContext.builder()
                .properties(properties)
                .start();
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

    @Factory
    @Requires(property = "spec.name", value = "polling-active")
    static final class PollingTestFactory {
        @Singleton
        PollingUpdateSource pollingUpdateSource() {
            return request -> {
                LAST_REQUEST.set(request);
                try {
                    Thread.sleep(25);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                }
                return new PollingBatch(List.of(sampleUpdate("from-polling")), 1L);
            };
        }

        @Singleton
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
