package ru.tardyon.botframework.client.method;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import ru.tardyon.botframework.client.MaxRequest;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.model.mapping.MaxApiModelMapper;
import ru.tardyon.botframework.model.request.SendMessageRequest;
import ru.tardyon.botframework.model.response.MessageResponse;

/**
 * Domain-level request for POST /messages.
 */
public final class SendMessageMethodRequest implements MaxRequest<MessageResponse> {
    private final SendMessageRequest request;

    public SendMessageMethodRequest(SendMessageRequest request) {
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
        return Map.of("chat_id", request.chatId().value());
    }

}
