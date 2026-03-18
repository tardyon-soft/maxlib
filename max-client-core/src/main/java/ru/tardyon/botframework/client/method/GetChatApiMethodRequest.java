package ru.tardyon.botframework.client.method;

import ru.tardyon.botframework.client.MaxRequest;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.model.transport.ApiChat;

/**
 * Docs-shaped request for GET /chats/{chatId}.
 */
public final class GetChatApiMethodRequest implements MaxRequest<ApiChat> {
    private final long chatId;

    public GetChatApiMethodRequest(long chatId) {
        this.chatId = chatId;
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.GET;
    }

    @Override
    public String path() {
        return "/chats/" + chatId;
    }

    @Override
    public Class<ApiChat> responseType() {
        return ApiChat.class;
    }
}
