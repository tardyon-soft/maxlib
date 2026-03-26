package ru.tardyon.botframework.demo.springpolling;

import static ru.tardyon.botframework.demo.springpolling.AppModeMiddleware.LAST_BOT_MSG_KEY;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.dispatcher.RuntimeContext;
import ru.tardyon.botframework.dispatcher.annotation.Callback;
import ru.tardyon.botframework.dispatcher.annotation.Command;
import ru.tardyon.botframework.dispatcher.annotation.Route;
import ru.tardyon.botframework.dispatcher.annotation.State;
import ru.tardyon.botframework.dispatcher.annotation.UseMiddleware;
import ru.tardyon.botframework.message.Buttons;
import ru.tardyon.botframework.message.KeyboardMarkup;
import ru.tardyon.botframework.message.Keyboards;
import ru.tardyon.botframework.message.Messages;
import ru.tardyon.botframework.model.Message;

/**
 * Demo route with "app-like" UX: user messages and old bot messages are auto-deleted,
 * so the chat feels like navigating an application, not a conversation.
 *
 * <p>Text messages go through {@link AppModeMiddleware} which deletes them.
 * Callback buttons edit the current message in place via {@code updateCurrentMessage}.
 */
@Route(value = "app-mode", autoRegister = true)
@UseMiddleware(AppModeMiddleware.class)
public final class AppModeRoute {

    // ── Commands (text messages — middleware deletes user msg + old bot msg) ──

    @Command("app")
    public CompletionStage<Void> start(RuntimeContext ctx) {
        Message sent = ctx.messaging().send(
                ctx.update().message().chat().id(),
                Messages.markdown("*Добро пожаловать в App Mode!*\n\nВыберите раздел:")
                        .keyboard(mainMenuKeyboard())
        );
        return saveLastBotMessage(ctx, sent);
    }

    // ── Callbacks (edit message in place — no deletions needed) ──

    @Callback("app:menu")
    public void backToMenu(ru.tardyon.botframework.model.Callback callback, RuntimeContext ctx) {
        ctx.answerCallback("Главное меню");
        ctx.callbacks().updateCurrentMessage(callback,
                Messages.markdown("*Главное меню*\n\nВыберите раздел:")
                        .keyboard(mainMenuKeyboard())
        );
    }

    @Callback("app:profile")
    public void profile(ru.tardyon.botframework.model.Callback callback, RuntimeContext ctx) {
        ctx.answerCallback("Профиль");
        String userName = callback.from() != null && callback.from().id() != null
                ? callback.from().id().value()
                : "Пользователь";
        ctx.callbacks().updateCurrentMessage(callback,
                Messages.markdown("*Профиль*\n\nИмя: " + userName)
                        .keyboard(Keyboards.inline(k -> k
                                .row(Buttons.callback("Редактировать имя", "app:edit_name"))
                                .row(Buttons.callback("Назад", "app:menu"))
                        ))
        );
    }

    @Callback("app:settings")
    public void settings(ru.tardyon.botframework.model.Callback callback, RuntimeContext ctx) {
        ctx.answerCallback("Настройки");
        ctx.callbacks().updateCurrentMessage(callback,
                Messages.markdown("*Настройки*\n\nВыберите параметр:")
                        .keyboard(Keyboards.inline(k -> k
                                .row(Buttons.callback("Язык: Русский", "app:lang"))
                                .row(Buttons.callback("Уведомления: Вкл", "app:notif"))
                                .row(Buttons.callback("Назад", "app:menu"))
                        ))
        );
    }

    @Callback("app:lang")
    public void language(ru.tardyon.botframework.model.Callback callback, RuntimeContext ctx) {
        ctx.answerCallback("Язык переключён");
        ctx.callbacks().updateCurrentMessage(callback,
                Messages.markdown("*Настройки*\n\nВыберите параметр:")
                        .keyboard(Keyboards.inline(k -> k
                                .row(Buttons.callback("Язык: English", "app:lang_back"))
                                .row(Buttons.callback("Уведомления: Вкл", "app:notif"))
                                .row(Buttons.callback("Назад", "app:menu"))
                        ))
        );
    }

    @Callback("app:lang_back")
    public void languageBack(ru.tardyon.botframework.model.Callback callback, RuntimeContext ctx) {
        ctx.answerCallback("Language switched");
        ctx.callbacks().updateCurrentMessage(callback,
                Messages.markdown("*Настройки*\n\nВыберите параметр:")
                        .keyboard(Keyboards.inline(k -> k
                                .row(Buttons.callback("Язык: Русский", "app:lang"))
                                .row(Buttons.callback("Уведомления: Вкл", "app:notif"))
                                .row(Buttons.callback("Назад", "app:menu"))
                        ))
        );
    }

