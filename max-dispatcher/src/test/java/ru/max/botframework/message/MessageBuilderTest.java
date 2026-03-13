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
import ru.max.botframework.upload.UploadFlowType;
import ru.max.botframework.upload.UploadMediaKind;
import ru.max.botframework.upload.UploadRef;
import ru.max.botframework.upload.UploadResult;

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
    void markdownAndHtmlConvenienceMethodsMapToLowLevelFormat() {
        NewMessageBody markdownBody = Messages.text("*hello*").markdown().toNewMessageBody();
        NewMessageBody htmlBody = Messages.text("<b>hello</b>").html().toNewMessageBody();

        assertEquals(TextFormat.MARKDOWN, markdownBody.format());
        assertEquals("*hello*", markdownBody.text());
        assertEquals(TextFormat.HTML, htmlBody.format());
        assertEquals("<b>hello</b>", htmlBody.text());
    }

    @Test
    void messagesFactoryFormattingEntrypointsSetExpectedFormat() {
        SendMessageRequest markdownRequest = Messages.markdown("*hello*")
                .toSendRequest(new ChatId("chat-md"));
        SendMessageRequest htmlRequest = Messages.html("<b>hello</b>")
                .toSendRequest(new ChatId("chat-html"));
        SendMessageRequest plainRequest = Messages.plain("hello")
                .toSendRequest(new ChatId("chat-plain"));

        assertEquals(TextFormat.MARKDOWN, markdownRequest.body().format());
        assertEquals(TextFormat.HTML, htmlRequest.body().format());
        assertEquals(TextFormat.PLAIN, plainRequest.body().format());
    }

    @Test
    void supportsAttachmentsExtensionPointInBodyMapping() {
        NewMessageAttachment attachment = NewMessageAttachment.media(
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
    void mapsInlineKeyboardToLowLevelInlineKeyboardAttachment() {
        MessageBuilder builder = Messages.text("hello")
                .keyboard(k -> k.row(
                        Buttons.callback("Pay", "pay:1"),
                        Buttons.link("Site", "https://example.com")
                ));

        assertTrue(builder.keyboard().isPresent());
        NewMessageBody body = builder.toNewMessageBody();
        assertEquals("hello", body.text());
        assertEquals(1, body.attachments().size());
        assertEquals(MessageAttachmentType.INLINE_KEYBOARD, body.attachments().getFirst().type());
        assertEquals(1, body.attachments().getFirst().inlineKeyboard().rows().size());
        assertEquals("pay:1", body.attachments().getFirst().inlineKeyboard().rows().getFirst().getFirst().callbackData());
        assertEquals("https://example.com", body.attachments().getFirst().inlineKeyboard().rows().getFirst().get(1).url());
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
        MessageBuilder builder = Messages.message().attachment(NewMessageAttachment.media(
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
                null,
                null
        );
        NewMessageAttachment second = new NewMessageAttachment(
                MessageAttachmentType.FILE,
                new AttachmentInput(null, "ref-2", null),
                null,
                null,
                null,
                null
        );

        MessageBuilder builder = Messages.text("hello").attachments(List.of(first, second));

        assertEquals(2, builder.attachments().size());
    }

    @Test
    void composesTextMediaAttachmentAndKeyboardTogether() {
        UploadResult uploaded = new UploadResult(
                new UploadRef("ref-media-1"),
                UploadFlowType.MULTIPART,
                42L,
                "image/png",
                UploadMediaKind.IMAGE,
                java.util.Map.of()
        );

        NewMessageBody body = Messages.text("hello")
                .attachment(MediaAttachment.image(uploaded).caption("preview"))
                .keyboard(k -> k.row(Buttons.callback("Open", "open:1")))
                .toNewMessageBody();

        assertEquals("hello", body.text());
        assertEquals(2, body.attachments().size());
        assertEquals(MessageAttachmentType.PHOTO, body.attachments().getFirst().type());
        assertEquals("ref-media-1", body.attachments().getFirst().input().uploadRef());
        assertEquals(MessageAttachmentType.INLINE_KEYBOARD, body.attachments().get(1).type());
    }
}
