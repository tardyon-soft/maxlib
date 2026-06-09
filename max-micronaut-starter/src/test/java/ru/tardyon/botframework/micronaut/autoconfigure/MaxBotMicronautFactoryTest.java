package ru.tardyon.botframework.micronaut.autoconfigure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Order;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import ru.tardyon.botframework.action.ChatActionsFacade;
import ru.tardyon.botframework.callback.CallbackFacade;
import ru.tardyon.botframework.client.MaxApiClientConfig;
import ru.tardyon.botframework.client.MaxBotClient;
import ru.tardyon.botframework.client.MaxRequest;
import ru.tardyon.botframework.client.http.MaxHttpClient;
import ru.tardyon.botframework.dispatcher.DispatchStatus;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.dispatcher.RuntimeDataKey;
import ru.tardyon.botframework.fsm.FSMStorage;
import ru.tardyon.botframework.fsm.InMemorySceneRegistry;
import ru.tardyon.botframework.fsm.MemorySceneStorage;
import ru.tardyon.botframework.fsm.MemoryStorage;
import ru.tardyon.botframework.fsm.SceneRegistry;
import ru.tardyon.botframework.fsm.SceneStorage;
import ru.tardyon.botframework.fsm.StateScope;
import ru.tardyon.botframework.message.MediaMessagingFacade;
import ru.tardyon.botframework.message.MessagingFacade;
import ru.tardyon.botframework.micronaut.properties.MaxBotProperties;
import ru.tardyon.botframework.micronaut.properties.MaxBotStorageType;
import ru.tardyon.botframework.micronaut.storage.RedisFSMStorage;
import ru.tardyon.botframework.micronaut.webhook.MicronautWebhookAdapter;
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
import ru.tardyon.botframework.screen.LegacyStringScreenActionCodec;
import ru.tardyon.botframework.screen.ScreenActionCodec;
import ru.tardyon.botframework.screen.ScreenRegistry;
import ru.tardyon.botframework.screen.TypedV1ScreenActionCodec;
import ru.tardyon.botframework.screen.WidgetActionDispatcher;
import ru.tardyon.botframework.screen.WidgetViewResolver;
import ru.tardyon.botframework.upload.InputFile;
import ru.tardyon.botframework.upload.UploadRequest;
import ru.tardyon.botframework.upload.UploadResult;
import ru.tardyon.botframework.upload.UploadService;

class MaxBotMicronautFactoryTest {

    @Test
    void autoConfigCreatesCoreSdkAndRuntimeBeans() {
        try (ApplicationContext context = context("base", Map.of())) {
            assertTrue(context.containsBean(MaxBotProperties.class));
            assertTrue(context.containsBean(MaxApiClientConfig.class));
            assertTrue(context.containsBean(MaxHttpClient.class));
            assertTrue(context.containsBean(MaxBotClient.class));
            assertTrue(context.containsBean(FSMStorage.class));
            assertTrue(context.containsBean(SceneRegistry.class));
            assertTrue(context.containsBean(SceneStorage.class));
            assertTrue(context.containsBean(ScreenRegistry.class));
            assertTrue(context.containsBean(ScreenActionCodec.class));
            assertTrue(context.containsBean(WidgetViewResolver.class));
            assertTrue(context.containsBean(WidgetActionDispatcher.class));
            assertTrue(context.containsBean(MessagingFacade.class));
            assertTrue(context.containsBean(CallbackFacade.class));
            assertTrue(context.containsBean(ChatActionsFacade.class));
            assertTrue(context.containsBean(Dispatcher.class));

            assertFalse(context.containsBean(MediaMessagingFacade.class));
            assertEquals(MaxBotStorageType.MEMORY, context.getBean(MaxBotProperties.class).getStorage().getType());
            assertTrue(context.getBean(FSMStorage.class) instanceof MemoryStorage);
            assertTrue(context.getBean(SceneRegistry.class) instanceof InMemorySceneRegistry);
            assertTrue(context.getBean(SceneStorage.class) instanceof MemorySceneStorage);
            assertTrue(context.getBean(ScreenActionCodec.class) instanceof LegacyStringScreenActionCodec);
        }
    }

    @Test
    void dispatcherIncludesRouterBeans() {
        try (ApplicationContext context = context("routers", Map.of())) {
            Dispatcher dispatcher = context.getBean(Dispatcher.class);
            List<Router> routers = context.getBeansOfType(Router.class).stream().toList();

            assertEquals(2, dispatcher.routers().size());
            assertEquals(2, routers.size());
            assertSame(routers.get(0), dispatcher.routers().get(0));
            assertSame(routers.get(1), dispatcher.routers().get(1));
        }
    }

