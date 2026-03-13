package ru.tardyon.botframework.spring.autoconfigure;

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
import ru.tardyon.botframework.client.MaxApiClientConfig;
import ru.tardyon.botframework.client.MaxBotClient;
import ru.tardyon.botframework.client.http.MaxHttpClient;
import ru.tardyon.botframework.action.ChatActionsFacade;
import ru.tardyon.botframework.callback.CallbackFacade;
import ru.tardyon.botframework.dispatcher.DispatchStatus;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.fsm.FSMStorage;
import ru.tardyon.botframework.fsm.InMemorySceneRegistry;
import ru.tardyon.botframework.fsm.MemorySceneStorage;
import ru.tardyon.botframework.fsm.MemoryStorage;
import ru.tardyon.botframework.fsm.SceneRegistry;
import ru.tardyon.botframework.fsm.SceneStorage;
import ru.tardyon.botframework.fsm.StateScope;
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
import ru.tardyon.botframework.message.MediaMessagingFacade;
import ru.tardyon.botframework.message.MessagingFacade;
import ru.tardyon.botframework.spring.polling.SpringPollingBootstrap;
import ru.tardyon.botframework.spring.properties.MaxBotProperties;
import ru.tardyon.botframework.spring.properties.MaxBotStorageType;
import ru.tardyon.botframework.spring.webhook.SpringWebhookAdapter;
import ru.tardyon.botframework.upload.InputFile;
import ru.tardyon.botframework.upload.UploadRequest;
import ru.tardyon.botframework.upload.UploadResult;
import ru.tardyon.botframework.upload.UploadService;

class MaxBotAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MaxBotAutoConfiguration.class))
            .withPropertyValues(
                    "max.bot.token=test-token",
                    "max.bot.polling.enabled=false"
            );

    @Test
    void autoConfigCreatesCoreSdkAndRuntimeBeans() {
        contextRunner.run(context -> {
            assertTrue(context.hasSingleBean(MaxBotProperties.class));
            assertTrue(context.hasSingleBean(MaxApiClientConfig.class));
            assertTrue(context.hasSingleBean(MaxHttpClient.class));
            assertTrue(context.hasSingleBean(MaxBotClient.class));
            assertTrue(context.hasSingleBean(FSMStorage.class));
            assertTrue(context.hasSingleBean(SceneRegistry.class));
            assertTrue(context.hasSingleBean(SceneStorage.class));
            assertTrue(context.hasSingleBean(MessagingFacade.class));
            assertTrue(context.hasSingleBean(CallbackFacade.class));
            assertTrue(context.hasSingleBean(ChatActionsFacade.class));
            assertTrue(context.hasSingleBean(Dispatcher.class));
            assertTrue(context.hasSingleBean(SpringPollingBootstrap.class));

            assertEquals(false, context.containsBean("springWebhookAdapter"));
            assertEquals(false, context.containsBean("mediaMessagingFacade"));
            MaxBotProperties properties = context.getBean(MaxBotProperties.class);
            assertEquals(MaxBotStorageType.MEMORY, properties.getStorage().getType());
            assertTrue(context.getBean(FSMStorage.class) instanceof MemoryStorage);
            assertTrue(context.getBean(SceneRegistry.class) instanceof InMemorySceneRegistry);
            assertTrue(context.getBean(SceneStorage.class) instanceof MemorySceneStorage);
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
    void backsOffWhenUserProvidesFsmStorageAndMessagingFacadeBeans() {
        MemoryStorage customStorage = new MemoryStorage();
        MessagingFacade customMessaging = new MessagingFacade(request -> {
            throw new UnsupportedOperationException("custom");
        });
        SceneRegistry customRegistry = new InMemorySceneRegistry();
        SceneStorage customSceneStorage = new MemorySceneStorage();

        contextRunner
                .withBean(FSMStorage.class, () -> customStorage)
                .withBean(MessagingFacade.class, () -> customMessaging)
                .withBean(SceneRegistry.class, () -> customRegistry)
                .withBean(SceneStorage.class, () -> customSceneStorage)
                .run(context -> {
                    assertSame(customStorage, context.getBean(FSMStorage.class));
                    assertSame(customMessaging, context.getBean(MessagingFacade.class));
                    assertSame(customRegistry, context.getBean(SceneRegistry.class));
                    assertSame(customSceneStorage, context.getBean(SceneStorage.class));
                });
    }

    @Test
    void createsMediaMessagingFacadeWhenUploadServiceBeanExists() {
        UploadService uploadService = new UploadService() {
            @Override
            public java.util.concurrent.CompletionStage<UploadResult> upload(InputFile inputFile, UploadRequest request) {
                throw new UnsupportedOperationException("not used in wiring test");
            }
        };

        contextRunner
                .withBean(UploadService.class, () -> uploadService)
                .run(context -> {
                    assertTrue(context.hasSingleBean(MediaMessagingFacade.class));
                    assertSame(uploadService, context.getBean(UploadService.class));
                });
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
                .withBean(ru.tardyon.botframework.ingestion.WebhookReceiver.class, () -> request -> {
                    assertNotNull(request);
                    return CompletableFuture.completedFuture(
                            ru.tardyon.botframework.ingestion.WebhookReceiveResult.accepted(null)
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
