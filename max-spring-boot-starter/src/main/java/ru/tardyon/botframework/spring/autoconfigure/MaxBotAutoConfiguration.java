package ru.tardyon.botframework.spring.autoconfigure;

import java.time.Duration;
import java.util.List;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
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
import ru.tardyon.botframework.action.ChatActionsFacade;
import ru.tardyon.botframework.callback.CallbackFacade;
import ru.tardyon.botframework.fsm.FSMStorage;
import ru.tardyon.botframework.fsm.InMemorySceneRegistry;
import ru.tardyon.botframework.fsm.MemoryStorage;
import ru.tardyon.botframework.fsm.MemorySceneStorage;
import ru.tardyon.botframework.fsm.SceneRegistry;
import ru.tardyon.botframework.fsm.SceneStorage;
import ru.tardyon.botframework.ingestion.DefaultLongPollingRunner;
import ru.tardyon.botframework.ingestion.DefaultWebhookReceiver;
import ru.tardyon.botframework.ingestion.DefaultWebhookSecretValidator;
import ru.tardyon.botframework.ingestion.LongPollingRunnerConfig;
import ru.tardyon.botframework.ingestion.PollingFetchRequest;
import ru.tardyon.botframework.ingestion.PollingUpdateSource;
import ru.tardyon.botframework.ingestion.SdkPollingUpdateSource;
import ru.tardyon.botframework.ingestion.LongPollingRunner;
import ru.tardyon.botframework.ingestion.WebhookReceiverConfig;
import ru.tardyon.botframework.ingestion.WebhookReceiver;
import ru.tardyon.botframework.ingestion.WebhookSecretValidator;
import ru.tardyon.botframework.spring.polling.SpringPollingBootstrap;
import ru.tardyon.botframework.spring.polling.SpringPollingLifecycle;
import ru.tardyon.botframework.spring.properties.MaxBotProperties;
import ru.tardyon.botframework.spring.properties.MaxBotStorageType;
import ru.tardyon.botframework.spring.webhook.SpringWebhookAdapter;
import ru.tardyon.botframework.spring.webhook.SpringWebhookController;
import ru.tardyon.botframework.upload.UploadService;
import ru.tardyon.botframework.message.MediaMessagingFacade;
import ru.tardyon.botframework.message.MessageTarget;
import ru.tardyon.botframework.message.MessagingFacade;

/**
 * Baseline auto-configuration contract for MAX Spring starter.
 */
