package ru.tardyon.botframework.micronaut.autoconfigure;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.BeanProvider;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.type.Argument;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.util.List;
import okhttp3.OkHttpClient;
import ru.tardyon.botframework.action.ChatActionsFacade;
import ru.tardyon.botframework.callback.CallbackFacade;
import ru.tardyon.botframework.client.DefaultMaxBotClient;
import ru.tardyon.botframework.client.MaxApiClientConfig;
import ru.tardyon.botframework.client.MaxBotClient;
import ru.tardyon.botframework.client.http.MaxHttpClient;
import ru.tardyon.botframework.client.http.okhttp.OkHttpMaxHttpClient;
import ru.tardyon.botframework.client.serialization.JacksonJsonCodec;
import ru.tardyon.botframework.client.serialization.JsonCodec;
import ru.tardyon.botframework.dispatcher.AnnotatedRouteRegistrar;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.fsm.FSMStorage;
import ru.tardyon.botframework.fsm.InMemorySceneRegistry;
import ru.tardyon.botframework.fsm.MemorySceneStorage;
import ru.tardyon.botframework.fsm.MemoryStorage;
import ru.tardyon.botframework.fsm.SceneRegistry;
import ru.tardyon.botframework.fsm.SceneStorage;
import ru.tardyon.botframework.message.MediaMessagingFacade;
import ru.tardyon.botframework.message.MessageTarget;
import ru.tardyon.botframework.message.MessagingFacade;
import ru.tardyon.botframework.micronaut.properties.MaxBotProperties;
import ru.tardyon.botframework.micronaut.properties.MaxBotMode;
import ru.tardyon.botframework.micronaut.properties.MaxBotStorageType;
import ru.tardyon.botframework.micronaut.storage.RedisFSMStorage;
import ru.tardyon.botframework.micronaut.widget.AnnotatedWidgetRegistry;
import ru.tardyon.botframework.screen.AnnotatedScreenRegistrar;
import ru.tardyon.botframework.screen.InMemoryScreenRegistry;
import ru.tardyon.botframework.screen.LegacyStringScreenActionCodec;
import ru.tardyon.botframework.screen.ScreenActionCodec;
import ru.tardyon.botframework.screen.ScreenMiddleware;
import ru.tardyon.botframework.screen.ScreenRegistry;
import ru.tardyon.botframework.screen.Screens;
import ru.tardyon.botframework.screen.TypedV1ScreenActionCodec;
import ru.tardyon.botframework.screen.WidgetActionDispatcher;
import ru.tardyon.botframework.screen.WidgetViewResolver;
import ru.tardyon.botframework.upload.UploadService;

/**
 * Baseline Micronaut wiring contract for MAX starter runtime.
 */
@Factory
public final class MaxBotMicronautFactory {
    @Singleton
    MaxBotProperties maxBotPropertiesMax(ApplicationContext applicationContext) {
        MaxBotProperties properties = new MaxBotProperties();
        String token = applicationContext.getProperty("max.bot.token", String.class).orElse(null);
        properties.setToken(token);
        applicationContext.getProperty("max.bot.base-url", String.class).ifPresent(properties::setBaseUrl);
        applicationContext.getProperty("max.bot.mode", MaxBotMode.class).ifPresent(properties::setMode);

        MaxBotProperties.Polling polling = new MaxBotProperties.Polling();
        applicationContext.getProperty("max.bot.polling.enabled", Boolean.class).ifPresent(polling::setEnabled);
        applicationContext.getProperty("max.bot.polling.limit", Integer.class).ifPresent(polling::setLimit);
        applicationContext.getProperty("max.bot.polling.timeout", Duration.class).ifPresent(polling::setTimeout);
        applicationContext.getProperty("max.bot.polling.types", Argument.listOf(ru.tardyon.botframework.model.UpdateEventType.class))
                .ifPresent(values -> polling.getTypes().addAll(values));
        properties.setPolling(polling);

        MaxBotProperties.Webhook webhook = new MaxBotProperties.Webhook();
        applicationContext.getProperty("max.bot.webhook.enabled", Boolean.class).ifPresent(webhook::setEnabled);
        applicationContext.getProperty("max.bot.webhook.path", String.class).ifPresent(webhook::setPath);
        applicationContext.getProperty("max.bot.webhook.secret", String.class).ifPresent(webhook::setSecret);
        applicationContext.getProperty("max.bot.webhook.max-in-flight", Integer.class).ifPresent(webhook::setMaxInFlight);
        properties.setWebhook(webhook);

        MaxBotProperties.Storage storage = new MaxBotProperties.Storage();
        applicationContext.getProperty("max.bot.storage.type", MaxBotStorageType.class).ifPresent(storage::setType);
        applicationContext.getProperty("max.bot.storage.state-scope", ru.tardyon.botframework.fsm.StateScope.class).ifPresent(storage::setStateScope);
        MaxBotProperties.Redis redis = new MaxBotProperties.Redis();
        applicationContext.getProperty("max.bot.storage.redis.key-prefix", String.class).ifPresent(redis::setKeyPrefix);
        applicationContext.getProperty("max.bot.storage.redis.ttl", Duration.class).ifPresent(redis::setTtl);
        storage.setRedis(redis);
        properties.setStorage(storage);

        MaxBotProperties.Screen screen = new MaxBotProperties.Screen();
        applicationContext.getProperty("max.bot.screen.namespace", String.class).ifPresent(screen::setNamespace);
        MaxBotProperties.Callback callback = new MaxBotProperties.Callback();
        MaxBotProperties.Codec codec = new MaxBotProperties.Codec();
        applicationContext.getProperty("max.bot.screen.callback.codec.mode", ru.tardyon.botframework.screen.ScreenActionCodecMode.class).ifPresent(codec::setMode);
        callback.setCodec(codec);
        screen.setCallback(callback);
        properties.setScreen(screen);

        validate(properties);
        return properties;
    }

