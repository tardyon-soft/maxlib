package ru.tardyon.botframework.client.method;

import ru.tardyon.botframework.client.MaxRequest;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.model.response.OperationStatusResponse;

/**
 * Docs-shaped request for DELETE /chats/{chatId}/members/admins/{userId}.
 */
public final class RemoveChatAdminApiMethodRequest implements MaxRequest<OperationStatusResponse> {
    private final long chatId;
    private final long userId;

    public RemoveChatAdminApiMethodRequest(long chatId, long userId) {
        this.chatId = chatId;
        this.userId = userId;
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.DELETE;
    }

    @Override
    public String path() {
        return "/chats/" + chatId + "/members/admins/" + userId;
    }

    @Override
    public Class<OperationStatusResponse> responseType() {
        return OperationStatusResponse.class;
    }
}
