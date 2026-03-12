package ru.max.botframework.client.method;

import ru.max.botframework.client.MaxRequest;
import ru.max.botframework.client.http.HttpMethod;
import ru.max.botframework.model.response.SubscriptionsResponse;

/**
 * Domain-level request for GET /subscriptions.
 */
public final class GetSubscriptionsMethodRequest implements MaxRequest<SubscriptionsResponse> {
    public static final GetSubscriptionsMethodRequest INSTANCE = new GetSubscriptionsMethodRequest();

    private GetSubscriptionsMethodRequest() {
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.GET;
    }

    @Override
    public String path() {
        return "/subscriptions";
    }

    @Override
    public Class<SubscriptionsResponse> responseType() {
        return SubscriptionsResponse.class;
    }
}
