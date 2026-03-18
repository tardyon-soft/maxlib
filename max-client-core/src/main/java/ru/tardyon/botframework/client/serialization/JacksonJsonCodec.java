package ru.tardyon.botframework.client.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.tardyon.botframework.client.error.MaxSerializationException;
import ru.tardyon.botframework.model.Message;
import ru.tardyon.botframework.model.mapping.MaxApiModelMapper;
import ru.tardyon.botframework.model.response.MessageResponse;
import ru.tardyon.botframework.model.transport.ApiMessage;

/**
 * Jackson-backed JSON codec for MAX client-core foundation layer.
 */
public final class JacksonJsonCodec implements JsonCodec {
    private final ObjectMapper objectMapper;

    public JacksonJsonCodec() {
        this.objectMapper = SharedObjectMapper.instance();
    }

    @Override
    public <T> T read(String source, Class<T> targetType) {
        try {
            return objectMapper.readValue(source, targetType);
        } catch (JsonProcessingException e) {
            T fallback = tryReadMessageResponseFallback(source, targetType);
            if (fallback != null) {
                return fallback;
            }
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

    @SuppressWarnings("unchecked")
    private <T> T tryReadMessageResponseFallback(String source, Class<T> targetType) {
        if (targetType != MessageResponse.class) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(source);
            JsonNode payload = root.hasNonNull("message") ? root.get("message") : root;

            Message normalized = tryTreeToValue(payload, Message.class);
            if (normalized != null) {
                return (T) new MessageResponse(normalized);
            }

            ApiMessage apiMessage = tryTreeToValue(payload, ApiMessage.class);
            if (apiMessage != null) {
                return (T) new MessageResponse(MaxApiModelMapper.toNormalized(apiMessage));
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private <T> T tryTreeToValue(JsonNode node, Class<T> targetType) {
        try {
            return objectMapper.treeToValue(node, targetType);
        } catch (Exception ignored) {
            return null;
        }
    }
}
