package ru.tardyon.botframework.quarkus.polling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.ingestion.DefaultLongPollingRunner;
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
import ru.tardyon.botframework.screen.Screens;

@QuarkusComponentTest({
        ru.tardyon.botframework.quarkus.runtime.MaxBotProducer.class,
        QuarkusPollingFactory.class,
        QuarkusPollingBootstrap.class,
        QuarkusPollingLifecycle.class,
        QuarkusPollingWiringTest.TrackingBeans.class
})
@TestConfigProperty(key = "max.bot.token", value = "test-token")
@TestConfigProperty(key = "max.bot.mode", value = "POLLING")
@TestConfigProperty(key = "max.bot.polling.enabled", value = "true")
@TestConfigProperty(key = "max.bot.polling.limit", value = "20")
@TestConfigProperty(key = "max.bot.polling.timeout", value = "45s")
@TestConfigProperty(key = "max.bot.polling.types[0]", value = "message_created")
@TestConfigProperty(key = "max.bot.polling.types[1]", value = "message_callback")
class QuarkusPollingWiringTest {
    @Inject
    LongPollingRunnerConfig config;

    @Inject
    LongPollingRunner runner;

    @Inject
    QuarkusPollingBootstrap bootstrap;

    @Inject
    QuarkusPollingLifecycle lifecycle;

    @Inject
    Dispatcher dispatcher;

    @BeforeEach
    void resetState() {
        TrackingBeans.FIRST_REQUEST.set(new CountDownLatch(1));
        TrackingBeans.FIRST_HANDLED_UPDATE.set(new CountDownLatch(1));
        TrackingBeans.LAST_REQUEST.set(null);
        TrackingBeans.POLL_CALLS.set(0);
        TrackingBeans.HANDLED_UPDATES.set(0);
        TrackingBeans.DISPATCHER = null;
    }

    @Test
    void runnerCreationMatchesSpringMicronautAssembly() {
        assertInstanceOf(DefaultLongPollingRunner.class, runner);
        assertEquals(null, config.request().marker());
        assertEquals(Integer.valueOf(45), config.request().timeout());
        assertEquals(Integer.valueOf(20), config.request().limit());
        assertEquals(List.of(UpdateEventType.MESSAGE_CREATED, UpdateEventType.MESSAGE_CALLBACK), config.request().types());
        assertTrue(bootstrap.longPollingRunner().isPresent());
    }

    @Test
    void runtimeFeedThroughUsesAssembledRequest() throws Exception {
        assertTrue(TrackingBeans.FIRST_REQUEST.get().await(2, TimeUnit.SECONDS));
        assertTrue(TrackingBeans.FIRST_HANDLED_UPDATE.get().await(2, TimeUnit.SECONDS));
        assertTrue(TrackingBeans.LAST_REQUEST.get() != null);
        assertEquals(Integer.valueOf(45), TrackingBeans.LAST_REQUEST.get().timeout());
        assertEquals(Integer.valueOf(20), TrackingBeans.LAST_REQUEST.get().limit());
        assertEquals(List.of(UpdateEventType.MESSAGE_CREATED, UpdateEventType.MESSAGE_CALLBACK), TrackingBeans.LAST_REQUEST.get().types());
        assertEquals(1, TrackingBeans.HANDLED_UPDATES.get());
        assertEquals("max.screen", dispatcher.applicationData().get(Screens.SCREEN_FSM_NAMESPACE_KEY));
    }

    @Singleton
    static final class TrackingBeans {
        static final AtomicReference<CountDownLatch> FIRST_REQUEST = new AtomicReference<>(new CountDownLatch(1));
        static final AtomicReference<CountDownLatch> FIRST_HANDLED_UPDATE = new AtomicReference<>(new CountDownLatch(1));
        static final AtomicReference<PollingFetchRequest> LAST_REQUEST = new AtomicReference<>();
        static final AtomicInteger POLL_CALLS = new AtomicInteger();
        static final AtomicInteger HANDLED_UPDATES = new AtomicInteger();
        static Dispatcher DISPATCHER;

        @Produces
        PollingUpdateSource pollingUpdateSource() {
            return request -> {
                LAST_REQUEST.compareAndSet(null, request);
                FIRST_REQUEST.get().countDown();
                int call = POLL_CALLS.incrementAndGet();
                if (call > 1) {
                    return new PollingBatch(List.of(), 1L);
                }
                return new PollingBatch(List.of(sampleUpdate("from-polling")), 1L);
            };
        }

        @Produces
        Dispatcher dispatcher() {
            Dispatcher dispatcher = new Dispatcher();
            DISPATCHER = dispatcher;
            dispatcher.registerApplicationData(Screens.SCREEN_FSM_NAMESPACE_KEY, "max.screen");
            dispatcher.includeRouter(new ru.tardyon.botframework.dispatcher.Router("polling")
                    .message((message, context) -> {
                        HANDLED_UPDATES.incrementAndGet();
                        FIRST_HANDLED_UPDATE.get().countDown();
                        return CompletableFuture.completedFuture(null);
                    }));
            return dispatcher;
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
