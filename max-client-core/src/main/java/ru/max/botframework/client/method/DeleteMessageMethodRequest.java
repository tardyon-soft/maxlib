package ru.max.botframework.client.method;

import java.util.Map;
import java.util.Objects;
import ru.max.botframework.client.MaxRequest;
import ru.max.botframework.client.http.HttpMethod;
import ru.max.botframework.model.MessageId;
import ru.max.botframework.model.response.OperationStatusResponse;

/**
 * Domain-level request for DELETE /messages.
 */
public final class DeleteMessageMethodRequest implements MaxRequest<OperationStatusResponse> {
    private final MessageId messageId;

    public DeleteMessageMethodRequest(MessageId messageId) {
        this.messageId = Objects.requireNonNull(messageId, "messageId");
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.DELETE;
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
    public Map<String, String> queryParameters() {
        return Map.of("message_id", messageId.value());
    }
}