    @Callback("app:notif")
    public void toggleNotifications(ru.tardyon.botframework.model.Callback callback, RuntimeContext ctx) {
        ctx.answerCallback("Уведомления переключены");
        ctx.callbacks().updateCurrentMessage(callback,
                Messages.markdown("*Настройки*\n\nВыберите параметр:")
                        .keyboard(Keyboards.inline(k -> k
                                .row(Buttons.callback("Язык: Русский", "app:lang"))
                                .row(Buttons.callback("Уведомления: Выкл", "app:notif_on"))
                                .row(Buttons.callback("Назад", "app:menu"))
                        ))
        );
    }

    @Callback("app:notif_on")
    public void notificationsOn(ru.tardyon.botframework.model.Callback callback, RuntimeContext ctx) {
        ctx.answerCallback("Уведомления включены");
        ctx.callbacks().updateCurrentMessage(callback,
                Messages.markdown("*Настройки*\n\nВыберите параметр:")
                        .keyboard(Keyboards.inline(k -> k
                                .row(Buttons.callback("Язык: Русский", "app:lang"))
                                .row(Buttons.callback("Уведомления: Вкл", "app:notif"))
                                .row(Buttons.callback("Назад", "app:menu"))
                        ))
        );
    }

    @Callback("app:help")
    public void help(ru.tardyon.botframework.model.Callback callback, RuntimeContext ctx) {
        ctx.answerCallback("Помощь");
        ctx.callbacks().updateCurrentMessage(callback,
                Messages.markdown(
                        "*Помощь*\n\n"
                                + "Этот бот работает как приложение:\n"
                                + "- Ваши сообщения автоматически удаляются\n"
                                + "- Интерфейс обновляется на месте\n"
                                + "- Навигация через кнопки\n\n"
                                + "Команда /app — перезапуск"
                ).keyboard(Keyboards.inline(k -> k
                        .row(Buttons.callback("Назад", "app:menu"))
                ))
        );
    }

    // ── FSM form: edit name (text input with app-mode UX) ──

    @Callback("app:edit_name")
    public CompletionStage<Void> editNameStart(ru.tardyon.botframework.model.Callback callback, RuntimeContext ctx) {
        ctx.answerCallback("Введите имя");
        ctx.callbacks().updateCurrentMessage(callback,
                Messages.markdown("*Редактирование профиля*\n\nВведите новое имя:")
        );
        // Save the current message ID so middleware can delete it when user replies
        String msgId = callback.message() != null && callback.message().messageId() != null
                ? callback.message().messageId().value()
                : null;
        return ctx.fsm().setState("app.edit_name")
                .thenCompose(ignored -> msgId != null
                        ? ctx.fsm().updateData(Map.of(LAST_BOT_MSG_KEY, msgId)).thenAccept(d -> {})
                        : CompletableFuture.completedFuture(null));
    }

    @State("app.edit_name")
    public CompletionStage<Void> editNameCollect(Message message, RuntimeContext ctx) {
        String name = message.text() == null || message.text().isBlank()
                ? "Без имени"
                : message.text().trim();

        Message sent = ctx.messaging().send(
                message.chat().id(),
                Messages.markdown("*Профиль обновлён!*\n\nНовое имя: " + name)
                        .keyboard(Keyboards.inline(k -> k
                                .row(Buttons.callback("Профиль", "app:profile"))
                                .row(Buttons.callback("Главное меню", "app:menu"))
                        ))
        );
        return ctx.fsm().clear()
                .thenCompose(ignored -> ctx.fsm().updateData(Map.of(LAST_BOT_MSG_KEY, sent.messageId().value())))
                .thenAccept(d -> {});
    }

    // ── Helpers ──

    private static KeyboardMarkup mainMenuKeyboard() {
        return Keyboards.inline(k -> k
                .row(Buttons.callback("Профиль", "app:profile"),
                        Buttons.callback("Настройки", "app:settings"))
                .row(Buttons.callback("Помощь", "app:help"))
        );
    }

    private static CompletionStage<Void> saveLastBotMessage(RuntimeContext ctx, Message sent) {
        return ctx.fsm().updateData(Map.of(LAST_BOT_MSG_KEY, sent.messageId().value()))
                .thenAccept(d -> {});
    }
}
