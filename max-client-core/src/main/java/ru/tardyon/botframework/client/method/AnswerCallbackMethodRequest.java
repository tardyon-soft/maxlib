package ru.tardyon.botframework.client.method;

import java.util.Objects;
import java.util.Optional;
import ru.tardyon.botframework.client.MaxRequest;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.model.request.AnswerCallbackRequest;
import ru.tardyon.botframework.model.response.OperationStatusResponse;

/**
 * Domain-level request for POST /answers callback operation.
 */
public final class AnswerCallbackMethodRequest implements MaxRequest<OperationStatusResponse> {
    private final AnswerCallbackRequest request;

    public AnswerCallbackMethodRequest(AnswerCallbackRequest request) {
        this.request = Objects.requireNonNull(request, "request");
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.POST;
    }

    @Override
    public String path() {
        return "/answers";
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
