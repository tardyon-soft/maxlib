package ru.max.botframework.client.method;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import ru.max.botframework.client.MaxRequest;
import ru.max.botframework.client.http.HttpMethod;
import ru.max.botframework.model.UpdateEventType;
import ru.max.botframework.model.request.GetUpdatesRequest;
import ru.max.botframework.model.response.GetUpdatesResponse;

/**
 * Domain-level request for GET /updates.
 */
public final class GetUpdatesMethodRequest implements MaxRequest<GetUpdatesResponse> {
    private final GetUpdatesRequest request;

    public GetUpdatesMethodRequest(GetUpdatesRequest request) {
        this.request = Objects.requireNonNull(request, "request");
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.GET;
    }

    @Override
    public String path() {
        return "/updates";
    }

    @Override
    public Class<GetUpdatesResponse> responseType() {
        return GetUpdatesResponse.class;
    }

    @Override
    public Map<String, String> queryParameters() {
        Map<String, String> query = new LinkedHashMap<>();
        if (request.marker() != null) {
            query.put("marker", String.valueOf(request.marker()));
        }
        if (request.timeout() != null) {
            query.put("timeout", String.valueOf(request.timeout()));
        }
        if (request.limit() != null) {
            query.put("limit", String.valueOf(request.limit()));
        }
        if (!request.types().isEmpty()) {
            StringJoiner types = new StringJoiner(",");
            for (UpdateEventType type : request.types()) {
                types.add(type.value());
            }
            query.put("types", types.toString());
        }
        return Map.copyOf(query);
    }
}
