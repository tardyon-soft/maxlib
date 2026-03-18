package ru.tardyon.botframework.client.method;

import ru.tardyon.botframework.client.MaxRequest;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.model.response.OperationStatusResponse;

/**
 * Docs-shaped request for DELETE /chats/{chatId}/members/me.
 */
public final class LeaveChatApiMethodRequest implements MaxRequest<OperationStatusResponse> {
    private final long chatId;

    public LeaveChatApiMethodRequest(long chatId) {
        this.chatId = chatId;
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.DELETE;
    }

    @Override
    public String path() {
        return "/chats/" + chatId + "/members/me";
    }

    @Override
    public Class<OperationStatusResponse> responseType() {
        return OperationStatusResponse.class;
    }
}
