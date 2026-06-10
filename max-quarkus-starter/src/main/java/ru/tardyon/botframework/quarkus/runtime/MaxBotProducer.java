package ru.tardyon.botframework.quarkus.runtime;

import io.quarkus.arc.All;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.util.List;
import org.eclipse.microprofile.config.Config;
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
import ru.tardyon.botframework.ingestion.DefaultWebhookReceiver;
import ru.tardyon.botframework.ingestion.DefaultWebhookSecretValidator;
import ru.tardyon.botframework.fsm.FSMStorage;
import ru.tardyon.botframework.fsm.InMemorySceneRegistry;
import ru.tardyon.botframework.fsm.MemorySceneStorage;
import ru.tardyon.botframework.fsm.MemoryStorage;
import ru.tardyon.botframework.fsm.SceneRegistry;
import ru.tardyon.botframework.fsm.SceneStorage;
import ru.tardyon.botframework.fsm.StateScope;
import ru.tardyon.botframework.message.MediaMessagingFacade;
import ru.tardyon.botframework.message.MessageTarget;
import ru.tardyon.botframework.message.MessagingFacade;
import ru.tardyon.botframework.ingestion.WebhookReceiver;
import ru.tardyon.botframework.ingestion.WebhookReceiverConfig;
import ru.tardyon.botframework.ingestion.WebhookSecretValidator;
import ru.tardyon.botframework.quarkus.properties.MaxBotStorageType;
import ru.tardyon.botframework.quarkus.screen.QuarkusScreenAutoRegistrationBootstrap;
import ru.tardyon.botframework.quarkus.screen.QuarkusScreenControllerRegistrar;
import ru.tardyon.botframework.quarkus.route.QuarkusRouteAutoRegistrationBootstrap;
import ru.tardyon.botframework.quarkus.webhook.QuarkusWebhookAdapter;
import ru.tardyon.botframework.quarkus.widget.AnnotatedWidgetRegistry;
import ru.tardyon.botframework.quarkus.widget.QuarkusWidgetAutoRegistrationBootstrap;
import ru.tardyon.botframework.quarkus.storage.RedisFSMStorage;
import ru.tardyon.botframework.screen.InMemoryScreenRegistry;
import ru.tardyon.botframework.screen.AnnotatedScreenRegistrar;
import ru.tardyon.botframework.screen.LegacyStringScreenActionCodec;
import ru.tardyon.botframework.screen.ScreenActionCodec;
import ru.tardyon.botframework.screen.ScreenMiddleware;
import ru.tardyon.botframework.screen.ScreenRegistry;
import ru.tardyon.botframework.screen.Screens;
import ru.tardyon.botframework.screen.WidgetActionDispatcher;
import ru.tardyon.botframework.screen.WidgetViewResolver;
import ru.tardyon.botframework.screen.TypedV1ScreenActionCodec;
import ru.tardyon.botframework.upload.UploadService;

/**
 * Default CDI wiring for MAX Quarkus starter.
 */
@ApplicationScoped
public class MaxBotProducer {
    @Inject
    Config config;

    @Produces
    @Singleton
    @DefaultBean
    MaxApiClientConfig maxApiClientConfig() {
        return MaxApiClientConfig.builder()
                .token(config.getValue("max.bot.token", String.class))
                .baseUrl(config.getOptionalValue("max.bot.base-url", String.class).orElse("https://platform-api.max.ru"))
                .build();
    }

    @Produces
    @Singleton
    @DefaultBean
    OkHttpClient okHttpClient(MaxApiClientConfig config) {
        return new OkHttpClient.Builder()
                .connectTimeout(config.connectTimeout())
                .readTimeout(config.readTimeout())
                .callTimeout(Duration.ZERO)
                .build();
    }

    @Produces
    @Singleton
    @DefaultBean
    MaxHttpClient maxHttpClient(MaxApiClientConfig config, OkHttpClient okHttpClient) {
        return new OkHttpMaxHttpClient(config.baseUri(), okHttpClient);
    }

    @Produces
    @Singleton
    @DefaultBean
    JsonCodec jsonCodec() {
        return new JacksonJsonCodec();
    }

    @Produces
    @Singleton
    @DefaultBean
    MaxBotClient maxBotClient(MaxApiClientConfig config, MaxHttpClient maxHttpClient, JsonCodec jsonCodec) {
        return new DefaultMaxBotClient(config, maxHttpClient, jsonCodec);
    }

    @Produces
    @Singleton
    @DefaultBean
    AnnotatedRouteRegistrar annotatedRouteRegistrar(Instance<Object> beanLookup) {
        return new AnnotatedRouteRegistrar(new AnnotatedRouteRegistrar.ComponentResolver() {
            @Override
            public <T> T resolve(Class<T> type) {
                Instance<T> selection = beanLookup.select(type);
                if (selection.isResolvable()) {
                    return selection.get();
                }
                try {
                    var constructor = type.getDeclaredConstructor();
                    if (!constructor.canAccess(null)) {
                        constructor.setAccessible(true);
                    }
                    return constructor.newInstance();
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Failed to resolve route dependency: " + type.getName(), e);
                }
            }
        });
    }

