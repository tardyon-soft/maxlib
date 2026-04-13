package ru.tardyon.botframework.demo.springpolling;

import ru.tardyon.botframework.dispatcher.RuntimeContext;
import ru.tardyon.botframework.dispatcher.annotation.Command;
import ru.tardyon.botframework.dispatcher.annotation.Message;
import ru.tardyon.botframework.dispatcher.annotation.Route;
import ru.tardyon.botframework.message.Buttons;
import ru.tardyon.botframework.message.Keyboards;
import ru.tardyon.botframework.message.MessageBuilder;
import ru.tardyon.botframework.message.Messages;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.MessageAttachmentType;
import ru.tardyon.botframework.model.request.AttachmentInput;
import ru.tardyon.botframework.model.request.NewMessageAttachment;

/**
 * Demo route with prepared channel post flow.
 */
@Route(value = "channel-post-route", autoRegister = true)
public final class ChannelPostRoute {

    @Command("post_help")
    public void postHelp(RuntimeContext ctx) {
        ctx.reply(Messages.text("""
                Post commands:
                /clipboard_demo - send clipboard button demo
                /post_channel <chat_id> [upload_ref] - send prepared post to channel/chat
                Example:
                /post_channel 123456789 f9LHodD0cOJ...
                """));
    }

    @Command("clipboard_demo")
    public void clipboardDemo(RuntimeContext ctx) {
        ctx.reply(Messages.text("Демо clipboard-кнопки")
                .keyboard(Keyboards.inline(k -> k.row(
                        Buttons.clipboard("Скопировать промокод", "PROMO-2026-APR")
                ))));
    }

    @Message(text = "/post_channel ", startsWith = true)
    public void postChannel(ru.tardyon.botframework.model.Message message, RuntimeContext ctx) {
        String text = message.text() == null ? "" : message.text();
        String args = text.substring("/post_channel ".length()).trim();
        if (args.isEmpty()) {
            ctx.reply(Messages.text("Usage: /post_channel <chat_id> [upload_ref]"));
            return;
        }

        String[] parts = args.split("\\s+", 2);
        String chatIdValue = parts[0].trim();
        String uploadRef = parts.length > 1 ? parts[1].trim() : null;
        if (chatIdValue.isEmpty()) {
            ctx.reply(Messages.text("chat_id is required"));
            return;
        }
        if (uploadRef != null && uploadRef.isBlank()) {
            uploadRef = null;
        }

        MessageBuilder post = Messages.markdown("""
                *Анонс релиза*

                Вышло обновление:
                • нативные annotation routes
                • screen stack + back navigation
                • clipboard-кнопки

                #release #maxbot
                """);

        if (uploadRef != null) {
            post = post.attachment(NewMessageAttachment.media(
                    MessageAttachmentType.PHOTO,
                    new AttachmentInput(null, uploadRef, null),
                    "Обложка поста",
                    null,
                    null
            ));
        }

        ru.tardyon.botframework.model.Message sent = ctx.messaging().send(new ChatId(chatIdValue), post);
        String messageId = sent != null && sent.messageId() != null ? sent.messageId().value() : "msg-unknown";
        ctx.reply(Messages.text("Пост отправлен в chat_id=" + chatIdValue + ", message_id=" + messageId));
    }
}
