package ru.tardyon.botframework.micronaut.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.micronaut.context.ApplicationContext;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.fsm.StateScope;
import ru.tardyon.botframework.model.UpdateEventType;
import ru.tardyon.botframework.screen.ScreenActionCodecMode;

class MaxBotPropertiesTest {

    @Test
    void bindsDefaultsWithMinimalConfiguration() {
        try (ApplicationContext context = context(Map.of(
                "max.bot.token", "test-token"
        ))) {
            MaxBotProperties properties = context.getBean(MaxBotProperties.class);

            assertEquals("test-token", properties.getToken());
            assertEquals("https://platform-api.max.ru", properties.getBaseUrl());
            assertEquals(MaxBotMode.POLLING, properties.getMode());
            assertTrue(properties.getPolling().isEnabled());
            assertEquals(100, properties.getPolling().getLimit());
            assertEquals(Duration.ofSeconds(30), properties.getPolling().getTimeout());
            assertEquals("/webhook/max", properties.getWebhook().getPath());
            assertEquals(MaxBotStorageType.MEMORY, properties.getStorage().getType());
            assertEquals(StateScope.USER_IN_CHAT, properties.getStorage().getStateScope());
            assertEquals("max:bot:fsm", properties.getStorage().getRedis().getKeyPrefix());
            assertEquals("max.screen", properties.getScreen().getNamespace());
            assertEquals(ScreenActionCodecMode.LEGACY_STRING, properties.getScreen().getCallback().getCodec().getMode());
        }
    }

    @Test
    void bindsExplicitPollingWebhookAndStorageSettings() {
        try (ApplicationContext context = context(Map.ofEntries(
                Map.entry("max.bot.token", "prod-token"),
                Map.entry("max.bot.base-url", "https://api.dev.max.ru"),
                Map.entry("max.bot.mode", "WEBHOOK"),
                Map.entry("max.bot.polling.enabled", "false"),
                Map.entry("max.bot.polling.limit", "20"),
                Map.entry("max.bot.polling.timeout", "45s"),
                Map.entry("max.bot.polling.types[0]", "message_created"),
                Map.entry("max.bot.polling.types[1]", "message_callback"),
                Map.entry("max.bot.webhook.enabled", "true"),
                Map.entry("max.bot.webhook.path", "/hooks/max"),
                Map.entry("max.bot.webhook.secret", "secret-1"),
                Map.entry("max.bot.webhook.max-in-flight", "16"),
                Map.entry("max.bot.storage.type", "REDIS"),
                Map.entry("max.bot.storage.state-scope", "CHAT"),
                Map.entry("max.bot.storage.redis.key-prefix", "max:test:fsm"),
                Map.entry("max.bot.storage.redis.ttl", "120s"),
                Map.entry("max.bot.screen.namespace", "custom.screen"),
                Map.entry("max.bot.screen.callback.codec.mode", "TYPED_V1")
        ))) {
            MaxBotProperties properties = context.getBean(MaxBotProperties.class);

            assertEquals("prod-token", properties.getToken());
            assertEquals("https://api.dev.max.ru", properties.getBaseUrl());
            assertEquals(MaxBotMode.WEBHOOK, properties.getMode());
            assertEquals(false, properties.getPolling().isEnabled());
            assertEquals(20, properties.getPolling().getLimit());
            assertEquals(Duration.ofSeconds(45), properties.getPolling().getTimeout());
            assertEquals(2, properties.getPolling().getTypes().size());
            assertEquals(UpdateEventType.MESSAGE_CREATED, properties.getPolling().getTypes().get(0));
            assertEquals(UpdateEventType.MESSAGE_CALLBACK, properties.getPolling().getTypes().get(1));
            assertEquals(true, properties.getWebhook().isEnabled());
            assertEquals("/hooks/max", properties.getWebhook().getPath());
            assertEquals("secret-1", properties.getWebhook().getSecret());
            assertEquals(16, properties.getWebhook().getMaxInFlight());
            assertEquals(MaxBotStorageType.REDIS, properties.getStorage().getType());
            assertEquals(StateScope.CHAT, properties.getStorage().getStateScope());
            assertEquals("max:test:fsm", properties.getStorage().getRedis().getKeyPrefix());
            assertEquals(Duration.ofSeconds(120), properties.getStorage().getRedis().getTtl());
            assertEquals("custom.screen", properties.getScreen().getNamespace());
            assertEquals(ScreenActionCodecMode.TYPED_V1, properties.getScreen().getCallback().getCodec().getMode());
        }
    }