    @Produces
    @Singleton
    @DefaultBean
    AnnotatedScreenRegistrar annotatedScreenRegistrar() {
        return new AnnotatedScreenRegistrar();
    }

    @Produces
    @Singleton
    @DefaultBean
    QuarkusScreenControllerRegistrar quarkusScreenControllerRegistrar() {
        return new QuarkusScreenControllerRegistrar();
    }

    @Produces
    @Singleton
    @DefaultBean
    AnnotatedWidgetRegistry annotatedWidgetRegistry() {
        return new AnnotatedWidgetRegistry();
    }

    @Produces
    @Singleton
    @DefaultBean
    QuarkusWidgetAutoRegistrationBootstrap quarkusWidgetAutoRegistrationBootstrap() {
        return new QuarkusWidgetAutoRegistrationBootstrap();
    }

    @Produces
    @Singleton
    @DefaultBean
    FSMStorage fsmStorage(
            JsonCodec jsonCodec,
            Instance<Object> redisTemplateProvider,
            Instance<Object> redisConnectionFactoryProvider
    ) {
        if (storageType() == MaxBotStorageType.REDIS) {
            return createRedisStorage(jsonCodec, redisTemplateProvider, redisConnectionFactoryProvider);
        }
        return new MemoryStorage();
    }

    @Produces
    @Singleton
    @DefaultBean
    SceneRegistry sceneRegistry() {
        return new InMemorySceneRegistry();
    }

    @Produces
    @Singleton
    @DefaultBean
    SceneStorage sceneStorage() {
        return new MemorySceneStorage();
    }

    @Produces
    @Singleton
    @DefaultBean
    ScreenRegistry screenRegistry() {
        return new InMemoryScreenRegistry();
    }

    @Produces
    @Singleton
    @DefaultBean
    ScreenActionCodec screenActionCodec() {
        return switch (config.getOptionalValue("max.bot.screen.callback.codec.mode", ru.tardyon.botframework.screen.ScreenActionCodecMode.class)
                .orElse(ru.tardyon.botframework.screen.ScreenActionCodecMode.LEGACY_STRING)) {
            case LEGACY_STRING -> new LegacyStringScreenActionCodec();
            case TYPED_V1 -> new TypedV1ScreenActionCodec();
        };
    }

    @Produces
    @Singleton
    @DefaultBean
    MessagingFacade messagingFacade(
            MaxBotClient maxBotClient,
            Instance<MessageTarget.UserChatResolver> userChatResolverProvider
    ) {
        if (userChatResolverProvider.isResolvable()) {
            return new MessagingFacade(maxBotClient, userChatResolverProvider.get());
        }
        return new MessagingFacade(maxBotClient);
    }

    @Produces
    @Singleton
    @DefaultBean
    CallbackFacade callbackFacade(MaxBotClient maxBotClient) {
        return new CallbackFacade(maxBotClient);
    }

    @Produces
    @Singleton
    @DefaultBean
    ChatActionsFacade chatActionsFacade(MaxBotClient maxBotClient) {
        return new ChatActionsFacade(maxBotClient);
    }

    @Produces
    @Singleton
    @DefaultBean
    MediaMessagingFacade mediaMessagingFacade(Instance<UploadService> uploadServiceProvider, MessagingFacade messagingFacade) {
        if (!uploadServiceProvider.isResolvable()) {
            return null;
        }
        return new MediaMessagingFacade(uploadServiceProvider.get(), messagingFacade);
    }

    @Produces
    @Singleton
    @DefaultBean
    WebhookSecretValidator webhookSecretValidator() {
        if (!webhookEnabled()) {
            return null;
        }
        return new DefaultWebhookSecretValidator(config.getOptionalValue("max.bot.webhook.secret", String.class).orElse(null));
    }

    @Produces
    @Singleton
    @DefaultBean
    WebhookReceiverConfig webhookReceiverConfig() {
        if (!webhookEnabled()) {
            return null;
        }
        Integer maxInFlight = config.getOptionalValue("max.bot.webhook.max-in-flight", Integer.class).orElse(null);
        if (maxInFlight == null) {
            return WebhookReceiverConfig.defaults();
        }
        return new WebhookReceiverConfig(maxInFlight);
    }

    @Produces
    @Singleton
    @DefaultBean
    WebhookReceiver webhookReceiver(
            WebhookSecretValidator secretValidator,
            JsonCodec jsonCodec,
            Dispatcher dispatcher,
            WebhookReceiverConfig config
    ) {
        if (secretValidator == null || config == null || !webhookEnabled()) {
            return null;
        }
        return new DefaultWebhookReceiver(secretValidator, jsonCodec, dispatcher, config);
    }

    @Produces
    @Singleton
    @DefaultBean
    QuarkusWebhookAdapter quarkusWebhookAdapter(WebhookReceiver webhookReceiver) {
        if (webhookReceiver == null || !webhookEnabled()) {
            return null;
        }
        return new QuarkusWebhookAdapter(webhookReceiver);
    }

