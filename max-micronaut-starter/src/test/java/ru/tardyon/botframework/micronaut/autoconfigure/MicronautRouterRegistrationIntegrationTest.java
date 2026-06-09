package ru.tardyon.botframework.micronaut.autoconfigure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Order;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.dispatcher.DispatchResult;
import ru.tardyon.botframework.dispatcher.DispatchStatus;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.dispatcher.annotation.Command;
import ru.tardyon.botframework.dispatcher.annotation.Route;
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

class MicronautRouterRegistrationIntegrationTest {

    @Test
    void manualRouterBeanIsRegisteredAndInvoked() {
        SingleRouterFactory.CALLS.set(0);

        try (ApplicationContext context = context("single-router", Map.of(
                "max.bot.polling.enabled", "false",
                "max.bot.route-component-scan.enabled", "false"
        ))) {
            Dispatcher dispatcher = context.getBean(Dispatcher.class);
            assertEquals(1, dispatcher.routers().size());

            DispatchResult result = dispatcher.feedUpdate(sampleUpdate("hello")).toCompletableFuture().join();
            assertEquals(DispatchStatus.HANDLED, result.status());
            assertEquals(1, SingleRouterFactory.CALLS.get());
        }
    }

    @Test
    void orderedRoutersRespectOrderAndFirstMatch() {
        OrderedRoutersFactory.reset();

        try (ApplicationContext context = context("ordered-routers", Map.of(
                "max.bot.polling.enabled", "false",
                "max.bot.route-component-scan.enabled", "false"
        ))) {
            Dispatcher dispatcher = context.getBean(Dispatcher.class);
            List<Router> routers = dispatcher.routers();
            assertEquals(2, routers.size());
            assertEquals("first", routers.get(0).name());
            assertEquals("second", routers.get(1).name());

            DispatchResult result = dispatcher.feedUpdate(sampleUpdate("hello")).toCompletableFuture().join();
            assertEquals(DispatchStatus.HANDLED, result.status());
            assertEquals(1, OrderedRoutersFactory.FIRST_CALLS.get());
            assertEquals(0, OrderedRoutersFactory.SECOND_CALLS.get());
        }
    }

    @Test
    void routerBeanCanComposeChildRoutersViaIncludeRouter() {
        ComposedRouterFactory.CHILD_CALLS.set(0);

        try (ApplicationContext context = context("composed-router", Map.of(
                "max.bot.polling.enabled", "false",
                "max.bot.route-component-scan.enabled", "false"
        ))) {
            Dispatcher dispatcher = context.getBean(Dispatcher.class);
            assertEquals(1, dispatcher.routers().size());
            assertTrue(dispatcher.routers().get(0).children().stream().anyMatch(router -> "child".equals(router.name())));

            DispatchResult result = dispatcher.feedUpdate(sampleUpdate("hello")).toCompletableFuture().join();
            assertEquals(DispatchStatus.HANDLED, result.status());
            assertEquals(1, ComposedRouterFactory.CHILD_CALLS.get());
        }
    }

    @Test
    void routeBeanWithAutoRegisterTrueIsRegistered() {
        AutoRouteFactory.CALLS.set(0);

        try (ApplicationContext context = context("auto-route", Map.of(
                "max.bot.polling.enabled", "false",
                "max.bot.route-component-scan.enabled", "false"
        ))) {
            Dispatcher dispatcher = context.getBean(Dispatcher.class);
            DispatchResult result = dispatcher.feedUpdate(sampleUpdate("/start")).toCompletableFuture().join();

            assertEquals(DispatchStatus.HANDLED, result.status());
            assertEquals(1, AutoRouteFactory.CALLS.get());
        }
    }

    @Test
    void routeBeanWithAutoRegisterFalseIsIgnored() {
        ManualRouteFactory.CALLS.set(0);

        try (ApplicationContext context = context("manual-route", Map.of(
                "max.bot.polling.enabled", "false",
                "max.bot.route-component-scan.enabled", "false"
        ))) {
            Dispatcher dispatcher = context.getBean(Dispatcher.class);
            DispatchResult result = dispatcher.feedUpdate(sampleUpdate("/start")).toCompletableFuture().join();

            assertEquals(DispatchStatus.IGNORED, result.status());
            assertEquals(0, ManualRouteFactory.CALLS.get());
        }
    }

