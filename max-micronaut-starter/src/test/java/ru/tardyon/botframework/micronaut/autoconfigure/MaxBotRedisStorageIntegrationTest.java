package ru.tardyon.botframework.micronaut.autoconfigure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import ru.tardyon.botframework.fsm.FSMStorage;
import ru.tardyon.botframework.fsm.MemoryStorage;
import ru.tardyon.botframework.fsm.StateKey;
import ru.tardyon.botframework.micronaut.properties.MaxBotProperties;
import ru.tardyon.botframework.micronaut.properties.MaxBotStorageType;
import ru.tardyon.botframework.micronaut.storage.RedisFSMStorage;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.UserId;

class MaxBotRedisStorageIntegrationTest {

    @Test
    void defaultStoragePathStaysMemory() {
        try (ApplicationContext context = context("memory-default", Map.of())) {
            FSMStorage storage = context.getBean(FSMStorage.class);

            assertInstanceOf(MemoryStorage.class, storage);
            assertEquals(MaxBotStorageType.MEMORY, context.getBean(MaxBotProperties.class).getStorage().getType());
        }
    }

    @Test
    void explicitRedisUsesRedisBackedStorage() {
        try (ApplicationContext context = context("redis-template", Map.of(
                "max.bot.storage.type", "REDIS"
        ))) {
            FSMStorage storage = context.getBean(FSMStorage.class);

            assertInstanceOf(RedisFSMStorage.class, storage);
            assertEquals(MaxBotStorageType.REDIS, context.getBean(MaxBotProperties.class).getStorage().getType());
        }
    }

    @Test
    void redisMisconfigurationFailsWithClearDiagnostic() {
        RuntimeException failure = assertThrows(RuntimeException.class, () -> {
            try (ApplicationContext context = context("redis-misconfigured", Map.of(
                    "max.bot.storage.type", "REDIS"
            ))) {
                context.getBean(FSMStorage.class);
            }
        });

        assertTrue(failure.getMessage().contains("RedisConnectionFactory is missing"));
    }

    @Test
    void redisTtlAndKeyPrefixArePropagated() {
        RedisTemplateFactory.reset();

        try (ApplicationContext context = context("redis-template", Map.of(
                "max.bot.storage.type", "REDIS",
                "max.bot.storage.redis.key-prefix", "max:test:fsm",
                "max.bot.storage.redis.ttl", "120s"
        ))) {
            FSMStorage storage = context.getBean(FSMStorage.class);
            assertInstanceOf(RedisFSMStorage.class, storage);

            storage.setState(
                    StateKey.userInChat(new UserId("u1"), new ChatId("c1")),
                    "form:name"
            ).toCompletableFuture().join();

            verify(RedisTemplateFactory.VALUES).set(
                    eq("max:test:fsm:user_in_chat:u:u1:c:c1:state"),
                    eq("form:name"),
                    eq(Duration.ofSeconds(120))
            );
        }
    }

    @Test
    void redisKeyPrefixIsUsedWithoutTtlWhenTtlIsMissing() {
        RedisTemplateFactory.reset();

        try (ApplicationContext context = context("redis-template", Map.of(
                "max.bot.storage.type", "REDIS",
                "max.bot.storage.redis.key-prefix", "max:custom:fsm"
        ))) {
            FSMStorage storage = context.getBean(FSMStorage.class);
            assertInstanceOf(RedisFSMStorage.class, storage);

            storage.setState(
                    StateKey.userInChat(new UserId("u1"), new ChatId("c1")),
                    "form:name"
            ).toCompletableFuture().join();

            verify(RedisTemplateFactory.VALUES).set(
                    eq("max:custom:fsm:user_in_chat:u:u1:c:c1:state"),
                    eq("form:name")
            );
        }
    }

    private static ApplicationContext context(String specName, Map<String, Object> extraProperties) {
        LinkedHashMap<String, Object> properties = new LinkedHashMap<>();
        properties.put("spec.name", specName);
        properties.put("max.bot.token", "test-token");
        properties.put("max.bot.route-component-scan.enabled", "false");
        properties.putAll(extraProperties);
        return ApplicationContext.builder()
                .properties(properties)
                .start();
    }

    @Factory
    @Requires(property = "spec.name", value = "redis-template")
    static final class RedisTemplateFactory {
        static final StringRedisTemplate TEMPLATE = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        static final ValueOperations<String, String> VALUES = Mockito.mock(ValueOperations.class);

        static void reset() {
            Mockito.reset(TEMPLATE, VALUES);
            when(TEMPLATE.opsForValue()).thenReturn(VALUES);
        }

        @Singleton
        StringRedisTemplate stringRedisTemplate() {
            return TEMPLATE;
        }
    }
}
