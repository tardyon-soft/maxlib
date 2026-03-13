package ru.max.botframework.spring.autoconfigure;

import java.util.List;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import ru.max.botframework.client.DefaultMaxBotClient;
import ru.max.botframework.client.MaxApiClientConfig;
import ru.max.botframework.client.MaxBotClient;
import ru.max.botframework.client.http.MaxHttpClient;
import ru.max.botframework.client.http.okhttp.OkHttpMaxHttpClient;
import ru.max.botframework.client.serialization.JacksonJsonCodec;
import ru.max.botframework.client.serialization.JsonCodec;
import ru.max.botframework.dispatcher.Dispatcher;
import ru.max.botframework.dispatcher.Router;
import ru.max.botframework.fsm.FSMStorage;
import ru.max.botframework.fsm.MemoryStorage;
import ru.max.botframework.fsm.SceneRegistry;
import ru.max.botframework.fsm.SceneStorage;
import ru.max.botframework.ingestion.DefaultLongPollingRunner;
import ru.max.botframework.ingestion.DefaultWebhookReceiver;
import ru.max.botframework.ingestion.DefaultWebhookSecretValidator;
import ru.max.botframework.ingestion.LongPollingRunnerConfig;
import ru.max.botframework.ingestion.PollingFetchRequest;
import ru.max.botframework.ingestion.PollingUpdateSource;
import ru.max.botframework.ingestion.SdkPollingUpdateSource;
import ru.max.botframework.ingestion.LongPollingRunner;
import ru.max.botframework.ingestion.WebhookReceiverConfig;
import ru.max.botframework.ingestion.WebhookReceiver;
import ru.max.botframework.ingestion.WebhookSecretValidator;
import ru.max.botframework.spring.polling.SpringPollingBootstrap;
import ru.max.botframework.spring.polling.SpringPollingLifecycle;
import ru.max.botframework.spring.properties.MaxBotProperties;
import ru.max.botframework.spring.properties.MaxBotStorageType;
import ru.max.botframework.spring.webhook.SpringWebhookAdapter;
import ru.max.botframework.spring.webhook.SpringWebhookController;
import ru.max.botframework.upload.UploadService;

/**
 * Baseline auto-configuration contract for MAX Spring starter.
 */
@AutoConfiguration
@EnableConfigurationProperties(MaxBotProperties.class)
public class MaxBotAutoConfiguration {
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
    public FSMStorage fsmStorage(MaxBotProperties properties) {
        if (properties.getStorage().getType() == MaxBotStorageType.MEMORY) {
            return new MemoryStorage();
        }
        throw new IllegalStateException("Unsupported storage type: " + properties.getStorage().getType());
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
        for (Router router : routers) {
            dispatcher.includeRouter(router);
        }
        return dispatcher;
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