    @Test
    void routeAnnotatedClassIsAutoDetectedWithoutExplicitBeanDeclaration() {
        AutoDetectedRouteController.CALLS.set(0);

        try (ApplicationContext context = context("autodetected-route-scan", Map.of(
                "max.bot.polling.enabled", "false",
                "max.bot.route-component-scan.enabled", "true"
        ))) {
            Dispatcher dispatcher = context.getBean(Dispatcher.class);
            DispatchResult result = dispatcher.feedUpdate(sampleUpdate("/autodetected")).toCompletableFuture().join();

            assertEquals(DispatchStatus.HANDLED, result.status());
            assertEquals(1, AutoDetectedRouteController.CALLS.get());
        }
    }

    private static ApplicationContext context(String specName, Map<String, Object> extraProperties) {
        LinkedHashMap<String, Object> properties = new LinkedHashMap<>();
        properties.put("spec.name", specName);
        properties.put("max.bot.token", "test-token");
        properties.putAll(extraProperties);
        return ApplicationContext.builder()
                .properties(properties)
                .start();
    }

    private static Update sampleUpdate(String text) {
        return new Update(
                new UpdateId("u-micronaut-router-1"),
                UpdateType.MESSAGE,
                new Message(
                        new MessageId("m-micronaut-router-1"),
                        new Chat(new ChatId("c-micronaut-router-1"), ChatType.PRIVATE, "chat", null, null),
                        new User(new UserId("u-micronaut-router-1"), "demo", "Demo", "User", "Demo User", false, "en"),
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

    @Factory
    @Requires(property = "spec.name", value = "single-router")
    static final class SingleRouterFactory {
        static final AtomicInteger CALLS = new AtomicInteger();

        @Singleton
        Router singleRouter() {
            Router router = new Router("single");
            router.message((message, context) -> {
                CALLS.incrementAndGet();
                return CompletableFuture.completedFuture(null);
            });
            return router;
        }
    }

    @Factory
    @Requires(property = "spec.name", value = "ordered-routers")
    static final class OrderedRoutersFactory {
        static final AtomicInteger FIRST_CALLS = new AtomicInteger();
        static final AtomicInteger SECOND_CALLS = new AtomicInteger();

        static void reset() {
            FIRST_CALLS.set(0);
            SECOND_CALLS.set(0);
        }

        @Singleton
        @Order(1)
        Router firstRouter() {
            Router first = new Router("first");
            first.message((message, context) -> {
                FIRST_CALLS.incrementAndGet();
                return CompletableFuture.completedFuture(null);
            });
            return first;
        }

        @Singleton
        @Order(2)
        Router secondRouter() {
            Router second = new Router("second");
            second.message((message, context) -> {
                SECOND_CALLS.incrementAndGet();
                return CompletableFuture.completedFuture(null);
            });
            return second;
        }
    }

    @Factory
    @Requires(property = "spec.name", value = "composed-router")
    static final class ComposedRouterFactory {
        static final AtomicInteger CHILD_CALLS = new AtomicInteger();

        @Singleton
        Router composedRouter() {
            Router root = new Router("root");
            Router child = new Router("child");
            child.message((message, context) -> {
                CHILD_CALLS.incrementAndGet();
                return CompletableFuture.completedFuture(null);
            });
            root.includeRouter(child);
            return root;
        }
    }

    @Factory
    @Requires(property = "spec.name", value = "auto-route")
    static final class AutoRouteFactory {
        static final AtomicInteger CALLS = new AtomicInteger();

        @Singleton
        RouteController autoRoute() {
            return new RouteController();
        }

        @Route(value = "auto", autoRegister = true)
        static class RouteController {
            @Command("start")
            public java.util.concurrent.CompletionStage<Void> onStart() {
                CALLS.incrementAndGet();
                return CompletableFuture.completedFuture(null);
            }
        }
    }

    @Factory
    @Requires(property = "spec.name", value = "manual-route")
    static final class ManualRouteFactory {
        static final AtomicInteger CALLS = new AtomicInteger();

        @Singleton
        ManualController manualRoute() {
            return new ManualController();
        }

        @Route(value = "manual", autoRegister = false)
        static class ManualController {
            @Command("start")
            public java.util.concurrent.CompletionStage<Void> onStart() {
                CALLS.incrementAndGet();
                return CompletableFuture.completedFuture(null);
            }
        }
    }

    @Factory
    @Requires(property = "spec.name", value = "autodetected-route-scan")
    static final class AutoDetectedRouteScanFactory {
        @Singleton
        Object packageMarker() {
            return new Object();
        }
    }
}
