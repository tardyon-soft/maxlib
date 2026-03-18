package ru.tardyon.botframework.client.method;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import ru.tardyon.botframework.client.MaxRequest;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.model.mapping.MaxApiModelMapper;
import ru.tardyon.botframework.model.request.EditMessageRequest;
import ru.tardyon.botframework.model.response.OperationStatusResponse;

/**
 * Domain-level request for PUT /messages.
 */
public final class EditMessageMethodRequest implements MaxRequest<OperationStatusResponse> {
    private final EditMessageRequest request;

    public EditMessageMethodRequest(EditMessageRequest request) {
        this.request = Objects.requireNonNull(request, "request");
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.PUT;
    }

    @Override
    public String path() {
        return "/messages";
    }

    @Override
    public Class<OperationStatusResponse> responseType() {
        return OperationStatusResponse.class;
    }

    @Override
    public Optional<Object> body() {
        return Optional.of(MaxApiModelMapper.toApiOutgoing(request.body(), request.sendNotification(), null));
    }

    @Override
    public Map<String, String> queryParameters() {
        return Map.of("message_id", request.messageId().value());
    }

}
