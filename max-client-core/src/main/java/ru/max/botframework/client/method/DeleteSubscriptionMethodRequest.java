package ru.max.botframework.client.method;

import java.util.Map;
import java.util.Objects;
import ru.max.botframework.client.MaxRequest;
import ru.max.botframework.client.http.HttpMethod;
import ru.max.botframework.model.request.DeleteSubscriptionRequest;
import ru.max.botframework.model.response.OperationStatusResponse;

/**
 * Domain-level request for DELETE /subscriptions.
 */
public final class DeleteSubscriptionMethodRequest implements MaxRequest<OperationStatusResponse> {
    private final DeleteSubscriptionRequest request;

    public DeleteSubscriptionMethodRequest(DeleteSubscriptionRequest request) {
        this.request = Objects.requireNonNull(request, "request");
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.DELETE;
    }

    @Override
    public String path() {
        return "/subscriptions";
    }

    @Override
    public Class<OperationStatusResponse> responseType() {
        return OperationStatusResponse.class;
    }

    @Override
    public Map<String, String> queryParameters() {
        return Map.of("url", request.url());
    }
}
