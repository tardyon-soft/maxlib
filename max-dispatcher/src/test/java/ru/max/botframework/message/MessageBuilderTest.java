package ru.max.botframework.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.max.botframework.model.ChatId;
import ru.max.botframework.model.MessageAttachmentType;
import ru.max.botframework.model.TextFormat;
import ru.max.botframework.model.UserId;
import ru.max.botframework.model.request.AttachmentInput;
import ru.max.botframework.model.request.NewMessageAttachment;
import ru.max.botframework.model.request.NewMessageBody;
import ru.max.botframework.model.request.SendMessageRequest;

class MessageBuilderTest {

    @Test
    void messagesTextCreatesBuilderWithDefaults() {
        MessageBuilder builder = Messages.text("hello");

        assertEquals("hello", builder.text().orElseThrow());
        assertTrue(builder.notify());
        assertEquals(TextFormat.PLAIN, builder.format());
        assertTrue(builder.attachments().isEmpty());
    }

    @Test
    void builderIsImmutable() {
        MessageBuilder base = Messages.text("hello");
        MessageBuilder changed = base.notify(false).format(TextFormat.MARKDOWN);

        assertNotSame(base, changed);
        assertTrue(base.notify());
        assertEquals(TextFormat.PLAIN, base.format());
        assertEquals(TextFormat.MARKDOWN, changed.format());
    }

    @Test
    void mapsTextNotifyFormatAndLinkToBody() {
        MessageBuilder builder = Messages.text("hello")
                .format(TextFormat.HTML)
                .notify(false)
                .link("https://example.com");

        NewMessageBody body = builder.toNewMessageBody();

        assertEquals("hello" + System.lineSeparator() + "https://example.com", body.text());
        assertEquals(TextFormat.HTML, body.format());
        assertTrue(body.attachments().isEmpty());
    }

    @Test
    void supportsAttachmentsExtensionPointInBodyMapping() {
        NewMessageAttachment attachment = new NewMessageAttachment(
                MessageAttachmentType.FILE,
                new AttachmentInput(null, "upload-ref-1", null),
                "doc",
                "application/pdf",
                1024L
        );
        MessageBuilder builder = Messages.text("with attachment").attachment(attachment);

        NewMessageBody body = builder.toNewMessageBody();

        assertEquals(1, body.attachments().size());
        assertEquals(MessageAttachmentType.FILE, body.attachments().getFirst().type());
        assertEquals("upload-ref-1", body.attachments().getFirst().input().uploadRef());
    }

    @Test
    void keepsKeyboardIntegrationPointWithoutAffectingLowLevelBody() {
        KeyboardMarkup keyboard = new KeyboardMarkup() {
        };
        MessageBuilder builder = Messages.text("hello").keyboard(keyboard);

        assertTrue(builder.keyboard().isPresent());
        assertEquals("hello", builder.toNewMessageBody().text());
    }

    @Test
    void mapsBuilderToLowLevelSendRequestForChatTarget() {
        SendMessageRequest request = Messages.text("hello")
                .notify(false)
                .toSendRequest(new ChatId("chat-1"));

        assertEquals("chat-1", request.chatId().value());
        assertEquals("hello", request.body().text());
        assertEquals(Boolean.FALSE, request.sendNotification());
    }

    @Test
    void mapsBuilderToLowLevelSendRequestForUserTargetViaResolver() {
        MessageTarget target = MessageTarget.user(new UserId("user-1"));
        SendMessageRequest request = Messages.text("hello")
                .toSendRequest(target, userId -> new ChatId("chat-for-" + userId.value()));

        assertEquals("chat-for-user-1", request.chatId().value());
        assertEquals("hello", request.body().text());
    }

    @Test
    void attachmentsOnlyMessageCanBeBuiltFromMessagesMessage() {
        MessageBuilder builder = Messages.message().attachment(new NewMessageAttachment(
                MessageAttachmentType.FILE,
                new AttachmentInput(null, null, "https://example.com/file.pdf"),
                null,
                null,
                null
        ));

        NewMessageBody body = builder.toNewMessageBody();

        assertEquals(1, body.attachments().size());
        assertEquals(TextFormat.PLAIN, body.format());
    }

    @Test
    void emptyMessageWithoutTextOrAttachmentsIsRejectedByLowLevelBodyValidation() {
        MessageBuilder empty = Messages.message();

        assertThrows(IllegalArgumentException.class, empty::toNewMessageBody);
    }

    @Test
    void rejectsBlankTextAndLink() {
        assertThrows(IllegalArgumentException.class, () -> Messages.text(" "));
        assertThrows(IllegalArgumentException.class, () -> Messages.message().link(" "));
    }

    @Test
    void attachmentsMethodAppendsAllValues() {
        NewMessageAttachment first = new NewMessageAttachment(
                MessageAttachmentType.FILE,
                new AttachmentInput(null, "ref-1", null),
                null,
                null,
                null
        );
        NewMessageAttachment second = new NewMessageAttachment(
                MessageAttachmentType.FILE,
                new AttachmentInput(null, "ref-2", null),
                null,
                null,
                null
        );

        MessageBuilder builder = Messages.text("hello").attachments(List.of(first, second));

        assertEquals(2, builder.attachments().size());
    }
}
