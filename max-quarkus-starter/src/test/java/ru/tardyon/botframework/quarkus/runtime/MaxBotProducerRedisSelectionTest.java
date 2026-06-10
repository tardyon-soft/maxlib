package ru.tardyon.botframework.quarkus.runtime;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.enterprise.inject.Instance;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.Optional;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.client.serialization.JsonCodec;
import ru.tardyon.botframework.fsm.FSMStorage;
import ru.tardyon.botframework.fsm.MemoryStorage;
import ru.tardyon.botframework.quarkus.properties.MaxBotStorageType;
import ru.tardyon.botframework.quarkus.storage.RedisFSMStorage;

class MaxBotProducerRedisSelectionTest {
    @Test
    void selectsMemoryStorageWhenConfigured() {
        MaxBotProducer producer = new MaxBotProducer();
        producer.config = config(MaxBotStorageType.MEMORY, "max:test", null);

        FSMStorage storage = producer.fsmStorage(mock(JsonCodec.class), unresolvedInstance(), unresolvedInstance());

        assertInstanceOf(MemoryStorage.class, storage);
    }

    @Test
    void selectsRedisStorageWhenConfiguredAndRedisBeansExist() {
        MaxBotProducer producer = new MaxBotProducer();
        producer.config = config(MaxBotStorageType.REDIS, "max:test", Duration.ofSeconds(45));

        Object redisTemplate = mockRedisTemplate();
        FSMStorage storage = producer.fsmStorage(
                mock(JsonCodec.class),
                resolvableInstance(redisTemplate),
                unresolvedInstance()
        );

        assertInstanceOf(RedisFSMStorage.class, storage);
    }

    @Test
    void failsWithClearMessageWhenRedisBeansAreMissing() {
        MaxBotProducer producer = new MaxBotProducer();
        producer.config = config(MaxBotStorageType.REDIS, "max:test", null);

        IllegalStateException failure = assertThrows(IllegalStateException.class, () ->
                producer.fsmStorage(mock(JsonCodec.class), unresolvedInstance(), unresolvedInstance())
        );

        assertTrue(failure.getMessage().contains("RedisDataSource is missing"));
    }

    @Test
    void failsClearlyWhenQuarkusRedisClassesAreMissing() throws Exception {
        ClassLoader filtered = new MissingQuarkusRedisClassLoader(getClass().getClassLoader());
        Class<?> producerClass = filtered.loadClass("ru.tardyon.botframework.quarkus.runtime.MaxBotProducer");
        Object producer = producerClass.getConstructor().newInstance();
        Field configField = producerClass.getDeclaredField("config");
        configField.setAccessible(true);
        configField.set(producer, config(MaxBotStorageType.REDIS, "max:test", null));

        var fsmStorage = producerClass.getDeclaredMethod(
                "fsmStorage",
                JsonCodec.class,
                Instance.class,
                Instance.class
        );
        fsmStorage.setAccessible(true);

        InvocationTargetException failure = assertThrows(InvocationTargetException.class, () ->
                fsmStorage.invoke(producer, mock(JsonCodec.class), unresolvedInstance(), unresolvedInstance())
        );

        assertInstanceOf(IllegalStateException.class, failure.getCause());
        assertTrue(failure.getCause().getMessage().contains("quarkus-redis-client is not available"));
    }

    private static Config config(MaxBotStorageType storageType, String keyPrefix, Duration ttl) {
        Config config = mock(Config.class);
        when(config.getValue("max.bot.token", String.class)).thenReturn("test-token");
        when(config.getOptionalValue("max.bot.storage.type", MaxBotStorageType.class)).thenReturn(Optional.of(storageType));
        when(config.getOptionalValue("max.bot.storage.redis.key-prefix", String.class)).thenReturn(Optional.of(keyPrefix));
        when(config.getOptionalValue("max.bot.storage.redis.ttl", Duration.class)).thenReturn(Optional.ofNullable(ttl));
        return config;
    }

    @SuppressWarnings("unchecked")
    private static Instance<Object> unresolvedInstance() {
        Instance<Object> provider = mock(Instance.class);
        Instance<Object> selected = mock(Instance.class);
        when(provider.select(any(Class.class))).thenReturn(selected);
        when(selected.isResolvable()).thenReturn(false);
        return provider;
    }

    @SuppressWarnings("unchecked")
    private static Instance<Object> resolvableInstance(Object bean) {
        Instance<Object> provider = mock(Instance.class);
        Instance<Object> selected = mock(Instance.class);
        when(provider.select(any(Class.class))).thenReturn(selected);
        when(selected.isResolvable()).thenReturn(true);
        when(selected.get()).thenReturn(bean);
        return provider;
    }

    @SuppressWarnings("unchecked")
    private static Object mockRedisTemplate() {
        try {
            return mock((Class<Object>) Class.forName("io.quarkus.redis.datasource.RedisDataSource"));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class MissingQuarkusRedisClassLoader extends ClassLoader {
        MissingQuarkusRedisClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith("io.quarkus.redis.")) {
                throw new ClassNotFoundException(name);
            }
            if (name.equals("ru.tardyon.botframework.quarkus.runtime.MaxBotProducer")) {
                synchronized (getClassLoadingLock(name)) {
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded == null) {
                        loaded = defineProducerClass();
                    }
                    if (resolve) {
                        resolveClass(loaded);
                    }
                    return loaded;
                }
            }
            return super.loadClass(name, resolve);
        }

        private Class<?> defineProducerClass() throws ClassNotFoundException {
            try (var in = MaxBotProducer.class.getResourceAsStream("/ru/tardyon/botframework/quarkus/runtime/MaxBotProducer.class")) {
                if (in == null) {
                    throw new ClassNotFoundException("MaxBotProducer class bytes not found");
                }
                byte[] bytes = in.readAllBytes();
                return defineClass("ru.tardyon.botframework.quarkus.runtime.MaxBotProducer", bytes, 0, bytes.length);
            } catch (java.io.IOException e) {
                throw new ClassNotFoundException("Failed to read MaxBotProducer class bytes", e);
            }
        }
    }
}