@AutoConfiguration
@EnableConfigurationProperties(MaxBotProperties.class)
public class MaxBotAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger(MaxBotAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "max.bot.route-component-scan.enabled", havingValue = "true", matchIfMissing = true)
    public static RouteComponentAutoRegistrar routeComponentAutoRegistrar() {
        return new RouteComponentAutoRegistrar();
    }

    @Bean
    @ConditionalOnMissingBean
    public MaxApiClientConfig maxApiClientConfig(MaxBotProperties properties) {
        return MaxApiClientConfig.builder()
                .token(properties.getToken())
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public OkHttpClient okHttpClient(MaxApiClientConfig config) {
        return new OkHttpClient.Builder()
                .connectTimeout(config.connectTimeout())
                .readTimeout(config.readTimeout())
                .callTimeout(java.time.Duration.ZERO)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public MaxHttpClient maxHttpClient(MaxApiClientConfig config, OkHttpClient okHttpClient) {
        return new OkHttpMaxHttpClient(config.baseUri(), okHttpClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonCodec maxJsonCodec() {
        return new JacksonJsonCodec();
    }

    @Bean
    @ConditionalOnMissingBean
    public MaxBotClient maxBotClient(
            MaxApiClientConfig config,
            MaxHttpClient maxHttpClient,
            JsonCodec jsonCodec
    ) {
        return new DefaultMaxBotClient(config, maxHttpClient, jsonCodec);
    }

    @Bean
    @ConditionalOnMissingBean
    public FSMStorage fsmStorage(
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

    private static FSMStorage createRedisStorageReflective(
            ApplicationContext applicationContext,
            JsonCodec jsonCodec,
            MaxBotProperties properties
    ) {
        try {
            ClassLoader classLoader = MaxBotAutoConfiguration.class.getClassLoader();
            Class<?> connectionFactoryClass = Class.forName(
                    "org.springframework.data.redis.connection.RedisConnectionFactory",
                    true,
                    classLoader
            );
            Class<?> templateClass = Class.forName(
                    "org.springframework.data.redis.core.StringRedisTemplate",
                    true,
                    classLoader
            );
            Class<?> storageClass = Class.forName(
                    "ru.tardyon.botframework.spring.storage.RedisFSMStorage",
                    true,
                    classLoader
            );

            Object template = applicationContext.getBeanProvider(templateClass).getIfAvailable();
            if (template == null) {
                Object factory = applicationContext.getBeanProvider(connectionFactoryClass).getIfAvailable();
                if (factory == null) {
                    throw new IllegalStateException(
                            "Redis storage is configured but RedisConnectionFactory is missing. "
                                    + "Add spring-data-redis starter and redis connection properties."
                    );
                }
                template = templateClass.getConstructor(connectionFactoryClass).newInstance(factory);
            }

            return (FSMStorage) storageClass
                    .getConstructor(templateClass, JsonCodec.class, String.class, Duration.class)
                    .newInstance(
                            template,
                            jsonCodec,
                            properties.getStorage().getRedis().getKeyPrefix(),
                            properties.getStorage().getRedis().getTtl()
                    );
        } catch (IllegalStateException e) {
            throw e;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "Redis storage is configured but spring-data-redis is not on classpath. "
                            + "Add dependency: org.springframework.boot:spring-boot-starter-data-redis",
                    e
            );
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to initialize Redis FSM storage", e);
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public SceneRegistry sceneRegistry() {
        return new InMemorySceneRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public SceneStorage sceneStorage() {
        return new MemorySceneStorage();
    }

    @Bean
    @ConditionalOnMissingBean
    public MessagingFacade messagingFacade(
            MaxBotClient maxBotClient,
            ObjectProvider<MessageTarget.UserChatResolver> userChatResolverProvider
    ) {
        MessageTarget.UserChatResolver resolver = userChatResolverProvider.getIfAvailable();
        if (resolver == null) {
            return new MessagingFacade(maxBotClient);
        }
        return new MessagingFacade(maxBotClient, resolver);
    }

    @Bean
    @ConditionalOnMissingBean
    public CallbackFacade callbackFacade(MaxBotClient maxBotClient) {
        return new CallbackFacade(maxBotClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public ChatActionsFacade chatActionsFacade(MaxBotClient maxBotClient) {
        return new ChatActionsFacade(maxBotClient);
    }

    @Bean
    @ConditionalOnBean(UploadService.class)
    @ConditionalOnMissingBean
    public MediaMessagingFacade mediaMessagingFacade(UploadService uploadService, MessagingFacade messagingFacade) {
        return new MediaMessagingFacade(uploadService, messagingFacade);
    }

    @Bean
    @ConditionalOnMissingBean
    public Dispatcher dispatcher(
            MaxBotProperties properties,
            MaxBotClient maxBotClient,
            FSMStorage fsmStorage,
            ObjectProvider<UploadService> uploadServiceProvider,
            ObjectProvider<SceneRegistry> sceneRegistryProvider,
            ObjectProvider<SceneStorage> sceneStorageProvider,
            ObjectProvider<Router> routerProvider
    ) {
        log.debug("Creating Dispatcher bean: mode={}, baseUrl={}", properties.getMode(), properties.getBaseUrl());
        Dispatcher dispatcher = new Dispatcher()
                .withBotClient(maxBotClient)
                .withFsmStorage(fsmStorage)
                .withStateScope(properties.getStorage().getStateScope());

        UploadService uploadService = uploadServiceProvider.getIfAvailable();
        if (uploadService != null) {
            dispatcher.withUploadService(uploadService);
        }
        SceneRegistry sceneRegistry = sceneRegistryProvider.getIfAvailable();
        if (sceneRegistry != null) {
            dispatcher.withSceneRegistry(sceneRegistry);
        }
        SceneStorage sceneStorage = sceneStorageProvider.getIfAvailable();
        if (sceneStorage != null) {
            dispatcher.withSceneStorage(sceneStorage);
        }

        List<Router> routers = routerProvider.orderedStream().toList();
        log.debug("Registering Router beans into Dispatcher: count={}", routers.size());
        for (Router router : routers) {
            dispatcher.includeRouter(router);
        }
        log.debug("Dispatcher bean initialized: routers={}", dispatcher.routers().stream().map(Router::name).toList());
        return dispatcher;
    }

    @Bean
    @ConditionalOnMissingBean
    public AnnotatedRouteRegistrar annotatedRouteRegistrar(ApplicationContext applicationContext) {
        log.debug("Creating AnnotatedRouteRegistrar bean");
        return new AnnotatedRouteRegistrar(new AnnotatedRouteRegistrar.ComponentResolver() {
            @Override
            public <T> T resolve(Class<T> type) {
                return applicationContext.getBeanProvider(type).getIfAvailable(() -> {
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

    @Bean
    @ConditionalOnMissingBean
    public SpringAnnotatedRouteBootstrap springAnnotatedRouteBootstrap(
            Dispatcher dispatcher,
            AnnotatedRouteRegistrar annotatedRouteRegistrar,
            ObjectProvider<Object> beanProvider
    ) {
        log.debug("Creating SpringAnnotatedRouteBootstrap bean");
        return new SpringAnnotatedRouteBootstrap(dispatcher, annotatedRouteRegistrar, beanProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringPollingBootstrap springPollingBootstrap(ObjectProvider<LongPollingRunner> runnerProvider) {
        return new SpringPollingBootstrap(runnerProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnExpression("'${max.bot.mode:POLLING}' == 'POLLING' && '${max.bot.polling.enabled:true}' == 'true'")
    public PollingUpdateSource pollingUpdateSource(MaxBotClient maxBotClient) {
        return new SdkPollingUpdateSource(maxBotClient);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnExpression("'${max.bot.mode:POLLING}' == 'POLLING' && '${max.bot.polling.enabled:true}' == 'true'")
    public LongPollingRunnerConfig longPollingRunnerConfig(MaxBotProperties properties) {
        Integer timeoutSeconds = null;
        if (properties.getPolling().getTimeout() != null) {
            timeoutSeconds = Math.toIntExact(properties.getPolling().getTimeout().toSeconds());
        }
        PollingFetchRequest request = new PollingFetchRequest(
                null,
                timeoutSeconds,
                properties.getPolling().getLimit(),
                properties.getPolling().getTypes()
        );
        return LongPollingRunnerConfig.builder()
                .request(request)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnExpression("'${max.bot.mode:POLLING}' == 'POLLING' && '${max.bot.polling.enabled:true}' == 'true'")
    public LongPollingRunner longPollingRunner(
            PollingUpdateSource source,
            Dispatcher dispatcher,
            LongPollingRunnerConfig config
    ) {
        return new DefaultLongPollingRunner(source, dispatcher, config);
    }

    @Bean
    @ConditionalOnBean(LongPollingRunner.class)
    @ConditionalOnMissingBean
    @ConditionalOnExpression("'${max.bot.mode:POLLING}' == 'POLLING' && '${max.bot.polling.enabled:true}' == 'true'")
    public SpringPollingLifecycle springPollingLifecycle(LongPollingRunner longPollingRunner) {
        return new SpringPollingLifecycle(longPollingRunner);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnExpression("'${max.bot.mode:POLLING}' == 'WEBHOOK' || '${max.bot.webhook.enabled:false}' == 'true'")
    public WebhookSecretValidator webhookSecretValidator(MaxBotProperties properties) {
        return new DefaultWebhookSecretValidator(properties.getWebhook().getSecret());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnExpression("'${max.bot.mode:POLLING}' == 'WEBHOOK' || '${max.bot.webhook.enabled:false}' == 'true'")
    public WebhookReceiverConfig webhookReceiverConfig(MaxBotProperties properties) {
        Integer maxInFlight = properties.getWebhook().getMaxInFlight();
        if (maxInFlight == null) {
            return WebhookReceiverConfig.defaults();
        }
        return new WebhookReceiverConfig(maxInFlight);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnExpression("'${max.bot.mode:POLLING}' == 'WEBHOOK' || '${max.bot.webhook.enabled:false}' == 'true'")
    public WebhookReceiver webhookReceiver(
            WebhookSecretValidator secretValidator,
            JsonCodec jsonCodec,
            Dispatcher dispatcher,
            WebhookReceiverConfig config
    ) {
        return new DefaultWebhookReceiver(secretValidator, jsonCodec, dispatcher, config);
    }

    @Bean
    @ConditionalOnBean(WebhookReceiver.class)
    @ConditionalOnMissingBean
    @ConditionalOnExpression("'${max.bot.mode:POLLING}' == 'WEBHOOK' || '${max.bot.webhook.enabled:false}' == 'true'")
    public SpringWebhookAdapter springWebhookAdapter(WebhookReceiver webhookReceiver) {
        return new SpringWebhookAdapter(webhookReceiver);
    }

    @Bean
    @ConditionalOnBean(SpringWebhookAdapter.class)
    @ConditionalOnClass(name = "org.springframework.web.bind.annotation.RestController")
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnExpression("'${max.bot.mode:POLLING}' == 'WEBHOOK' || '${max.bot.webhook.enabled:false}' == 'true'")
    @ConditionalOnMissingBean
    public SpringWebhookController springWebhookController(SpringWebhookAdapter springWebhookAdapter) {
        return new SpringWebhookController(springWebhookAdapter);
    }
}
