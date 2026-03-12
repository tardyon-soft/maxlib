package ru.max.botframework.message;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import ru.max.botframework.model.ChatId;
import ru.max.botframework.model.TextFormat;
import ru.max.botframework.model.request.NewMessageAttachment;
import ru.max.botframework.model.request.NewMessageBody;
import ru.max.botframework.model.request.SendMessageRequest;

/**
 * Immutable high-level builder for outgoing message payload.
 */
public final class MessageBuilder {
    private final String text;
    private final boolean notify;
    private final TextFormat format;
    private final String link;
    private final List<NewMessageAttachment> attachments;
    private final KeyboardMarkup keyboard;

    private MessageBuilder(
            String text,
            boolean notify,
            TextFormat format,
            String link,
            List<NewMessageAttachment> attachments,
            KeyboardMarkup keyboard
    ) {
        this.text = text;
        this.notify = notify;
        this.format = Objects.requireNonNull(format, "format");
        this.link = link;
        this.attachments = List.copyOf(attachments);
        this.keyboard = keyboard;
    }

    static MessageBuilder empty() {
        return new MessageBuilder(null, true, TextFormat.PLAIN, null, List.of(), null);
    }

    static MessageBuilder textOnly(String text) {
        return empty().text(text);
    }

    public MessageBuilder text(String value) {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        return new MessageBuilder(value, notify, format, link, attachments, keyboard);
    }

    public MessageBuilder notify(boolean value) {
        return new MessageBuilder(text, value, format, link, attachments, keyboard);
    }

    public MessageBuilder format(TextFormat value) {
        return new MessageBuilder(text, notify, Objects.requireNonNull(value, "value"), link, attachments, keyboard);
    }

    public MessageBuilder link(String value) {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("link must not be blank");
        }
        return new MessageBuilder(text, notify, format, value, attachments, keyboard);
    }

    public MessageBuilder attachment(NewMessageAttachment value) {
        Objects.requireNonNull(value, "value");
        ArrayList<NewMessageAttachment> merged = new ArrayList<>(attachments);
        merged.add(value);
        return new MessageBuilder(text, notify, format, link, merged, keyboard);
    }

    public MessageBuilder attachments(List<NewMessageAttachment> values) {
        Objects.requireNonNull(values, "values");
        ArrayList<NewMessageAttachment> merged = new ArrayList<>(attachments);
        merged.addAll(values);
        return new MessageBuilder(text, notify, format, link, merged, keyboard);
    }

    public MessageBuilder keyboard(KeyboardMarkup value) {
        return new MessageBuilder(text, notify, format, link, attachments, Objects.requireNonNull(value, "value"));
    }

    public Optional<String> text() {
        return Optional.ofNullable(text);
    }

    public boolean notify() {
        return notify;
    }

    public TextFormat format() {
        return format;
    }

    public Optional<String> link() {
        return Optional.ofNullable(link);
    }

    public List<NewMessageAttachment> attachments() {
        return attachments;
    }

    /**
     * Returns keyboard integration point object, if configured.
     */
    public Optional<KeyboardMarkup> keyboard() {
        return Optional.ofNullable(keyboard);
    }

    public NewMessageBody toNewMessageBody() {
        return new NewMessageBody(composeText(), format, attachments);
    }

    public SendMessageRequest toSendRequest(ChatId chatId) {
        return new SendMessageRequest(
                Objects.requireNonNull(chatId, "chatId"),
                toNewMessageBody(),
                notify,
                null
        );
    }

    public SendMessageRequest toSendRequest(MessageTarget target, MessageTarget.UserChatResolver resolver) {
        Objects.requireNonNull(target, "target");
        return toSendRequest(target.toChatId(resolver));
    }

    private String composeText() {
        if (text == null && link == null) {
            return null;
        }
        if (text == null) {
            return link;
        }
        if (link == null) {
            return text;
        }
        return text + System.lineSeparator() + link;
    }
}