    @Produces
    @Singleton
    @DefaultBean
    Dispatcher dispatcher(
            MaxBotClient maxBotClient,
            FSMStorage fsmStorage,
            SceneRegistry sceneRegistry,
            SceneStorage sceneStorage,
            ScreenRegistry screenRegistry,
            ScreenActionCodec screenActionCodec,
            AnnotatedWidgetRegistry widgetRegistry,
            Instance<Object> beanLookup,
            Config config,
            @All List<Router> routers,
            Instance<UploadService> uploadServiceProvider,
            QuarkusScreenAutoRegistrationBootstrap screenBootstrap,
            QuarkusWidgetAutoRegistrationBootstrap widgetBootstrap,
            QuarkusRouteAutoRegistrationBootstrap routeBootstrap
    ) {
        Dispatcher dispatcher = new Dispatcher()
                .withBotClient(maxBotClient)
                .withFsmStorage(fsmStorage)
                .withStateScope(storageStateScope())
                .withSceneRegistry(sceneRegistry)
                .withSceneStorage(sceneStorage);

        dispatcher.registerService(ScreenRegistry.class, screenRegistry);
        dispatcher.registerService(ScreenActionCodec.class, screenActionCodec);
        dispatcher.registerService(WidgetViewResolver.class, widgetRegistry);
        dispatcher.registerService(WidgetActionDispatcher.class, widgetRegistry);
        dispatcher.outerMiddleware(new ScreenMiddleware(screenRegistry, screenActionCodec));
        dispatcher.registerApplicationData(Screens.SCREEN_FSM_NAMESPACE_KEY, screenNamespace());
        screenBootstrap.registerScreens(screenRegistry);
        widgetBootstrap.registerWidgets(widgetRegistry, beanLookup, config);

        if (uploadServiceProvider.isResolvable()) {
            dispatcher.withUploadService(uploadServiceProvider.get());
        }

        for (int i = routers.size() - 1; i >= 0; i--) {
            dispatcher.includeRouter(routers.get(i));
        }
        routeBootstrap.registerRoutes(dispatcher);
        return dispatcher;
    }

    private FSMStorage createRedisStorage(
            JsonCodec jsonCodec,
            Instance<Object> redisDataSourceProvider,
            Instance<Object> unusedRedisConnectionProvider
    ) {
        try {
            Class<?> dataSourceClass = Class.forName("io.quarkus.redis.datasource.RedisDataSource");
            Object dataSource = resolveRedisDataSource(redisDataSourceProvider, dataSourceClass);
            if (dataSource == null) {
                throw new IllegalStateException(
                        "Redis storage is configured but RedisDataSource is missing. "
                                + "Add quarkus-redis-client and Redis connection properties."
                );
            }
            Class<?> storageClass = Class.forName("ru.tardyon.botframework.quarkus.storage.RedisFSMStorage");
            return (FSMStorage) storageClass
                    .getConstructor(dataSourceClass, JsonCodec.class, String.class, Duration.class)
                    .newInstance(dataSource, jsonCodec, redisKeyPrefix(), redisTtl());
        } catch (IllegalStateException e) {
            throw e;
        } catch (LinkageError e) {
            throw new IllegalStateException(
                    "Redis storage is configured but quarkus-redis-client is not available on the runtime classpath",
                    e
            );
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "Redis storage is configured but quarkus-redis-client is not available on the runtime classpath",
                    e
            );
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to initialize Redis FSM storage", e);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object resolveRedisDataSource(
            Instance<Object> redisDataSourceProvider,
            Class<?> dataSourceClass
    ) {
        Instance<?> selection = redisDataSourceProvider.select((Class) dataSourceClass);
        if (!selection.isResolvable()) {
            return null;
        }
        return selection.get();
    }

    private MaxBotStorageType storageType() {
        return config.getOptionalValue("max.bot.storage.type", MaxBotStorageType.class)
                .orElse(MaxBotStorageType.MEMORY);
    }

    private StateScope storageStateScope() {
        return config.getOptionalValue("max.bot.storage.state-scope", StateScope.class)
                .orElse(StateScope.USER_IN_CHAT);
    }

    private String redisKeyPrefix() {
        return config.getOptionalValue("max.bot.storage.redis.key-prefix", String.class)
                .orElse("max:bot:fsm");
    }

    private Duration redisTtl() {
        return config.getOptionalValue("max.bot.storage.redis.ttl", Duration.class).orElse(null);
    }

    private String screenNamespace() {
        return config.getOptionalValue("max.bot.screen.namespace", String.class)
                .orElse("max.screen");
    }

    private boolean webhookEnabled() {
        String mode = config.getOptionalValue("max.bot.mode", String.class).orElse("POLLING");
        boolean enabled = config.getOptionalValue("max.bot.webhook.enabled", Boolean.class).orElse(false);
        return "WEBHOOK".equalsIgnoreCase(mode) || enabled;
    }
}
