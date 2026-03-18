package ru.tardyon.botframework.client.method;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import ru.tardyon.botframework.client.MaxRequest;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.model.request.RemoveChatMemberApiRequest;
import ru.tardyon.botframework.model.response.OperationStatusResponse;

/**
 * Docs-shaped request for DELETE /chats/{chatId}/members.
 */
public final class RemoveChatMemberApiMethodRequest implements MaxRequest<OperationStatusResponse> {
    private final long chatId;
    private final RemoveChatMemberApiRequest request;

    public RemoveChatMemberApiMethodRequest(long chatId, RemoveChatMemberApiRequest request) {
        this.chatId = chatId;
        this.request = Objects.requireNonNull(request, "request");
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.DELETE;
    }

    @Override
    public String path() {
        return "/chats/" + chatId + "/members";
    }

    @Override
    public Class<OperationStatusResponse> responseType() {
        return OperationStatusResponse.class;
    }

    @Override
    public Map<String, String> queryParameters() {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("user_id", String.valueOf(request.userId()));
        if (request.block() != null) {
            query.put("block", String.valueOf(request.block()));
        }
        return Map.copyOf(query);
    }
}
