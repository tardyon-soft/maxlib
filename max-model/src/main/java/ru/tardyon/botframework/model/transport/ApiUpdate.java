package ru.tardyon.botframework.model.transport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MAX API transport update shape.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiUpdate(
        @JsonProperty("update_type") String updateType,
        @JsonProperty("timestamp") Long timestamp,
        @JsonProperty("message_id") String messageId,
        @JsonProperty("chat_id") Long chatId,
        @JsonProperty("user_id") Long userId,
        @JsonProperty("message") ApiMessage message,
        @JsonProperty("callback") ApiCallback callback,
        @JsonProperty("chat_member") ApiChatMember chatMember,
        @JsonProperty("user") ApiUser user,
        @JsonProperty("is_channel") Boolean isChannel,
        @JsonProperty("payload") String payload,
        @JsonProperty("title") String title,
        @JsonProperty("user_locale") String userLocale
) {
    public ApiUpdate(
            String updateType,
            Long timestamp,
            Long chatId,
            ApiMessage message,
            ApiCallback callback,
            ApiChatMember chatMember,
            ApiUser user,
            Boolean isChannel,
            String userLocale
    ) {
        this(updateType, timestamp, null, chatId, null, message, callback, chatMember, user, isChannel, null, null, userLocale);
    }

    public ApiUpdate(
            String updateType,
            Long timestamp,
            String messageId,
            Long chatId,
            Long userId,
            ApiMessage message,
            ApiCallback callback,
            ApiChatMember chatMember,
            ApiUser user,
            Boolean isChannel,
            String userLocale
    ) {
        this(updateType, timestamp, messageId, chatId, userId, message, callback, chatMember, user, isChannel, null, null,
                userLocale);
    }
}
