package ru.max.botframework.client.method;

import java.util.Objects;
import java.util.Optional;
import ru.max.botframework.client.MaxRequest;
import ru.max.botframework.client.http.HttpMethod;
import ru.max.botframework.model.request.CreateSubscriptionRequest;
import ru.max.botframework.model.response.OperationStatusResponse;

/**
 * Domain-level request for POST /subscriptions.
 */
public final class CreateSubscriptionMethodRequest implements MaxRequest<OperationStatusResponse> {
    private final CreateSubscriptionRequest request;

    public CreateSubscriptionMethodRequest(CreateSubscriptionRequest request) {
        this.request = Objects.requireNonNull(request, "request");
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.POST;
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
    public Optional<Object> body() {
        return Optional.of(request);
    }
}
