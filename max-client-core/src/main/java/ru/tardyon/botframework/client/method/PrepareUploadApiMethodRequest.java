package ru.tardyon.botframework.client.method;

import java.util.Objects;
import java.util.Optional;
import ru.tardyon.botframework.client.MaxRequest;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.model.request.PrepareUploadApiRequest;
import ru.tardyon.botframework.model.transport.ApiUploadResponse;

/**
 * Docs-shaped request for POST /uploads.
 */
public final class PrepareUploadApiMethodRequest implements MaxRequest<ApiUploadResponse> {
    private final PrepareUploadApiRequest request;

    public PrepareUploadApiMethodRequest(PrepareUploadApiRequest request) {
        this.request = Objects.requireNonNull(request, "request");
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.POST;
    }

    @Override
    public String path() {
        return "/uploads";
    }

    @Override
    public Class<ApiUploadResponse> responseType() {
        return ApiUploadResponse.class;
    }

    @Override
    public Optional<Object> body() {
        return Optional.of(request);
    }
}
