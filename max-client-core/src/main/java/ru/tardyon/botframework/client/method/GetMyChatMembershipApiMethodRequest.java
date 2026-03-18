package ru.tardyon.botframework.client.method;

import ru.tardyon.botframework.client.MaxRequest;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.model.transport.ApiUser;

/**
 * Docs-shaped request for GET /chats/{chatId}/members/me.
 */
public final class GetMyChatMembershipApiMethodRequest implements MaxRequest<ApiUser> {
    private final long chatId;

    public GetMyChatMembershipApiMethodRequest(long chatId) {
        this.chatId = chatId;
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.GET;
    }

    @Override
    public String path() {
        return "/chats/" + chatId + "/members/me";
    }

    @Override
    public Class<ApiUser> responseType() {
        return ApiUser.class;
    }
}