    @Test
    void backsOffWhenUserProvidesDispatcherBean() {
        try (ApplicationContext context = context("custom-dispatcher", Map.of())) {
            assertSame(CustomDispatcherFactory.CUSTOM_DISPATCHER, context.getBean(Dispatcher.class));
        }
    }

    @Test
    void backsOffWhenUserProvidesMaxBotClientBean() {
        try (ApplicationContext context = context("custom-client", Map.of())) {
            assertSame(CustomClientFactory.CUSTOM_CLIENT, context.getBean(MaxBotClient.class));
        }
    }

    @Test
    void backsOffWhenUserProvidesFsmStorageAndMessagingFacadeBeans() {
        try (ApplicationContext context = context("custom-runtime", Map.of())) {
            assertSame(CustomRuntimeFactory.CUSTOM_STORAGE, context.getBean(FSMStorage.class));
            assertSame(CustomRuntimeFactory.CUSTOM_MESSAGING, context.getBean(MessagingFacade.class));
            assertSame(CustomRuntimeFactory.CUSTOM_SCENE_REGISTRY, context.getBean(SceneRegistry.class));
            assertSame(CustomRuntimeFactory.CUSTOM_SCENE_STORAGE, context.getBean(SceneStorage.class));
        }
    }

    @Test
    void createsMediaMessagingFacadeWhenUploadServiceBeanExists() {
        try (ApplicationContext context = context("upload-service", Map.of())) {
            assertTrue(context.containsBean(MediaMessagingFacade.class));
            assertSame(UploadServiceFactory.UPLOAD_SERVICE, context.getBean(UploadService.class));
        }
    }

    @Test
    void appliesConfiguredStateScopeToRuntime() {
        AtomicReference<StateScope> seenScope = ScopeRouterFactory.SEEN_SCOPE;
        seenScope.set(null);

        try (ApplicationContext context = context("scope-router", Map.of(
                "max.bot.storage.state-scope", "CHAT"
        ))) {
            Dispatcher dispatcher = context.getBean(Dispatcher.class);
            var result = dispatcher.feedUpdate(sampleUpdate("hello")).toCompletableFuture().join();

            assertEquals(DispatchStatus.HANDLED, result.status());
            assertEquals(StateScope.CHAT, seenScope.get());
        }
    }

    @Test
    void createsRedisFsmStorageWhenConfigured() {
        try (ApplicationContext context = context("redis", Map.of(
                "max.bot.storage.type", "REDIS"
        ))) {
            assertTrue(context.containsBean(FSMStorage.class));
            assertTrue(context.getBean(FSMStorage.class) instanceof RedisFSMStorage);
            assertEquals(MaxBotStorageType.REDIS, context.getBean(MaxBotProperties.class).getStorage().getType());
        }
    }

    @Test
    void usesTypedV1ScreenActionCodecWhenConfigured() {
        try (ApplicationContext context = context("base", Map.of(
                "max.bot.screen.callback.codec.mode", "TYPED_V1"
        ))) {
            assertTrue(context.getBean(ScreenActionCodec.class) instanceof TypedV1ScreenActionCodec);
        }
    }

    @Test
    void typedCodecBeanIsWiredAndRegisteredInDispatcherServices() {
        try (ApplicationContext context = context("base", Map.of(
                "max.bot.screen.callback.codec.mode", "TYPED_V1"
        ))) {
            ScreenActionCodec screenActionCodec = context.getBean(ScreenActionCodec.class);
            Dispatcher dispatcher = context.getBean(Dispatcher.class);

            assertTrue(screenActionCodec instanceof TypedV1ScreenActionCodec);
            assertTrue(dispatcher.applicationData().containsKey(
                    RuntimeDataKey.application("service:" + ScreenActionCodec.class.getName(), ScreenActionCodec.class)
            ));
        }
    }

    @Test
    void createsWebhookAdapterWhenWebhookReceiverBeanExists() {
        try (ApplicationContext context = context("custom-webhook-receiver", Map.of(
                "max.bot.mode", "WEBHOOK"
        ))) {
            assertTrue(context.containsBean(MicronautWebhookAdapter.class));
        }
    }

    private static ApplicationContext context(String specName, Map<String, Object> extraProperties) {
        java.util.LinkedHashMap<String, Object> properties = new java.util.LinkedHashMap<>();
        properties.put("spec.name", specName);
        properties.put("max.bot.token", "test-token");
        properties.put("max.bot.route-component-scan.enabled", "false");
        properties.putAll(extraProperties);
        return ApplicationContext.builder()
                .properties(properties)
                .start();
    }

