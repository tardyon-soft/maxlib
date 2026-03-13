package ru.max.botframework.message;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;
import ru.max.botframework.model.ChatId;
import ru.max.botframework.model.MessageId;
import ru.max.botframework.model.TextFormat;
import ru.max.botframework.model.request.EditMessageRequest;
import ru.max.botframework.model.request.NewMessageAttachment;
import ru.max.botframework.model.request.NewMessageBody;
import ru.max.botframework.model.request.SendMessageRequest;

/**
 * Immutable high-level builder for outgoing message payload.
 *
 * <p>Builder stays transport-agnostic until explicit mapping via
 * {@link #toNewMessageBody()}, {@link #toSendRequest(ChatId)} or {@link #toEditRequest(ChatId, MessageId)}.</p>
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

    public MessageBuilder plain() {
        return format(TextFormat.PLAIN);
    }

    public MessageBuilder markdown() {
        return format(TextFormat.MARKDOWN);
    }

    public MessageBuilder html() {
        return format(TextFormat.HTML);
    }

    public MessageBuilder link(String value) {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("link must not be blank");
        }
        return new MessageBuilder(text, notify, format, value, attachments, keyboard);
    }

    /**
     * Appends low-level attachment model as extension point for current Sprint 6 surface.
     */
    public MessageBuilder attachment(NewMessageAttachment value) {
        Objects.requireNonNull(value, "value");
        ArrayList<NewMessageAttachment> merged = new ArrayList<>(attachments);
        merged.add(value);
        return new MessageBuilder(text, notify, format, link, merged, keyboard);
    }

    /**
     * Appends low-level attachment models as extension point for current Sprint 6 surface.
     */
    public MessageBuilder attachments(List<NewMessageAttachment> values) {
        Objects.requireNonNull(values, "values");
        ArrayList<NewMessageAttachment> merged = new ArrayList<>(attachments);
        merged.addAll(values);
        return new MessageBuilder(text, notify, format, link, merged, keyboard);
    }

    /**
     * Assigns pre-built keyboard markup.
     */
    public MessageBuilder keyboard(KeyboardMarkup value) {
        return new MessageBuilder(text, notify, format, link, attachments, Objects.requireNonNull(value, "value"));
    }

    /**
     * Declarative inline keyboard configuration shortcut.
     */
    public MessageBuilder keyboard(UnaryOperator<KeyboardBuilder> spec) {
        Objects.requireNonNull(spec, "spec");
        return keyboard(Keyboards.inline(spec));
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

    /**
     * Maps builder to low-level body DTO used by send/edit SDK requests.
     */
    public NewMessageBody toNewMessageBody() {
        return new NewMessageBody(composeText(), format, composeAttachments());
    }

    /**
     * Maps builder to low-level send request without reply context.
     */
    public SendMessageRequest toSendRequest(ChatId chatId) {
        return toSendRequest(chatId, null);
    }

    /**
     * Maps builder to low-level send request with optional reply context.
     */
    public SendMessageRequest toSendRequest(ChatId chatId, MessageId replyToMessageId) {
        return new SendMessageRequest(
                Objects.requireNonNull(chatId, "chatId"),
                toNewMessageBody(),
                notify,
                replyToMessageId
        );
    }

    /**
     * Maps builder to low-level send request with {@link MessageTarget} resolution.
     */
    public SendMessageRequest toSendRequest(MessageTarget target, MessageTarget.UserChatResolver resolver) {
        Objects.requireNonNull(target, "target");
        return toSendRequest(target.toChatId(resolver));
    }

    /**
     * Maps builder to low-level edit request.
     */
    public EditMessageRequest toEditRequest(ChatId chatId, MessageId messageId) {
        return new EditMessageRequest(
                Objects.requireNonNull(chatId, "chatId"),
                Objects.requireNonNull(messageId, "messageId"),
                toNewMessageBody(),
                notify
        );
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

    private List<NewMessageAttachment> composeAttachments() {
        if (keyboard instanceof InlineKeyboard inlineKeyboard) {
            ArrayList<NewMessageAttachment> merged = new ArrayList<>(attachments);
            merged.add(inlineKeyboard.toAttachment());
            return List.copyOf(merged);
        }
        return attachments;
    }
}
