package ru.tardyon.botframework.demo.springpolling;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import ru.tardyon.botframework.dispatcher.BuiltInFilters;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.ingestion.LongPollingRunnerConfig;
import ru.tardyon.botframework.ingestion.PollingFetchRequest;
import ru.tardyon.botframework.message.Buttons;
import ru.tardyon.botframework.message.Keyboards;
import ru.tardyon.botframework.message.Messages;
import ru.tardyon.botframework.model.ChatAction;
import ru.tardyon.botframework.spring.properties.MaxBotProperties;

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
                            + "Аннотационный API: /astart, /amenu, /aform, /aecho <text>. "
                            + "Smoke API: /qa, /qa_run_all, /qa_callback, /qa_set_video <token>"
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

        return router;
    }

    @Bean
    LongPollingRunnerConfig longPollingRunnerConfig(MaxBotProperties properties) {
        Integer timeoutSeconds = null;
        if (properties.getPolling().getTimeout() != null) {
            timeoutSeconds = Math.toIntExact(properties.getPolling().getTimeout().toSeconds());
        }
        PollingFetchRequest request = new PollingFetchRequest(
                null,
                timeoutSeconds,
                properties.getPolling().getLimit(),
                properties.getPolling().getTypes()
        );

        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "demo-long-polling-runner");
            thread.setDaemon(false);
            return thread;
        };
        ExecutorService executor = Executors.newSingleThreadExecutor(threadFactory);

        return LongPollingRunnerConfig.builder()
                .request(request)
                .executor(executor, true)
                .build();
    }
}
