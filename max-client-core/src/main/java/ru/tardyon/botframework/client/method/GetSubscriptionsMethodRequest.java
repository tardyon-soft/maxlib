package ru.tardyon.botframework.client.method;

import ru.tardyon.botframework.client.MaxRequest;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.model.response.SubscriptionsResponse;

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
