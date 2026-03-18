package ru.tardyon.botframework.client.method;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import ru.tardyon.botframework.client.MaxRequest;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.model.request.GetChatMembersApiRequest;
import ru.tardyon.botframework.model.transport.ApiChatMembersResponse;

/**
 * Docs-shaped request for GET /chats/{chatId}/members.
 */
public final class GetChatMembersApiMethodRequest implements MaxRequest<ApiChatMembersResponse> {
    private final long chatId;
    private final GetChatMembersApiRequest request;

    public GetChatMembersApiMethodRequest(long chatId, GetChatMembersApiRequest request) {
        this.chatId = chatId;
        this.request = Objects.requireNonNull(request, "request");
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.GET;
    }

    @Override
    public String path() {
        return "/chats/" + chatId + "/members";
    }

    @Override
    public Class<ApiChatMembersResponse> responseType() {
        return ApiChatMembersResponse.class;
    }

    @Override
    public Map<String, String> queryParameters() {
        Map<String, String> query = new LinkedHashMap<>();
        if (!request.userIds().isEmpty()) {
            StringJoiner joiner = new StringJoiner(",");
            for (Long id : request.userIds()) {
                joiner.add(String.valueOf(id));
            }
            query.put("user_ids", joiner.toString());
        }
        if (request.marker() != null) {
            query.put("marker", String.valueOf(request.marker()));
        }
        if (request.count() != null) {
            query.put("count", String.valueOf(request.count()));
        }
        return Map.copyOf(query);
    }
}
