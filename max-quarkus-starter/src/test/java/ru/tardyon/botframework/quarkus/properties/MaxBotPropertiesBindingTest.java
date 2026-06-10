package ru.tardyon.botframework.quarkus.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import ru.tardyon.botframework.fsm.StateScope;
import ru.tardyon.botframework.model.UpdateEventType;
import ru.tardyon.botframework.screen.ScreenActionCodecMode;

class MaxBotPropertiesBindingTest {
    @Test
    void bindsExplicitPollingWebhookAndStorageSettings() {
        MaxBotProperties properties = bind(Map.ofEntries(
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
                Map.entry("max.bot.screen.callback.codec.mode", "TYPED_V1")));

        assertEquals("prod-token", properties.getToken());
        assertEquals("https://api.dev.max.ru", properties.getBaseUrl());
        assertEquals(MaxBotMode.WEBHOOK, properties.getMode());
        assertFalse(properties.getPolling().isEnabled());
        assertEquals(20, properties.getPolling().getLimit());
        assertEquals(Duration.ofSeconds(45), properties.getPolling().getTimeout());
        assertEquals(2, properties.getPolling().getTypes().size());
        assertEquals(UpdateEventType.MESSAGE_CREATED, properties.getPolling().getTypes().get(0));
        assertEquals(UpdateEventType.MESSAGE_CALLBACK, properties.getPolling().getTypes().get(1));
        assertTrue(properties.getWebhook().isEnabled());
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

    private static MaxBotProperties bind(Map<String, Object> properties) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", new LinkedHashMap<>(properties)));
        return Binder.get(environment).bind("max.bot", Bindable.of(MaxBotProperties.class))
                .orElseThrow(IllegalStateException::new);
    }
}
