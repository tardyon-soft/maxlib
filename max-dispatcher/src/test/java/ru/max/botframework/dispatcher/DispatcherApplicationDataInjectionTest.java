package ru.max.botframework.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
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

class DispatcherApplicationDataInjectionTest {

    @Test
    void registerApplicationDataStoresSnapshot() {
        Dispatcher dispatcher = new Dispatcher();
        RuntimeDataKey<String> key = RuntimeDataKey.application("app.name", String.class);

        dispatcher.registerApplicationData(key, "max-bot");

        assertEquals("max-bot", dispatcher.applicationData().get(key));
    }

    @Test
    void registerApplicationDataRejectsNonApplicationScope() {
        Dispatcher dispatcher = new Dispatcher();
        RuntimeDataKey<String> key = RuntimeDataKey.middleware("traceId", String.class);

        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> dispatcher.registerApplicationData(key, "trace-1")
        );

        assertTrue(thrown.getMessage().contains("APPLICATION"));
    }

    @Test
    void registerApplicationDataRejectsSameNameWithDifferentType() {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.registerApplicationData(RuntimeDataKey.application("service.repo", String.class), "repo");

        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> dispatcher.registerApplicationData(RuntimeDataKey.application("service.repo", Integer.class), 1)
        );

        assertTrue(thrown.getMessage().contains("service.repo"));
    }

    @Test
    void registerApplicationDataRejectsConflictingValueForSameKey() {
        Dispatcher dispatcher = new Dispatcher();
        RuntimeDataKey<String> key = RuntimeDataKey.application("service.name", String.class);
        dispatcher.registerApplicationData(key, "orders");

        assertThrows(
                RuntimeDataConflictException.class,
                () -> dispatcher.registerApplicationData(key, "payments")
        );
    }

    @Test
    void reflectiveHandlerReceivesRegisteredServiceByType() throws Exception {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        GreetingService service = text -> "hi " + text;
        ServiceProbe probe = new ServiceProbe();
        Method method = ServiceProbe.class.getDeclaredMethod("onMessage", Message.class, GreetingService.class);
        router.message(ReflectiveEventHandler.of(probe, method, DefaultHandlerInvoker.withDefaults()));
        dispatcher.includeRouter(router);
        dispatcher.registerService(GreetingService.class, service);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate("max")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertSame(service, probe.lastService.get());
        assertEquals("hi max", probe.lastGreeting.get());
    }

    @Test
    void reflectiveHandlerFailsWhenServiceIsMissing() throws Exception {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        ServiceProbe probe = new ServiceProbe();
        Method method = ServiceProbe.class.getDeclaredMethod("onMessage", Message.class, GreetingService.class);
        router.message(ReflectiveEventHandler.of(probe, method, DefaultHandlerInvoker.withDefaults()));
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate("max")).toCompletableFuture().join();

        assertEquals(DispatchStatus.FAILED, result.status());
        assertTrue(result.errorOpt().orElseThrow() instanceof MissingHandlerDependencyException);
        assertNull(probe.lastGreeting.get());
    }

    private interface GreetingService {
        String greet(String text);
    }

    private static final class ServiceProbe {
        private final AtomicReference<GreetingService> lastService = new AtomicReference<>();
        private final AtomicReference<String> lastGreeting = new AtomicReference<>();

        public CompletableFuture<Void> onMessage(Message message, GreetingService service) {
            lastService.set(service);
            lastGreeting.set(service.greet(message.text()));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static Update messageUpdate(String text) {
        return new Update(
                new UpdateId("u-app-1"),
                UpdateType.MESSAGE,
                message(text),
                null,
                null,
                Instant.parse("2026-03-12T00:00:00Z")
        );
    }

    private static Message message(String text) {
        return new Message(
                new MessageId("m-app-1"),
                new Chat(new ChatId("c-app-1"), ChatType.PRIVATE, "chat", null, null),
                new User(new UserId("u-app-user"), "demo", "Demo", "User", "Demo User", false, "en"),
                text,
                Instant.parse("2026-03-12T00:00:00Z"),
                null,
                List.of(),
                List.of()
        );
    }
}
