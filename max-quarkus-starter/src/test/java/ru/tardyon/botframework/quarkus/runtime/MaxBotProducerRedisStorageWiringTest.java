package ru.tardyon.botframework.quarkus.runtime;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import io.quarkus.redis.datasource.RedisDataSource;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.tardyon.botframework.fsm.FSMStorage;
import ru.tardyon.botframework.quarkus.storage.RedisFSMStorage;

@QuarkusComponentTest(MaxBotProducer.class)
@TestConfigProperty(key = "max.bot.token", value = "test-token")
@TestConfigProperty(key = "max.bot.storage.type", value = "REDIS")
class MaxBotProducerRedisStorageWiringTest {
    @Inject
    FSMStorage fsmStorage;

    @Test
    void selectsRedisStorageWhenConfigured() {
        assertInstanceOf(RedisFSMStorage.class, fsmStorage);
    }

    @Singleton
    static final class RedisBeans {
        @Produces
        RedisDataSource redisDataSource() {
            return Mockito.mock(RedisDataSource.class);
        }
    }
}
