package ru.tardyon.botframework.demo.springpolling;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import ru.tardyon.botframework.dispatcher.BuiltInFilters;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.message.Buttons;
import ru.tardyon.botframework.message.Keyboards;
import ru.tardyon.botframework.message.Messages;
import ru.tardyon.botframework.model.ChatAction;

/**
 * Manual Spring Boot demo app for polling-mode runtime verification.
 */
@SpringBootApplication
public class DemoSpringPollingApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoSpringPollingApplication.class, args);
    }

    @Bean
    Router demoRouter() {
        Router router = new Router("demo-main");

        router.message(BuiltInFilters.command("start"), (message, ctx) -> {
            ctx.reply(Messages.text(
                    "Привет! Классический API: /menu, /typing, /form. "
                            + "Аннотационный API: /a-start, /a-menu, /a-form, /a-echo <text>"
            ));
            return CompletableFuture.completedFuture(null);
        });

        router.message(BuiltInFilters.command("menu"), (message, ctx) -> {
            ctx.reply(
                    Messages.text("Выберите действие")
                            .keyboard(Keyboards.inline(k -> k.row(
                                    Buttons.callback("Оплатить", "menu:pay"),
                                    Buttons.callback("Помощь", "menu:help")
                            )))
            );
            return CompletableFuture.completedFuture(null);
        });

        router.callback((callback, ctx) -> {
            String data = callback.data();
            if (data == null || !data.startsWith("menu:")) {
                return CompletableFuture.completedFuture(null);
            }

            switch (data) {
                case "menu:pay" -> {
                    ctx.answerCallback("Платёж подтверждён");
                    ctx.callbacks().updateCurrentMessage(callback, Messages.markdown("*Платёж:* подтверждён"));
                }
                case "menu:help" -> {
                    ctx.answerCallback("Подсказка отправлена");
                    ctx.callbacks().updateCurrentMessage(callback, Messages.text("Используйте /start для навигации"));
                }
                default -> ctx.answerCallback("Неизвестное действие");
            }

            return CompletableFuture.completedFuture(null);
        });

        router.message(BuiltInFilters.command("typing"), (message, ctx) -> {
            ctx.chatAction(ChatAction.TYPING);
            ctx.reply(Messages.text("Отправлен chat action: typing"));
            return CompletableFuture.completedFuture(null);
        });

        router.message(BuiltInFilters.command("form"), (message, ctx) ->
                ctx.fsm().setState("demo.form.name")
                        .thenAccept(ignored -> ctx.reply(Messages.text("Введите ваше имя:")))
        );

        router.message(BuiltInFilters.state("demo.form.name"), (message, ctx) -> {
            String name = message.text() == null || message.text().isBlank()
                    ? "без имени"
                    : message.text().trim();

            return ctx.fsm().updateData(Map.of("name", name))
                    .thenCompose(updated -> ctx.fsm().setState("demo.form.done"))
                    .thenAccept(ignored -> ctx.reply(Messages.text("Спасибо, " + name + "! Форма сохранена.")));
        });

        router.message(BuiltInFilters.state("demo.form.done"), (message, ctx) ->
                ctx.fsm().clear().thenAccept(ignored ->
                        ctx.reply(Messages.text("Форма уже заполнена. Для нового ввода снова используйте /form")))
        );

        router.message((message, ctx) -> {
            String text = message.text() == null ? "" : message.text();
            ctx.reply(Messages.text("Echo: " + text));
            return CompletableFuture.completedFuture(null);
        });

        return router;
    }
}
