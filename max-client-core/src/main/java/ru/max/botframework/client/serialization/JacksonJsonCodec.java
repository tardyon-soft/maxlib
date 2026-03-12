package ru.max.botframework.client.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import ru.max.botframework.client.error.MaxSerializationException;

/**
 * Jackson-backed JSON codec for MAX client-core foundation layer.
 */
public final class JacksonJsonCodec implements JsonCodec {
    private final ObjectMapper objectMapper;

    public JacksonJsonCodec() {
        this(new ObjectMapper()
                .registerModule(new ParameterNamesModule())
                .registerModule(new JavaTimeModule()));
    }

    public JacksonJsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> T read(String source, Class<T> targetType) {
        try {
            return objectMapper.readValue(source, targetType);
        } catch (JsonProcessingException e) {
            throw new MaxSerializationException("Unable to deserialize MAX API response", e);
        }
    }

    @Override
    public String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new MaxSerializationException("Unable to serialize MAX API request body", e);
        }
    }
}
