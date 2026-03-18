package ru.tardyon.botframework.client.method;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import ru.tardyon.botframework.client.MaxRequest;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.model.mapping.MaxApiModelMapper;
import ru.tardyon.botframework.model.request.SendMessageApiRequest;
import ru.tardyon.botframework.model.response.MessageResponse;

/**
 * Docs-shaped request for POST /messages.
 */
public final class SendMessageApiMethodRequest implements MaxRequest<MessageResponse> {
    private final SendMessageApiRequest request;

    public SendMessageApiMethodRequest(SendMessageApiRequest request) {
        this.request = Objects.requireNonNull(request, "request");
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.POST;
    }

    @Override
    public String path() {
        return "/messages";
    }

    @Override
    public Class<MessageResponse> responseType() {
        return MessageResponse.class;
    }

    @Override
    public Optional<Object> body() {
        return Optional.of(MaxApiModelMapper.toApiOutgoing(request.body(), request.sendNotification(), request.replyToMessageId()));
    }

    @Override
    public Map<String, String> queryParameters() {
        Map<String, String> query = new LinkedHashMap<>();
        if (request.userId() != null) {
            query.put("user_id", String.valueOf(request.userId()));
        }
        if (request.chatId() != null) {
            query.put("chat_id", String.valueOf(request.chatId()));
        }
        if (request.disableLinkPreview() != null) {
            query.put("disable_link_preview", String.valueOf(request.disableLinkPreview()));
        }
        return Map.copyOf(query);
    }
}
