package ru.tardyon.botframework.client.method;

import java.util.Map;
import java.util.Objects;
import ru.tardyon.botframework.client.MaxRequest;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.model.request.GetUpdatesRequest;
import ru.tardyon.botframework.model.transport.ApiGetUpdatesResponse;

/**
 * Transport-level request for GET /updates in documented MAX API shape.
 */
public final class GetUpdatesApiMethodRequest implements MaxRequest<ApiGetUpdatesResponse> {
    private final GetUpdatesRequest request;

    public GetUpdatesApiMethodRequest(GetUpdatesRequest request) {
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
    public Class<ApiGetUpdatesResponse> responseType() {
        return ApiGetUpdatesResponse.class;
    }

    @Override
    public Map<String, String> queryParameters() {
        return new GetUpdatesMethodRequest(request).queryParameters();
    }
}
