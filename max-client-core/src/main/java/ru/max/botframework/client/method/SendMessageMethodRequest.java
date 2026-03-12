package ru.max.botframework.client.method;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import ru.max.botframework.client.MaxRequest;
import ru.max.botframework.client.http.HttpMethod;
import ru.max.botframework.model.MessageId;
import ru.max.botframework.model.TextFormat;
import ru.max.botframework.model.request.NewMessageAttachment;
import ru.max.botframework.model.request.SendMessageRequest;
import ru.max.botframework.model.response.MessageResponse;

/**
 * Domain-level request for POST /messages.
 */
public final class SendMessageMethodRequest implements MaxRequest<MessageResponse> {
    private final SendMessageRequest request;

    public SendMessageMethodRequest(SendMessageRequest request) {
        this.request = Objects.requireNonNull(request, "request");
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.POST;
    }

    @Override
    public String path() {
        return "/messages";
    }

    @Override
    public Class<MessageResponse> responseType() {
        return MessageResponse.class;
    }

    @Override
    public Optional<Object> body() {
        return Optional.of(new SendOrEditMessageBody(
                request.body().text(),
                request.body().format(),
                request.sendNotification(),
                request.body().attachments(),
                request.replyToMessageId()
        ));
    }

    @Override
    public Map<String, String> queryParameters() {
        return Map.of("chat_id", request.chatId().value());
    }

    private record SendOrEditMessageBody(
            String text,
            TextFormat format,
            @JsonProperty("notify") Boolean sendNotification,
            java.util.List<NewMessageAttachment> attachments,
            MessageId replyToMessageId
    ) {
    }
}
