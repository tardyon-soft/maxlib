package ru.tardyon.botframework.quarkus.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import ru.tardyon.botframework.screen.ScreenActionCodecMode;

class MaxBotPropertiesDefaultsTest {
    @Test
    void bindsDefaultsWithMinimalConfiguration() {
        MaxBotProperties properties = bind(Map.of("max.bot.token", "test-token"));

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
        assertNull(properties.getStorage().getRedis().getTtl());
        assertEquals("max.screen", properties.getScreen().getNamespace());
        assertEquals(ScreenActionCodecMode.LEGACY_STRING, properties.getScreen().getCallback().getCodec().getMode());
    }

    private static MaxBotProperties bind(Map<String, Object> properties) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", new LinkedHashMap<>(properties)));
        return Binder.get(environment).bind("max.bot", Bindable.of(MaxBotProperties.class))
                .orElseThrow(IllegalStateException::new);
    }
}
