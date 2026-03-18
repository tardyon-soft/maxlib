package ru.tardyon.botframework.client.method;

import ru.tardyon.botframework.client.MaxRequest;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.model.transport.ApiChatPinResponse;

/**
 * Docs-shaped request for GET /chats/{chatId}/pin.
 */
public final class GetChatPinApiMethodRequest implements MaxRequest<ApiChatPinResponse> {
    private final long chatId;

    public GetChatPinApiMethodRequest(long chatId) {
        this.chatId = chatId;
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.GET;
    }

    @Override
    public String path() {
        return "/chats/" + chatId + "/pin";
    }

    @Override
    public Class<ApiChatPinResponse> responseType() {
        return ApiChatPinResponse.class;
    }
}
