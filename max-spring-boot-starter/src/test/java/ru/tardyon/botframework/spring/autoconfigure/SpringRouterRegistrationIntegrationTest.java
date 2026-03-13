package ru.tardyon.botframework.spring.autoconfigure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import ru.tardyon.botframework.dispatcher.DispatchResult;
import ru.tardyon.botframework.dispatcher.DispatchStatus;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.dispatcher.Router;
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

class SpringRouterRegistrationIntegrationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MaxBotAutoConfiguration.class))
            .withPropertyValues(
                    "max.bot.token=test-token",
                    "max.bot.polling.enabled=false"
            );

    @Test
    void singleRouterBeanIsRegisteredAndInvoked() {
        AtomicInteger calls = new AtomicInteger();

        contextRunner
                .withBean("singleRouter", Router.class, () -> {
                    Router router = new Router("single");
                    router.message((message, context) -> {
                        calls.incrementAndGet();
                        return CompletableFuture.completedFuture(null);
                    });
                    return router;
                })
                .run(context -> {
                    Dispatcher dispatcher = context.getBean(Dispatcher.class);
                    assertEquals(1, dispatcher.routers().size());

                    DispatchResult result = dispatcher.feedUpdate(sampleUpdate("hello")).toCompletableFuture().join();
                    assertEquals(DispatchStatus.HANDLED, result.status());
                    assertEquals(1, calls.get());
                });
    }

    @Test
    void multipleRouterBeansRespectOrderAndFirstMatch() {
        OrderedRoutersConfig.reset();

        contextRunner
                .withUserConfiguration(OrderedRoutersConfig.class)
                .run(context -> {
                    Dispatcher dispatcher = context.getBean(Dispatcher.class);
                    List<Router> routers = dispatcher.routers();
                    assertEquals(2, routers.size());
                    assertEquals("first", routers.get(0).name());
                    assertEquals("second", routers.get(1).name());

                    DispatchResult result = dispatcher.feedUpdate(sampleUpdate("hello")).toCompletableFuture().join();
                    assertEquals(DispatchStatus.HANDLED, result.status());
                    assertEquals(1, OrderedRoutersConfig.FIRST_CALLS.get());
                    assertEquals(0, OrderedRoutersConfig.SECOND_CALLS.get());
                });
    }

    @Test
    void routerBeanCanComposeChildRoutersViaIncludeRouter() {
        AtomicInteger childCalls = new AtomicInteger();

        contextRunner
                .withBean("composedRouter", Router.class, () -> {
                    Router root = new Router("root");
                    Router child = new Router("child");
                    child.message((message, context) -> {
                        childCalls.incrementAndGet();
                        return CompletableFuture.completedFuture(null);
                    });
                    root.includeRouter(child);
                    return root;
                })
                .run(context -> {
                    Dispatcher dispatcher = context.getBean(Dispatcher.class);
                    assertEquals(1, dispatcher.routers().size());
                    assertTrue(dispatcher.routers().get(0).children().stream().anyMatch(router -> "child".equals(router.name())));

                    DispatchResult result = dispatcher.feedUpdate(sampleUpdate("hello")).toCompletableFuture().join();
                    assertEquals(DispatchStatus.HANDLED, result.status());
                    assertEquals(1, childCalls.get());
                });
    }

    private static Update sampleUpdate(String text) {
        return new Update(
                new UpdateId("u-spring-router-1"),
                UpdateType.MESSAGE,
                new Message(
                        new MessageId("m-spring-router-1"),
                        new Chat(new ChatId("c-spring-router-1"), ChatType.PRIVATE, "chat", null, null),
                        new User(new UserId("u-spring-router-1"), "demo", "Demo", "User", "Demo User", false, "en"),
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

    @Configuration
    static class OrderedRoutersConfig {
        static final AtomicInteger FIRST_CALLS = new AtomicInteger();
        static final AtomicInteger SECOND_CALLS = new AtomicInteger();

        static void reset() {
            FIRST_CALLS.set(0);
            SECOND_CALLS.set(0);
        }

        @Bean
        @Order(1)
        Router firstRouter() {
            Router first = new Router("first");
            first.message((message, context) -> {
                FIRST_CALLS.incrementAndGet();
                return CompletableFuture.completedFuture(null);
            });
            return first;
        }

        @Bean
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
}
