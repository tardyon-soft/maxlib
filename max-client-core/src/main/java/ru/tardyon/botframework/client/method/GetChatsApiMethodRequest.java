package ru.tardyon.botframework.client.method;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import ru.tardyon.botframework.client.MaxRequest;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.model.request.GetChatsApiRequest;
import ru.tardyon.botframework.model.transport.ApiChatsResponse;

/**
 * Docs-shaped request for GET /chats.
 */
public final class GetChatsApiMethodRequest implements MaxRequest<ApiChatsResponse> {
    private final GetChatsApiRequest request;

    public GetChatsApiMethodRequest(GetChatsApiRequest request) {
        this.request = Objects.requireNonNull(request, "request");
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.GET;
    }

    @Override
    public String path() {
        return "/chats";
    }

    @Override
    public Class<ApiChatsResponse> responseType() {
        return ApiChatsResponse.class;
    }

    @Override
    public Map<String, String> queryParameters() {
        Map<String, String> query = new LinkedHashMap<>();
        if (request.marker() != null) {
            query.put("marker", String.valueOf(request.marker()));
        }
        if (request.count() != null) {
            query.put("count", String.valueOf(request.count()));
        }
        return Map.copyOf(query);
    }
}
