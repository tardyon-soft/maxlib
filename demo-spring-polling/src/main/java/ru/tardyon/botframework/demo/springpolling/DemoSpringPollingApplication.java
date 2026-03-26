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
import ru.tardyon.botframework.screen.ScreenRegistry;
import ru.tardyon.botframework.screen.ScreenRouter;
import ru.tardyon.botframework.screen.Screens;

/**
 * Manual Spring Boot demo app for polling-mode runtime verification.
 */
@SpringBootApplication
public class DemoSpringPollingApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoSpringPollingApplication.class, args);
    }

    @Bean
    Router demoRouter(ScreenRegistry screenRegistry) {
        Router router = new Router("demo-main");
        ScreenDemoSupport.registerDefaults(screenRegistry);
        ScreenRouter.attach(router, screenRegistry);

        router.message(BuiltInFilters.command("start"), (message, ctx) -> {
            ctx.messaging().send(message.chat().id(), Messages.text(
                    "Привет! Классический API: /menu, /typing, /form. "
                            + "Аннотационный API: /astart, /amenu, /aform, /aecho <text>. "
                            + "App Mode: /app (UX без истории сообщений). /screen и /ascreen - demo screen stack. "
                            + "Smoke API: /qa, /qa_run_all, /qa_callback, /qa_set_video <token>"
            ));
            return CompletableFuture.completedFuture(null);
        });

        router.message(BuiltInFilters.command("screen"), (message, ctx) ->
                Screens.navigator(ctx, screenRegistry).start("home", Map.of())
        );

        router.message(BuiltInFilters.command("menu"), (message, ctx) -> {
            ctx.messaging().send(message.chat().id(),
                    Messages.text("Выберите действие")
                            .keyboard(Keyboards.inline(k -> k.row(
                                    Buttons.callback("Оплатить", "menu:pay"),
                                    Buttons.callback("Помощь", "menu:help")
                            )))
            );
            return CompletableFuture.completedFuture(null);
        });

        router.callback(BuiltInFilters.callbackDataStartsWith("menu:"), (callback, ctx) -> {
            String data = callback.data();
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
            ctx.messaging().send(message.chat().id(), Messages.text("Отправлен chat action: typing"));
            return CompletableFuture.completedFuture(null);
        });

        router.message(BuiltInFilters.command("form"), (message, ctx) ->
                ctx.fsm().setState("demo.form.name")
                        .thenAccept(ignored -> ctx.messaging().send(message.chat().id(), Messages.text("Введите ваше имя:")))
        );

        router.message(BuiltInFilters.state("demo.form.name"), (message, ctx) -> {
            String name = message.text() == null || message.text().isBlank()
                    ? "без имени"
                    : message.text().trim();

            return ctx.fsm().updateData(Map.of("name", name))
                    .thenCompose(updated -> ctx.fsm().setState("demo.form.done"))
                    .thenAccept(ignored -> ctx.messaging().send(
                            message.chat().id(),
                            Messages.text("Спасибо, " + name + "! Форма сохранена.")
                    ));
        });

        router.message(BuiltInFilters.state("demo.form.done"), (message, ctx) ->
                ctx.fsm().clear().thenAccept(ignored ->
                        ctx.messaging().send(
                                message.chat().id(),
                                Messages.text("Форма уже заполнена. Для нового ввода снова используйте /form")
                        ))
        );

        return router;
    }
}