    @Singleton
    @Requires(missingBeans = MaxApiClientConfig.class)
    MaxApiClientConfig maxApiClientConfigMax(MaxBotProperties properties) {
        return MaxApiClientConfig.builder()
                .token(properties.getToken())
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    @Singleton
    @Requires(missingBeans = OkHttpClient.class)
    OkHttpClient okHttpClientMax(MaxApiClientConfig config) {
        return new OkHttpClient.Builder()
                .connectTimeout(config.connectTimeout())
                .readTimeout(config.readTimeout())
                .callTimeout(Duration.ZERO)
                .build();
    }

    @Singleton
    @Requires(missingBeans = MaxHttpClient.class)
    MaxHttpClient maxHttpClientMax(MaxApiClientConfig config, OkHttpClient okHttpClient) {
        return new OkHttpMaxHttpClient(config.baseUri(), okHttpClient);
    }

    @Singleton
    @Requires(missingBeans = JsonCodec.class)
    JsonCodec maxJsonCodecMax() {
        return new JacksonJsonCodec();
    }

    @Singleton
    @Requires(missingBeans = AnnotatedRouteRegistrar.class)
    AnnotatedRouteRegistrar annotatedRouteRegistrarMax(ApplicationContext applicationContext) {
        return new AnnotatedRouteRegistrar(new AnnotatedRouteRegistrar.ComponentResolver() {
            @Override
            public <T> T resolve(Class<T> type) {
                return applicationContext.findBean(type).orElseGet(() -> {
                    try {
                        var constructor = type.getDeclaredConstructor();
                        if (!constructor.canAccess(null)) {
                            constructor.setAccessible(true);
                        }
                        return constructor.newInstance();
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException("Failed to resolve " + type.getName(), e);
                    }
                });
            }
        });
    }

    @Singleton
    @Requires(missingBeans = MaxBotClient.class)
    MaxBotClient maxBotClientMax(
            MaxApiClientConfig config,
            MaxHttpClient maxHttpClient,
            JsonCodec jsonCodec
    ) {
        return new DefaultMaxBotClient(config, maxHttpClient, jsonCodec);
    }

    @Singleton
    @Requires(missingBeans = FSMStorage.class)
    FSMStorage fsmStorageMax(
            MaxBotProperties properties,
            ApplicationContext applicationContext,
            JsonCodec jsonCodec
    ) {
        if (properties.getStorage().getType() == MaxBotStorageType.MEMORY) {
            return new MemoryStorage();
        }
        if (properties.getStorage().getType() == MaxBotStorageType.REDIS) {
            return createRedisStorageReflective(applicationContext, jsonCodec, properties);
        }
        throw new IllegalStateException("Unsupported storage type: " + properties.getStorage().getType());
    }

    @Singleton
    @Requires(missingBeans = SceneRegistry.class)
    SceneRegistry sceneRegistryMax() {
        return new InMemorySceneRegistry();
    }

    @Singleton
    @Requires(missingBeans = SceneStorage.class)
    SceneStorage sceneStorageMax() {
        return new MemorySceneStorage();
    }

    @Singleton
    @Context
    @Requires(missingBeans = ScreenRegistry.class)
    ScreenRegistry screenRegistryMax() {
        return new InMemoryScreenRegistry();
    }

    @Singleton
    @Requires(missingBeans = ScreenActionCodec.class)
    ScreenActionCodec screenActionCodecMax(MaxBotProperties properties) {
        return switch (properties.getScreen().getCallback().getCodec().getMode()) {
            case LEGACY_STRING -> new LegacyStringScreenActionCodec();
            case TYPED_V1 -> new TypedV1ScreenActionCodec();
        };
    }

    @Singleton
    @Context
    @Requires(missingBeans = AnnotatedWidgetRegistry.class)
    AnnotatedWidgetRegistry annotatedWidgetRegistryMax() {
        return new AnnotatedWidgetRegistry();
    }

    @Singleton
    @Requires(missingBeans = MicronautWidgetBeanRegistrar.class)
    MicronautWidgetBeanRegistrar micronautWidgetBeanRegistrarMax(ApplicationContext applicationContext) {
        return new MicronautWidgetBeanRegistrar(applicationContext);
    }

    @Singleton
    @Requires(missingBeans = AnnotatedScreenRegistrar.class)
    AnnotatedScreenRegistrar annotatedScreenRegistrarMax() {
        return new AnnotatedScreenRegistrar();
    }

    @Singleton
    @Requires(missingBeans = MicronautScreenControllerRegistrar.class)
    MicronautScreenControllerRegistrar micronautScreenControllerRegistrarMax() {
        return new MicronautScreenControllerRegistrar();
    }

    @Singleton
    @Requires(missingBeans = MessagingFacade.class)
    MessagingFacade messagingFacadeMax(
            MaxBotClient maxBotClient,
            BeanProvider<MessageTarget.UserChatResolver> userChatResolverProvider
    ) {
        MessageTarget.UserChatResolver resolver = uniqueOrNull(userChatResolverProvider);
        if (resolver == null) {
            return new MessagingFacade(maxBotClient);
        }
        return new MessagingFacade(maxBotClient, resolver);
    }

    @Singleton
    @Requires(missingBeans = CallbackFacade.class)
    CallbackFacade callbackFacadeMax(MaxBotClient maxBotClient) {
        return new CallbackFacade(maxBotClient);
    }

    @Singleton
    @Requires(missingBeans = ChatActionsFacade.class)
    ChatActionsFacade chatActionsFacadeMax(MaxBotClient maxBotClient) {
        return new ChatActionsFacade(maxBotClient);
    }

    @Singleton
    @Requires(beans = {UploadService.class, MessagingFacade.class})
    @Requires(missingBeans = MediaMessagingFacade.class)
    MediaMessagingFacade mediaMessagingFacadeMax(UploadService uploadService, MessagingFacade messagingFacade) {
        return new MediaMessagingFacade(uploadService, messagingFacade);
    }

    @Singleton
    @Requires(missingBeans = Dispatcher.class)
    Dispatcher dispatcherMax(
            MaxBotProperties properties,
            MaxBotClient maxBotClient,
            FSMStorage fsmStorage,
            BeanProvider<UploadService> uploadServiceProvider,
            BeanProvider<SceneRegistry> sceneRegistryProvider,
            BeanProvider<SceneStorage> sceneStorageProvider,
            BeanProvider<ScreenRegistry> screenRegistryProvider,
            BeanProvider<ScreenActionCodec> screenActionCodecProvider,
            BeanProvider<WidgetViewResolver> widgetViewResolverProvider,
            BeanProvider<WidgetActionDispatcher> widgetActionDispatcherProvider,
            BeanProvider<Router> routerProvider
    ) {
        Dispatcher dispatcher = new Dispatcher()
                .withBotClient(maxBotClient)
                .withFsmStorage(fsmStorage)
                .withStateScope(properties.getStorage().getStateScope());

        UploadService uploadService = uniqueOrNull(uploadServiceProvider);
        if (uploadService != null) {
            dispatcher.withUploadService(uploadService);
        }
        SceneRegistry sceneRegistry = uniqueOrNull(sceneRegistryProvider);
        if (sceneRegistry != null) {
            dispatcher.withSceneRegistry(sceneRegistry);
        }
        SceneStorage sceneStorage = uniqueOrNull(sceneStorageProvider);
        if (sceneStorage != null) {
            dispatcher.withSceneStorage(sceneStorage);
        }
        ScreenRegistry screenRegistry = uniqueOrNull(screenRegistryProvider);
        if (screenRegistry != null) {
            dispatcher.registerService(ScreenRegistry.class, screenRegistry);
        }
        ScreenActionCodec screenActionCodec = uniqueOrNull(screenActionCodecProvider);
        if (screenActionCodec != null) {
            dispatcher.registerService(ScreenActionCodec.class, screenActionCodec);
        }
        if (screenRegistry != null) {
            dispatcher.outerMiddleware(new ScreenMiddleware(
                    screenRegistry,
                    screenActionCodec == null ? new LegacyStringScreenActionCodec() : screenActionCodec
            ));
        }
        dispatcher.registerApplicationData(Screens.SCREEN_FSM_NAMESPACE_KEY, properties.getScreen().getNamespace());
        WidgetViewResolver widgetViewResolver = uniqueOrNull(widgetViewResolverProvider);
        if (widgetViewResolver != null) {
            dispatcher.registerService(WidgetViewResolver.class, widgetViewResolver);
        }
        WidgetActionDispatcher widgetActionDispatcher = uniqueOrNull(widgetActionDispatcherProvider);
        if (widgetActionDispatcher != null) {
            dispatcher.registerService(WidgetActionDispatcher.class, widgetActionDispatcher);
        }
        for (Router router : routerProvider.stream().toList()) {
            dispatcher.includeRouter(router);
        }
        return dispatcher;
    }

    private static FSMStorage createRedisStorageReflective(
            ApplicationContext applicationContext,
            JsonCodec jsonCodec,
            MaxBotProperties properties
    ) {
        try {
            ClassLoader classLoader = MaxBotMicronautFactory.class.getClassLoader();
            Class<?> connectionClass = Class.forName(
                    "io.lettuce.core.api.StatefulRedisConnection",
                    true,
                    classLoader
            );
            Class<?> storageClass = Class.forName(
                    "ru.tardyon.botframework.micronaut.storage.RedisFSMStorage",
                    true,
                    classLoader
            );

            Object connection = findBean(applicationContext, connectionClass);
            if (connection == null) {
                throw new IllegalStateException(
                        "Redis storage is configured but StatefulRedisConnection is missing. "
                                + "Add dependency: io.micronaut.redis:micronaut-redis-lettuce and Redis connection properties."
                );
            }

            return (FSMStorage) storageClass
                    .getConstructor(connectionClass, JsonCodec.class, String.class, Duration.class)
                    .newInstance(
                            connection,
                            jsonCodec,
                            properties.getStorage().getRedis().getKeyPrefix(),
                            properties.getStorage().getRedis().getTtl()
                    );
        } catch (IllegalStateException e) {
            throw e;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "Redis storage is configured but micronaut-redis-lettuce is not on classpath. "
                            + "Add dependency: io.micronaut.redis:micronaut-redis-lettuce",
                    e
            );
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to initialize Redis FSM storage", e);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object findBean(ApplicationContext applicationContext, Class<?> beanType) {
        return applicationContext.findBean((Class) beanType).orElse(null);
    }

    private static <T> T uniqueOrNull(BeanProvider<T> provider) {
        return provider.isPresent() ? provider.get() : null;
    }

    private static void validate(MaxBotProperties properties) {
        requireNotBlank(properties.getToken(), "token");
        requireNotBlank(properties.getBaseUrl(), "baseUrl");
        requireNotNull(properties.getMode(), "mode");
        requireNotNull(properties.getPolling(), "polling");
        if (properties.getPolling().getLimit() != null && properties.getPolling().getLimit() <= 0) {
            throw new IllegalStateException("polling.limit must be positive");
        }
        requireNotNull(properties.getPolling().getTimeout(), "polling.timeout");
        requireNotNull(properties.getWebhook(), "webhook");
        requireNotBlank(properties.getWebhook().getPath(), "webhook.path");
        if (properties.getWebhook().getMaxInFlight() != null && properties.getWebhook().getMaxInFlight() <= 0) {
            throw new IllegalStateException("webhook.maxInFlight must be positive");
        }
        requireNotNull(properties.getStorage(), "storage");
        requireNotNull(properties.getStorage().getType(), "storage.type");
        requireNotNull(properties.getStorage().getStateScope(), "storage.stateScope");
        requireNotNull(properties.getStorage().getRedis(), "storage.redis");
        requireNotBlank(properties.getStorage().getRedis().getKeyPrefix(), "storage.redis.keyPrefix");
        requireNotNull(properties.getScreen(), "screen");
        requireNotBlank(properties.getScreen().getNamespace(), "screen.namespace");
        requireNotNull(properties.getScreen().getCallback(), "screen.callback");
        requireNotNull(properties.getScreen().getCallback().getCodec(), "screen.callback.codec");
        requireNotNull(properties.getScreen().getCallback().getCodec().getMode(), "screen.callback.codec.mode");
    }

    private static void requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(field + " must not be blank");
        }
    }

    private static void requireNotNull(Object value, String field) {
        if (value == null) {
            throw new IllegalStateException(field + " must not be null");
        }
    }
}
