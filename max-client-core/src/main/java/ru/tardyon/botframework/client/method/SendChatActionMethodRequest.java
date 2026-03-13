package ru.tardyon.botframework.client.method;

import java.util.Objects;
import java.util.Optional;
import ru.tardyon.botframework.client.MaxRequest;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.request.SendChatActionRequest;
import ru.tardyon.botframework.model.response.OperationStatusResponse;

/**
 * Domain-level request for POST /chats/{chatId}/actions operation.
 */
public final class SendChatActionMethodRequest implements MaxRequest<OperationStatusResponse> {
    private final ChatId chatId;
    private final SendChatActionRequest request;

    public SendChatActionMethodRequest(ChatId chatId, SendChatActionRequest request) {
        this.chatId = Objects.requireNonNull(chatId, "chatId");
        this.request = Objects.requireNonNull(request, "request");
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.POST;
    }

    @Override
    public String path() {
        return "/chats/" + chatId.value() + "/actions";
    }

    @Override
    public Class<OperationStatusResponse> responseType() {
        return OperationStatusResponse.class;
    }

    @Override
    public Optional<Object> body() {
        return Optional.of(request);
    }
}
