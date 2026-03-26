package ru.tardyon.botframework.spring.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import ru.tardyon.botframework.client.serialization.JacksonJsonCodec;
import ru.tardyon.botframework.fsm.StateData;
import ru.tardyon.botframework.fsm.StateKey;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.UserId;

class RedisFSMStorageTest {

    @Test
    void readsStateDataAndStateFromRedis() {
        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = Mockito.mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("max:test:user_in_chat:u:u1:c:c1:state")).thenReturn("form:name");
        when(values.get("max:test:user_in_chat:u:u1:c:c1:data")).thenReturn("{\"name\":\"Alice\"}");

        RedisFSMStorage storage = new RedisFSMStorage(redis, new JacksonJsonCodec(), "max:test", null);
        StateKey key = StateKey.userInChat(new UserId("u1"), new ChatId("c1"));

        Optional<String> state = storage.getState(key).toCompletableFuture().join();
        StateData data = storage.getStateData(key).toCompletableFuture().join();

        assertEquals(Optional.of("form:name"), state);
        assertEquals("Alice", data.get("name", String.class).orElseThrow());
    }

    @Test
    void writesWithTtlWhenConfigured() {
        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = Mockito.mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);

        RedisFSMStorage storage = new RedisFSMStorage(redis, new JacksonJsonCodec(), "max:test", Duration.ofSeconds(60));
        StateKey key = StateKey.userInChat(new UserId("u1"), new ChatId("c1"));

        storage.setState(key, "form:name").toCompletableFuture().join();

        verify(values).set(eq("max:test:user_in_chat:u:u1:c:c1:state"), eq("form:name"), eq(Duration.ofSeconds(60)));
    }

    @Test
    void clearsDataKeyWhenEmptyPayloadProvided() {
        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = Mockito.mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);

        RedisFSMStorage storage = new RedisFSMStorage(redis, new JacksonJsonCodec(), "max:test", null);
        StateKey key = StateKey.userInChat(new UserId("u1"), new ChatId("c1"));

        storage.setStateData(key, StateData.of(Map.of())).toCompletableFuture().join();

        verify(redis).delete("max:test:user_in_chat:u:u1:c:c1:data");
        verify(values, Mockito.never()).set(eq("max:test:user_in_chat:u:u1:c:c1:data"), any(String.class));
    }

    @Test
    void returnsEmptyStateWhenKeyAbsent() {
        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = Mockito.mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("max:test:user_in_chat:u:u1:c:c1:state")).thenReturn(null);

        RedisFSMStorage storage = new RedisFSMStorage(redis, new JacksonJsonCodec(), "max:test", null);
        StateKey key = StateKey.userInChat(new UserId("u1"), new ChatId("c1"));

        Optional<String> state = storage.getState(key).toCompletableFuture().join();
        assertFalse(state.isPresent());
    }
}
