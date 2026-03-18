package ru.tardyon.botframework.client.method;

import java.util.Objects;
import java.util.Optional;
import ru.tardyon.botframework.client.MaxRequest;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.model.request.UpdateChatApiRequest;
import ru.tardyon.botframework.model.transport.ApiChat;

/**
 * Docs-shaped request for PATCH /chats/{chatId}.
 */
public final class PatchChatApiMethodRequest implements MaxRequest<ApiChat> {
    private final long chatId;
    private final UpdateChatApiRequest request;

    public PatchChatApiMethodRequest(long chatId, UpdateChatApiRequest request) {
        this.chatId = chatId;
        this.request = Objects.requireNonNull(request, "request");
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.PATCH;
    }

    @Override
    public String path() {
        return "/chats/" + chatId;
    }

    @Override
    public Class<ApiChat> responseType() {
        return ApiChat.class;
    }

    @Override
    public Optional<Object> body() {
        return Optional.of(request);
    }
}
