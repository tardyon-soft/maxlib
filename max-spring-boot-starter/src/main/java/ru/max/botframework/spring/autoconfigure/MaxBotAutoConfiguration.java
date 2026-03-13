package ru.max.botframework.spring.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import ru.max.botframework.ingestion.LongPollingRunner;
import ru.max.botframework.ingestion.WebhookReceiver;
import ru.max.botframework.spring.polling.SpringPollingBootstrap;
import ru.max.botframework.spring.properties.MaxBotProperties;
import ru.max.botframework.spring.webhook.SpringWebhookAdapter;

/**
 * Baseline auto-configuration contract for MAX Spring starter.
 */
@AutoConfiguration
@EnableConfigurationProperties(MaxBotProperties.class)
public class MaxBotAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SpringPollingBootstrap springPollingBootstrap(ObjectProvider<LongPollingRunner> runnerProvider) {
        return new SpringPollingBootstrap(runnerProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnBean(WebhookReceiver.class)
    @ConditionalOnMissingBean
    public SpringWebhookAdapter springWebhookAdapter(WebhookReceiver webhookReceiver) {
        return new SpringWebhookAdapter(webhookReceiver);
    }
}
