package ru.tardyon.botframework.client.method;

import ru.tardyon.botframework.client.MaxRequest;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.model.BotInfo;

/**
 * Domain-level request for GET /me.
 */
public final class GetMeRequest implements MaxRequest<BotInfo> {
    public static final GetMeRequest INSTANCE = new GetMeRequest();

    private GetMeRequest() {
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
    public Class<BotInfo> responseType() {
        return BotInfo.class;
    }
}
