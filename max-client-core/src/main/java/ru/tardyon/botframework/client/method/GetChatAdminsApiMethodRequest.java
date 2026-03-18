package ru.tardyon.botframework.client.method;

import ru.tardyon.botframework.client.MaxRequest;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.model.transport.ApiChatMembersResponse;

/**
 * Docs-shaped request for GET /chats/{chatId}/members/admins.
 */
public final class GetChatAdminsApiMethodRequest implements MaxRequest<ApiChatMembersResponse> {
    private final long chatId;

    public GetChatAdminsApiMethodRequest(long chatId) {
        this.chatId = chatId;
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.GET;
    }

    @Override
    public String path() {
        return "/chats/" + chatId + "/members/admins";
    }

    @Override
    public Class<ApiChatMembersResponse> responseType() {
        return ApiChatMembersResponse.class;
    }
}
