package ru.max.botframework.spring.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import ru.max.botframework.fsm.StateScope;
import ru.max.botframework.model.UpdateEventType;
import ru.max.botframework.spring.autoconfigure.MaxBotAutoConfiguration;

class MaxBotPropertiesTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MaxBotAutoConfiguration.class));

    @Test
    void bindsDefaultsWithMinimalConfiguration() {
        contextRunner
                .withPropertyValues("max.bot.token=test-token")
                .run(context -> {
                    assertTrue(context.hasSingleBean(MaxBotProperties.class));
                    MaxBotProperties properties = context.getBean(MaxBotProperties.class);

                    assertEquals("test-token", properties.getToken());
                    assertEquals("https://api.max.ru", properties.getBaseUrl());
                    assertEquals(MaxBotMode.POLLING, properties.getMode());
                    assertTrue(properties.getPolling().isEnabled());
                    assertEquals(100, properties.getPolling().getLimit());
                    assertEquals(java.time.Duration.ofSeconds(30), properties.getPolling().getTimeout());
                    assertEquals("/webhook/max", properties.getWebhook().getPath());
                    assertEquals(MaxBotStorageType.MEMORY, properties.getStorage().getType());
                    assertEquals(StateScope.USER_IN_CHAT, properties.getStorage().getStateScope());
                });
    }

    @Test
    void bindsExplicitPollingWebhookAndStorageSettings() {
        contextRunner
                .withPropertyValues(
                        "max.bot.token=prod-token",
                        "max.bot.base-url=https://api.dev.max.ru",
                        "max.bot.mode=WEBHOOK",
                        "max.bot.polling.enabled=false",
                        "max.bot.polling.limit=20",
                        "max.bot.polling.timeout=45s",
                        "max.bot.polling.types[0]=message_created",
                        "max.bot.polling.types[1]=message_callback",
                        "max.bot.webhook.enabled=true",
                        "max.bot.webhook.path=/hooks/max",
                        "max.bot.webhook.secret=secret-1",
                        "max.bot.webhook.max-in-flight=16",
                        "max.bot.storage.type=MEMORY",
                        "max.bot.storage.state-scope=CHAT"
                )
                .run(context -> {
                    MaxBotProperties properties = context.getBean(MaxBotProperties.class);

                    assertEquals("prod-token", properties.getToken());
                    assertEquals("https://api.dev.max.ru", properties.getBaseUrl());
                    assertEquals(MaxBotMode.WEBHOOK, properties.getMode());
                    assertEquals(false, properties.getPolling().isEnabled());
                    assertEquals(20, properties.getPolling().getLimit());
                    assertEquals(java.time.Duration.ofSeconds(45), properties.getPolling().getTimeout());
                    assertEquals(2, properties.getPolling().getTypes().size());
                    assertEquals(UpdateEventType.MESSAGE_CREATED, properties.getPolling().getTypes().get(0));
                    assertEquals(UpdateEventType.MESSAGE_CALLBACK, properties.getPolling().getTypes().get(1));
                    assertEquals(true, properties.getWebhook().isEnabled());
                    assertEquals("/hooks/max", properties.getWebhook().getPath());
                    assertEquals("secret-1", properties.getWebhook().getSecret());
                    assertEquals(16, properties.getWebhook().getMaxInFlight());
                    assertEquals(MaxBotStorageType.MEMORY, properties.getStorage().getType());
                    assertEquals(StateScope.CHAT, properties.getStorage().getStateScope());
                });
    }

    @Test
    void failsWhenTokenIsMissing() {
        contextRunner.run(context -> {
            assertNotNull(context.getStartupFailure());
            assertTrue(context.getStartupFailure().getMessage().contains("max.bot"));
        });
    }

    @Test
    void failsWhenNumericConstraintsAreInvalid() {
        contextRunner
                .withPropertyValues(
                        "max.bot.token=test-token",
                        "max.bot.polling.limit=0",
                        "max.bot.webhook.max-in-flight=0"
                )
                .run(context -> {
                    assertNotNull(context.getStartupFailure());
                    assertTrue(context.getStartupFailure().getMessage().contains("max.bot"));
                });
    }
}