    private static Update sampleUpdate(String text) {
        return new Update(
                new UpdateId("u-micronaut-1"),
                UpdateType.MESSAGE,
                new Message(
                        new MessageId("m-micronaut-1"),
                        new Chat(new ChatId("c-micronaut-1"), ChatType.PRIVATE, "chat", null, null),
                        new User(new UserId("u-micronaut-1"), "demo", "Demo", "User", "Demo User", false, "en"),
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
    @Requires(property = "spec.name", value = "routers")
    static final class RouterFactory {
        @Singleton
        @Order(1)
        Router routerA() {
            return new Router("a");
        }

        @Singleton
        @Order(2)
        Router routerB() {
            return new Router("b");
        }
    }

    @Factory
    @Requires(property = "spec.name", value = "custom-dispatcher")
    static final class CustomDispatcherFactory {
        static final Dispatcher CUSTOM_DISPATCHER = new Dispatcher();

        @Singleton
        Dispatcher dispatcher() {
            return CUSTOM_DISPATCHER;
        }
    }

    @Factory
    @Requires(property = "spec.name", value = "custom-client")
    static final class CustomClientFactory {
        static final MaxBotClient CUSTOM_CLIENT = new MaxBotClient() {
            @Override
            public <T> T execute(MaxRequest<T> request) {
                throw new UnsupportedOperationException("custom");
            }
        };

        @Singleton
        MaxBotClient maxBotClient() {
            return CUSTOM_CLIENT;
        }
    }

    @Factory
    @Requires(property = "spec.name", value = "custom-runtime")
    static final class CustomRuntimeFactory {
        static final MemoryStorage CUSTOM_STORAGE = new MemoryStorage();
        static final MaxBotClient CUSTOM_CLIENT = new MaxBotClient() {
            @Override
            public <T> T execute(MaxRequest<T> request) {
                throw new UnsupportedOperationException("custom");
            }
        };
        static final MessagingFacade CUSTOM_MESSAGING = new MessagingFacade(CUSTOM_CLIENT);
        static final SceneRegistry CUSTOM_SCENE_REGISTRY = new InMemorySceneRegistry();
        static final SceneStorage CUSTOM_SCENE_STORAGE = new MemorySceneStorage();

        @Singleton
        FSMStorage fsmStorage() {
            return CUSTOM_STORAGE;
        }

        @Singleton
        MessagingFacade messagingFacade() {
            return CUSTOM_MESSAGING;
        }

        @Singleton
        SceneRegistry sceneRegistry() {
            return CUSTOM_SCENE_REGISTRY;
        }

        @Singleton
        SceneStorage sceneStorage() {
            return CUSTOM_SCENE_STORAGE;
        }
    }

    @Factory
    @Requires(property = "spec.name", value = "upload-service")
    static final class UploadServiceFactory {
        static final UploadService UPLOAD_SERVICE = new UploadService() {
            @Override
            public java.util.concurrent.CompletionStage<UploadResult> upload(InputFile inputFile, UploadRequest request) {
                throw new UnsupportedOperationException("not used in wiring test");
            }
        };

        @Singleton
        UploadService uploadService() {
            return UPLOAD_SERVICE;
        }
    }

    @Factory
    @Requires(property = "spec.name", value = "scope-router")
    static final class ScopeRouterFactory {
        static final AtomicReference<StateScope> SEEN_SCOPE = new AtomicReference<>();

        @Singleton
        Router scopeRouter() {
            Router router = new Router("scope");
            router.message((message, fsm) -> {
                SEEN_SCOPE.set(fsm.fsm().scope().scope());
                return CompletableFuture.completedFuture(null);
            });
            return router;
        }
    }

    @Factory
    @Requires(property = "spec.name", value = "redis")
    static final class RedisFactory {
        @Singleton
        RedisConnectionFactory redisConnectionFactory() {
            return Mockito.mock(RedisConnectionFactory.class);
        }
    }

    @Factory
    @Requires(property = "spec.name", value = "custom-webhook-receiver")
    static final class CustomWebhookReceiverFactory {
        @Singleton
        ru.tardyon.botframework.ingestion.WebhookReceiver webhookReceiver() {
            return request -> CompletableFuture.completedFuture(
                    ru.tardyon.botframework.ingestion.WebhookReceiveResult.accepted(null)
            );
        }
    }
}
