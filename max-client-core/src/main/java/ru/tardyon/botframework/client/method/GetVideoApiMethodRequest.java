package ru.tardyon.botframework.client.method;

import java.util.Objects;
import ru.tardyon.botframework.client.MaxRequest;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.model.transport.ApiVideoInfo;

/**
 * Docs-shaped request for GET /videos/{videoToken}.
 */
public final class GetVideoApiMethodRequest implements MaxRequest<ApiVideoInfo> {
    private final String videoToken;

    public GetVideoApiMethodRequest(String videoToken) {
        this.videoToken = Objects.requireNonNull(videoToken, "videoToken");
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.GET;
    }

    @Override
    public String path() {
        return "/videos/" + videoToken;
    }

    @Override
    public Class<ApiVideoInfo> responseType() {
        return ApiVideoInfo.class;
    }
}
