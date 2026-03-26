package ru.tardyon.botframework.client.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.tardyon.botframework.client.error.MaxSerializationException;
import ru.tardyon.botframework.model.Message;
import ru.tardyon.botframework.model.mapping.MaxApiModelMapper;
import ru.tardyon.botframework.model.response.GetUpdatesResponse;
import ru.tardyon.botframework.model.response.MessageResponse;
import ru.tardyon.botframework.model.response.MessagesResponse;
import ru.tardyon.botframework.model.transport.ApiGetUpdatesResponse;
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
            fallback = tryReadMessagesResponseFallback(source, targetType);
            if (fallback != null) {
                return fallback;
            }
            fallback = tryReadGetUpdatesResponseFallback(source, targetType);
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

    @SuppressWarnings("unchecked")
    private <T> T tryReadMessagesResponseFallback(String source, Class<T> targetType) {
        if (targetType != MessagesResponse.class) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(source);
            JsonNode payload = root.has("messages") ? root.get("messages") : root;
            if (payload == null || !payload.isArray()) {
                return null;
            }

            java.util.ArrayList<Message> messages = new java.util.ArrayList<>();
            for (JsonNode node : payload) {
                Message normalized = tryTreeToValue(node, Message.class);
                if (normalized != null) {
                    messages.add(normalized);
                    continue;
                }
                ApiMessage apiMessage = tryTreeToValue(node, ApiMessage.class);
                if (apiMessage != null) {
                    messages.add(MaxApiModelMapper.toNormalized(apiMessage));
                }
            }
            return (T) new MessagesResponse(messages);
        } catch (Exception ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T tryReadGetUpdatesResponseFallback(String source, Class<T> targetType) {
        if (targetType != GetUpdatesResponse.class) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(source);
            JsonNode updatesNode = root.get("updates");
            if (updatesNode == null || !updatesNode.isArray()) {
                return null;
            }

            java.util.ArrayList<ru.tardyon.botframework.model.Update> updates = new java.util.ArrayList<>();
            for (JsonNode node : updatesNode) {
                ru.tardyon.botframework.model.Update normalized = tryTreeToValue(node, ru.tardyon.botframework.model.Update.class);
                if (normalized != null) {
                    updates.add(normalized);
                    continue;
                }
                var apiUpdate = tryTreeToValue(node, ru.tardyon.botframework.model.transport.ApiUpdate.class);
                if (apiUpdate != null) {
                    updates.add(MaxApiModelMapper.toNormalized(apiUpdate));
                }
            }

            Long marker = null;
            JsonNode markerNode = root.get("marker");
            if (markerNode != null && markerNode.isNumber()) {
                marker = markerNode.longValue();
            }

            return (T) new GetUpdatesResponse(updates, marker);
        } catch (Exception ignored) {
            try {
                ApiGetUpdatesResponse api = objectMapper.readValue(source, ApiGetUpdatesResponse.class);
                return (T) new GetUpdatesResponse(
                        api.updates().stream().map(MaxApiModelMapper::toNormalized).toList(),
                        api.marker()
                );
            } catch (Exception alsoIgnored) {
                return null;
            }
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
