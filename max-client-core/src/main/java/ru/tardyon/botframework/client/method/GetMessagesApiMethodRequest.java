package ru.tardyon.botframework.client.method;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import ru.tardyon.botframework.client.MaxRequest;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.model.request.GetMessagesApiRequest;
import ru.tardyon.botframework.model.response.MessagesResponse;

/**
 * Docs-shaped request for GET /messages.
 */
public final class GetMessagesApiMethodRequest implements MaxRequest<MessagesResponse> {
    private final GetMessagesApiRequest request;

    public GetMessagesApiMethodRequest(GetMessagesApiRequest request) {
        this.request = Objects.requireNonNull(request, "request");
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.GET;
    }

    @Override
    public String path() {
        return "/messages";
    }

    @Override
    public Class<MessagesResponse> responseType() {
        return MessagesResponse.class;
    }

    @Override
    public Map<String, String> queryParameters() {
        Map<String, String> query = new LinkedHashMap<>();
        if (request.chatId() != null) {
            query.put("chat_id", String.valueOf(request.chatId()));
        }
        if (request.from() != null) {
            query.put("from", String.valueOf(request.from()));
        }
        if (request.to() != null) {
            query.put("to", String.valueOf(request.to()));
        }
        if (request.count() != null) {
            query.put("count", String.valueOf(request.count()));
        }
        return Map.copyOf(query);
    }
}
