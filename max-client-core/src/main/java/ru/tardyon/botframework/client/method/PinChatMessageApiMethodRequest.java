package ru.tardyon.botframework.client.method;

import java.util.Objects;
import java.util.Optional;
import ru.tardyon.botframework.client.MaxRequest;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.model.request.PinChatMessageApiRequest;
import ru.tardyon.botframework.model.response.OperationStatusResponse;

/**
 * Docs-shaped request for PUT /chats/{chatId}/pin.
 */
public final class PinChatMessageApiMethodRequest implements MaxRequest<OperationStatusResponse> {
    private final long chatId;
    private final PinChatMessageApiRequest request;

    public PinChatMessageApiMethodRequest(long chatId, PinChatMessageApiRequest request) {
        this.chatId = chatId;
        this.request = Objects.requireNonNull(request, "request");
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.PUT;
    }

    @Override
    public String path() {
        return "/chats/" + chatId + "/pin";
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
