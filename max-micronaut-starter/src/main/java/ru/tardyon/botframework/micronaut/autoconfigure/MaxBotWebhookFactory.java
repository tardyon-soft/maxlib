package ru.tardyon.botframework.micronaut.autoconfigure;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import ru.tardyon.botframework.client.serialization.JsonCodec;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.ingestion.DefaultWebhookReceiver;
import ru.tardyon.botframework.ingestion.DefaultWebhookSecretValidator;
import ru.tardyon.botframework.ingestion.WebhookReceiver;
import ru.tardyon.botframework.ingestion.WebhookReceiverConfig;
import ru.tardyon.botframework.ingestion.WebhookSecretValidator;
import ru.tardyon.botframework.micronaut.properties.MaxBotProperties;
import ru.tardyon.botframework.micronaut.webhook.MicronautWebhookAdapter;

/**
 * Webhook-specific Micronaut bean wiring.
 */
@Factory
public final class MaxBotWebhookFactory {
    @Singleton
    @Requires(condition = WebhookModeOrEnabledCondition.class)
    @Requires(missingBeans = WebhookSecretValidator.class)
    WebhookSecretValidator webhookSecretValidatorMax(MaxBotProperties properties) {
        return new DefaultWebhookSecretValidator(properties.getWebhook().getSecret());
    }

    @Singleton
    @Requires(condition = WebhookModeOrEnabledCondition.class)
    @Requires(missingBeans = WebhookReceiverConfig.class)
    WebhookReceiverConfig webhookReceiverConfigMax(MaxBotProperties properties) {
        return webhookReceiverConfig(properties);
    }

    @Singleton
    @Requires(condition = WebhookModeOrEnabledCondition.class)
    @Requires(missingBeans = WebhookReceiver.class)
    WebhookReceiver webhookReceiverMax(
            WebhookSecretValidator secretValidator,
            JsonCodec jsonCodec,
            Dispatcher dispatcher,
            WebhookReceiverConfig config
    ) {
        return new DefaultWebhookReceiver(secretValidator, jsonCodec, dispatcher, config);
    }

    @Singleton
    @Requires(condition = WebhookModeOrEnabledCondition.class)
    @Requires(beans = WebhookReceiver.class)
    @Requires(missingBeans = MicronautWebhookAdapter.class)
    MicronautWebhookAdapter micronautWebhookAdapterMax(WebhookReceiver webhookReceiver) {
        return new MicronautWebhookAdapter(webhookReceiver);
    }

    private static WebhookReceiverConfig webhookReceiverConfig(MaxBotProperties properties) {
        Integer maxInFlight = properties.getWebhook().getMaxInFlight();
        if (maxInFlight == null) {
            return WebhookReceiverConfig.defaults();
        }
        return new WebhookReceiverConfig(maxInFlight);
    }
}
