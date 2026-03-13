package ru.max.botframework.spring.autoconfigure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import ru.max.botframework.client.MaxApiClientConfig;
import ru.max.botframework.client.MaxBotClient;
import ru.max.botframework.client.http.MaxHttpClient;
import ru.max.botframework.dispatcher.DispatchStatus;
import ru.max.botframework.dispatcher.Dispatcher;
import ru.max.botframework.dispatcher.Router;
import ru.max.botframework.fsm.FSMStorage;
import ru.max.botframework.fsm.StateScope;
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
import ru.max.botframework.spring.polling.SpringPollingBootstrap;
import ru.max.botframework.spring.properties.MaxBotProperties;
import ru.max.botframework.spring.properties.MaxBotStorageType;
import ru.max.botframework.spring.webhook.SpringWebhookAdapter;

class MaxBotAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MaxBotAutoConfiguration.class))
            .withPropertyValues("max.bot.token=test-token");

    @Test
    void autoConfigCreatesCoreSdkAndRuntimeBeans() {
        contextRunner.run(context -> {
            assertTrue(context.hasSingleBean(MaxBotProperties.class));
            assertTrue(context.hasSingleBean(MaxApiClientConfig.class));
            assertTrue(context.hasSingleBean(MaxHttpClient.class));
            assertTrue(context.hasSingleBean(MaxBotClient.class));
            assertTrue(context.hasSingleBean(FSMStorage.class));
            assertTrue(context.hasSingleBean(Dispatcher.class));
            assertTrue(context.hasSingleBean(SpringPollingBootstrap.class));

            assertEquals(false, context.containsBean("springWebhookAdapter"));
            MaxBotProperties properties = context.getBean(MaxBotProperties.class);
            assertEquals(MaxBotStorageType.MEMORY, properties.getStorage().getType());
        });
    }

    @Test
    void dispatcherIncludesRouterBeans() {
        Router a = new Router("a");
        Router b = new Router("b");

        contextRunner
                .withBean("routerA", Router.class, () -> a)
                .withBean("routerB", Router.class, () -> b)
                .run(context -> {
                    Dispatcher dispatcher = context.getBean(Dispatcher.class);
                    assertEquals(2, dispatcher.routers().size());
                    assertSame(a, dispatcher.routers().get(0));
                    assertSame(b, dispatcher.routers().get(1));
                });
    }

    @Test
    void backsOffWhenUserProvidesDispatcherBean() {
        Dispatcher custom = new Dispatcher();
        contextRunner
                .withBean(Dispatcher.class, () -> custom)
                .run(context -> assertSame(custom, context.getBean(Dispatcher.class)));
    }

    @Test
    void backsOffWhenUserProvidesMaxBotClientBean() {
        MaxBotClient custom = request -> {
            throw new UnsupportedOperationException("custom");
        };

        contextRunner
                .withBean(MaxBotClient.class, () -> custom)
                .run(context -> assertSame(custom, context.getBean(MaxBotClient.class)));
    }

    @Test
    void appliesConfiguredStateScopeToRuntime() {
        AtomicReference<StateScope> seenScope = new AtomicReference<>();
        Router router = new Router("scope");
        router.message((message, fsm) -> {
            seenScope.set(fsm.scope().scope());
            return CompletableFuture.completedFuture(null);
        });

        contextRunner
                .withPropertyValues("max.bot.storage.state-scope=CHAT")
                .withBean("scopeRouter", Router.class, () -> router)
                .run(context -> {
                    Dispatcher dispatcher = context.getBean(Dispatcher.class);
                    var result = dispatcher.feedUpdate(sampleUpdate("hello")).toCompletableFuture().join();
                    assertEquals(DispatchStatus.HANDLED, result.status());
                    assertEquals(StateScope.CHAT, seenScope.get());
                });
    }

    @Test
    void createsWebhookAdapterWhenWebhookReceiverBeanExists() {
        contextRunner
                .withPropertyValues("max.bot.mode=WEBHOOK")
                .withBean(ru.max.botframework.ingestion.WebhookReceiver.class, () -> request -> {
                    assertNotNull(request);
                    return CompletableFuture.completedFuture(
                            ru.max.botframework.ingestion.WebhookReceiveResult.accepted(null)
                    );
                })
                .run(context -> assertTrue(context.hasSingleBean(SpringWebhookAdapter.class)));
    }

    private static Update sampleUpdate(String text) {
        return new Update(
                new UpdateId("u-spring-1"),
                UpdateType.MESSAGE,
                new Message(
                        new MessageId("m-spring-1"),
                        new Chat(new ChatId("c-spring-1"), ChatType.PRIVATE, "chat", null, null),
                        new User(new UserId("u-spring-1"), "demo", "Demo", "User", "Demo User", false, "en"),
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