    @Test
    void failsWhenTokenIsBlank() {
        RuntimeException failure = assertThrows(RuntimeException.class, () -> {
            try (ApplicationContext context = context(Map.of(
                    "max.bot.token", " "
            ))) {
                context.getBean(MaxBotProperties.class);
            }
        });

        assertTrue(failure.getMessage().contains("token"));
    }

    @Test
    void failsWhenTokenIsMissing() {
        RuntimeException failure = assertThrows(RuntimeException.class, () -> {
            try (ApplicationContext context = context(Map.of())) {
                context.getBean(MaxBotProperties.class);
            }
        });

        assertTrue(failure.getMessage().contains("max.bot") || failure.getMessage().contains("token"));
    }

    @Test
    void failsWhenNumericConstraintsAreInvalid() {
        RuntimeException failure = assertThrows(RuntimeException.class, () -> {
            try (ApplicationContext context = context(Map.of(
                    "max.bot.token", "test-token",
                    "max.bot.polling.limit", "0",
                    "max.bot.webhook.max-in-flight", "0"
            ))) {
                context.getBean(MaxBotProperties.class);
            }
        });

        assertTrue(failure.getMessage().contains("polling.limit")
                || failure.getMessage().contains("webhook.max-in-flight")
                || failure.getMessage().contains("max.bot"));
    }

    @Test
    void failsWhenNestedPropertiesAreInvalid() {
        RuntimeException failure = assertThrows(RuntimeException.class, () -> {
            try (ApplicationContext context = context(Map.of(
                    "max.bot.token", "test-token",
                    "max.bot.polling.limit", "0",
                    "max.bot.webhook.path", " "
            ))) {
                context.getBean(MaxBotProperties.class);
            }
        });

        assertTrue(failure.getMessage().contains("polling.limit") || failure.getMessage().contains("webhook.path"));
    }

    @Test
    void bindsEnumsExactly() {
        try (ApplicationContext context = context(Map.of(
                "max.bot.token", "test-token",
                "max.bot.mode", "WEBHOOK",
                "max.bot.storage.type", "REDIS",
                "max.bot.screen.callback.codec.mode", "TYPED_V1"
        ))) {
            MaxBotProperties properties = context.getBean(MaxBotProperties.class);

            assertEquals(MaxBotMode.WEBHOOK, properties.getMode());
            assertEquals(MaxBotStorageType.REDIS, properties.getStorage().getType());
            assertEquals(ScreenActionCodecMode.TYPED_V1, properties.getScreen().getCallback().getCodec().getMode());
        }
    }

    @Test
    void bindsDurationsExactly() {
        try (ApplicationContext context = context(Map.of(
                "max.bot.token", "test-token",
                "max.bot.polling.timeout", "45s",
                "max.bot.storage.redis.ttl", "120s"
        ))) {
            MaxBotProperties properties = context.getBean(MaxBotProperties.class);

            assertEquals(Duration.ofSeconds(45), properties.getPolling().getTimeout());
            assertEquals(Duration.ofSeconds(120), properties.getStorage().getRedis().getTtl());
        }
    }

    private static ApplicationContext context(Map<String, Object> properties) {
        java.util.LinkedHashMap<String, Object> merged = new java.util.LinkedHashMap<>();
        merged.put("max.bot.route-component-scan.enabled", "false");
        merged.putAll(properties);
        return ApplicationContext.builder()
                .properties(merged)
                .start();
    }
}
