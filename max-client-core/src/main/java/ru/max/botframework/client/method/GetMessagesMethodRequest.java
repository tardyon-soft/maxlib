package ru.max.botframework.client.method;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import ru.max.botframework.client.MaxRequest;
import ru.max.botframework.client.http.HttpMethod;
import ru.max.botframework.model.ChatId;
import ru.max.botframework.model.MessageId;
import ru.max.botframework.model.response.MessagesResponse;

/**
 * Domain-level request for GET /messages with marker/list filters.
 */
public final class GetMessagesMethodRequest implements MaxRequest<MessagesResponse> {
    private final ChatId chatId;
    private final List<MessageId> messageIds;

    public GetMessagesMethodRequest(ChatId chatId) {
        this.chatId = Objects.requireNonNull(chatId, "chatId");
        this.messageIds = List.of();
    }

    public GetMessagesMethodRequest(List<MessageId> messageIds) {
        Objects.requireNonNull(messageIds, "messageIds");
        if (messageIds.isEmpty()) {
            throw new IllegalArgumentException("messageIds must not be empty");
        }
        this.chatId = null;
        this.messageIds = List.copyOf(messageIds);
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.GET;
    }

    @Override
    public String path() {
        return "/messages";
    }

    @Override
    public Class<MessagesResponse> responseType() {
        return MessagesResponse.class;
    }

    @Override
    public Map<String, String> queryParameters() {
        if (chatId != null) {
            return Map.of("chat_id", chatId.value());
        }
        StringJoiner joiner = new StringJoiner(",");
        for (MessageId messageId : messageIds) {
            joiner.add(messageId.value());
        }
        return Map.of("message_ids", joiner.toString());
    }
}
