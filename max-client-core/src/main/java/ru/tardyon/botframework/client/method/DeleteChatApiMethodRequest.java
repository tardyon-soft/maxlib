package ru.tardyon.botframework.client.method;

import ru.tardyon.botframework.client.MaxRequest;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.model.response.OperationStatusResponse;

/**
 * Docs-shaped request for DELETE /chats/{chatId}.
 */
public final class DeleteChatApiMethodRequest implements MaxRequest<OperationStatusResponse> {
    private final long chatId;

    public DeleteChatApiMethodRequest(long chatId) {
        this.chatId = chatId;
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.DELETE;
    }

    @Override
    public String path() {
        return "/chats/" + chatId;
    }

    @Override
    public Class<OperationStatusResponse> responseType() {
        return OperationStatusResponse.class;
    }
}
