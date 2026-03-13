package ru.tardyon.botframework.client.method;

import java.util.Objects;
import ru.tardyon.botframework.client.MaxRequest;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.model.Message;
import ru.tardyon.botframework.model.MessageId;

/**
 * Domain-level request for GET /messages/{messageId}.
 */
public final class GetMessageMethodRequest implements MaxRequest<Message> {
    private final MessageId messageId;

    public GetMessageMethodRequest(MessageId messageId) {
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
    public Class<Message> responseType() {
        return Message.class;
    }
}
