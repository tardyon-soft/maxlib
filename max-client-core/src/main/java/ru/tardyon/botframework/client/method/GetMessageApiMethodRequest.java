package ru.tardyon.botframework.client.method;

import java.util.Objects;
import ru.tardyon.botframework.client.MaxRequest;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.model.MessageId;
import ru.tardyon.botframework.model.transport.ApiMessage;

/**
 * Docs-shaped request for GET /messages/{messageId}.
 */
public final class GetMessageApiMethodRequest implements MaxRequest<ApiMessage> {
    private final MessageId messageId;

    public GetMessageApiMethodRequest(MessageId messageId) {
        this.messageId = Objects.requireNonNull(messageId, "messageId");
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.GET;
    }

    @Override
    public String path() {
        return "/messages/" + messageId.value();
    }

    @Override
    public Class<ApiMessage> responseType() {
        return ApiMessage.class;
    }
}
