package ru.tardyon.botframework.client.method;

import ru.tardyon.botframework.client.MaxRequest;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.model.transport.ApiUser;

/**
 * Docs-shaped request for GET /me.
 */
public final class GetMeApiMethodRequest implements MaxRequest<ApiUser> {
    public static final GetMeApiMethodRequest INSTANCE = new GetMeApiMethodRequest();

    private GetMeApiMethodRequest() {
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.GET;
    }

    @Override
    public String path() {
        return "/me";
    }

    @Override
    public Class<ApiUser> responseType() {
        return ApiUser.class;
    }
}
