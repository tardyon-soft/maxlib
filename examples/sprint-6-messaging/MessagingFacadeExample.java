package ru.max.botframework.examples.sprint6;

import ru.max.botframework.action.ChatActionsFacade;
import ru.max.botframework.callback.CallbackAnswers;
import ru.max.botframework.callback.CallbackContext;
import ru.max.botframework.callback.CallbackFacade;
import ru.max.botframework.client.MaxBotClient;
import ru.max.botframework.message.Buttons;
import ru.max.botframework.message.InlineKeyboard;
import ru.max.botframework.message.Keyboards;
import ru.max.botframework.message.MessageTarget;
import ru.max.botframework.message.Messages;
import ru.max.botframework.message.MessagingFacade;
import ru.max.botframework.model.ChatId;
import ru.max.botframework.model.UserId;

/**
 * Sprint 6 low-level high-level API usage example (outside dispatcher runtime context).
 */
public final class MessagingFacadeExample {

    public static void main(String[] args) {
        MaxBotClient botClient = createConfiguredBotClient();

        MessagingFacade messaging = new MessagingFacade(botClient, userId -> resolveUserChat(userId));
        CallbackFacade callbacks = new CallbackFacade(botClient);
        ChatActionsFacade actions = new ChatActionsFacade(botClient);

        MessageTarget chatTarget = MessageTarget.chat(new ChatId("chat-1"));
        MessageTarget userTarget = MessageTarget.user(new UserId("user-1"));

        InlineKeyboard keyboard = Keyboards.inline(k -> k
                .row(
                        Buttons.callback("Оплатить", "pay:1"),
                        Buttons.link("Сайт", "https://example.com")
                )
                .row(
                        Buttons.openApp("Мини-приложение", "app:orders"),
                        Buttons.message("Сообщение", "Привет из кнопки")
                )
        );

        // send (plain/markdown/html + keyboard)
        var sent = messaging.send(chatTarget, Messages.text("Привет").keyboard(keyboard));
        var markdownSent = messaging.send(userTarget, Messages.markdown("*Статус:* создан"));
        var htmlSent = messaging.send(chatTarget, Messages.html("<b>Заказ принят</b>"));

        // reply/edit/delete
        var reply = messaging.reply(sent, Messages.text("Готово"));
        boolean edited = messaging.edit(reply, Messages.text("Обновлено").markdown());
        boolean deleted = messaging.delete(htmlSent);

        // callback answer and update current callback message
        CallbackContext callbackContext = callbacks.context(loadCallbackEvent());
        callbackContext.answer(CallbackAnswers.text("Оплата принята").notify(true));
        callbackContext.updateCurrentMessage(Messages.text("Статус: оплачено"));

        // chat actions
        actions.typing(new ChatId("chat-1"));
        actions.sendingPhoto(new ChatId("chat-1"));

        System.out.println("sent=" + sent.messageId().value()
                + ", markdown=" + markdownSent.messageId().value()
                + ", edited=" + edited
                + ", deleted=" + deleted);
    }

    private static MaxBotClient createConfiguredBotClient() {
        throw new UnsupportedOperationException("Provide configured MaxBotClient instance");
    }

    private static ChatId resolveUserChat(UserId userId) {
        throw new UnsupportedOperationException("Resolve user to chat id: " + userId.value());
    }

    private static ru.max.botframework.model.Callback loadCallbackEvent() {
        throw new UnsupportedOperationException("Load callback event from update pipeline");
    }
}
