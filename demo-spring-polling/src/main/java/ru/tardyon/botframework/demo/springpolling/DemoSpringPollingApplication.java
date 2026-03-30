package ru.tardyon.botframework.demo.springpolling;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import ru.tardyon.botframework.dispatcher.BuiltInFilters;
import ru.tardyon.botframework.dispatcher.Filter;
import ru.tardyon.botframework.dispatcher.FilterResult;
import ru.tardyon.botframework.dispatcher.RuntimeContext;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.message.Buttons;
import ru.tardyon.botframework.message.Keyboards;
import ru.tardyon.botframework.message.Messages;
import ru.tardyon.botframework.model.ChatAction;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.Message;
import ru.tardyon.botframework.screen.ScreenRegistry;
import ru.tardyon.botframework.screen.ScreenRouter;
import ru.tardyon.botframework.screen.Screens;
import ru.tardyon.botframework.screen.form.FormDefinition;
import ru.tardyon.botframework.screen.form.FormEngine;
import ru.tardyon.botframework.screen.form.FormState;
import ru.tardyon.botframework.screen.form.FormStep;
import ru.tardyon.botframework.screen.form.FormValidator;
import ru.tardyon.botframework.screen.form.FsmFormStateStorage;

/**
 * Manual Spring Boot demo app for polling-mode runtime verification.
 */
@SpringBootApplication
public class DemoSpringPollingApplication {
    private static final String SCREEN_FORM_NAMESPACE = "screen";

    public static void main(String[] args) {
        SpringApplication.run(DemoSpringPollingApplication.class, args);
    }

    @Bean
    Router demoRouter(ScreenRegistry screenRegistry) {
        FormEngine formEngine = new FormEngine(new FsmFormStateStorage());
        FormDefinition channelForm = FormDefinition.of(
                "demo.channel.form",
                FormStep.of(
                        "channelLink",
                        "Channel mini-form: отправьте ссылку на канал (например, https://max.ru/channel/demo)",
                        FormValidator.required("Ссылка на канал обязательна")
                ),
                FormStep.of(
                        "timezone",
                        "Укажите часовой пояс канала (например, +03:00). Для значения по умолчанию отправьте +03:00",
                        FormValidator.required("Часовой пояс обязателен")
                )
        );
        FormDefinition scheduleForm = FormDefinition.of(
                "demo.schedule.form",
                FormStep.of(
                        "day",
                        "Schedule picker mini-form: выберите день: СЕГОДНЯ, ЗАВТРА или ПОСЛЕЗАВТРА",
                        FormValidator.required("День обязателен")
                ),
                FormStep.of(
                        "time",
                        "Укажите время публикации в формате HH:mm (например, 09:30)",
                        FormValidator.required("Время обязательно")
                ),
                FormStep.of(
                        "minute",
                        "Укажите минуту с шагом 5 (0, 5, 10, ... 55)",
                        (input, state) -> {
                            if (input == null || input.isBlank()) {
                                return FormValidator.ValidationResult.error("Минута обязательна");
                            }
                            try {
                                int minute = Integer.parseInt(input.trim());
                                if (minute < 0 || minute > 55 || minute % 5 != 0) {
                                    return FormValidator.ValidationResult.error("Минута должна быть 0..55 с шагом 5");
                                }
                                return FormValidator.ValidationResult.ok();
                            } catch (NumberFormatException ex) {
                                return FormValidator.ValidationResult.error("Минута должна быть числом");
                            }
                        }
                )
        );
        Map<String, FormDefinition> formDefinitions = Map.of(
                channelForm.id(), channelForm,
                scheduleForm.id(), scheduleForm
        );

        Router router = new Router("demo-main");
        ScreenDemoSupport.registerDefaults(screenRegistry);
        ScreenRouter.attach(router, screenRegistry);

        router.message(BuiltInFilters.command("start"), (message, ctx) -> {
            ctx.messaging().send(message.chat().id(), Messages.text(
                    "Привет! Классический API: /menu, /typing, /form, /form_channel, /form_schedule. "
                            + "Аннотационный API: /astart, /amenu, /aform, /aecho <text>. "
                            + "App Mode: /app (UX без истории сообщений). "
                            + "/screen и /ascreen - demo screen stack. /cscreen - screen controller facade demo. "
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

        router.message(BuiltInFilters.command("form_channel"), (message, ctx) ->
                startScreenForm(ctx, message, formEngine, channelForm)
        );

        router.message(BuiltInFilters.command("form_schedule"), (message, ctx) ->
                startScreenForm(ctx, message, formEngine, scheduleForm)
        );

        router.message(activeScreenFormFilter(formEngine), (message, ctx) ->
                handleScreenFormInput(ctx, message, formEngine, formDefinitions)
        );

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

    private static Filter<Message> activeScreenFormFilter(FormEngine formEngine) {
        return new Filter<>() {
            @Override
            public CompletionStage<FilterResult> test(Message message) {
                return CompletableFuture.completedFuture(FilterResult.notMatched());
            }

            @Override
            public CompletionStage<FilterResult> test(Message message, RuntimeContext context) {
                if (message == null || message.text() == null || message.text().startsWith("/")) {
                    return CompletableFuture.completedFuture(FilterResult.notMatched());
                }
                return formEngine.current(context.fsm(SCREEN_FORM_NAMESPACE))
                        .thenApply(state -> state.filter(FormState::inProgress).isPresent()
                                ? FilterResult.matched()
                                : FilterResult.notMatched()
                        );
            }
        };
    }

    private static CompletionStage<Void> startScreenForm(
            RuntimeContext context,
            Message message,
            FormEngine formEngine,
            FormDefinition definition
    ) {
        return formEngine.start(context.fsm(SCREEN_FORM_NAMESPACE), definition).thenAccept(state ->
                context.messaging().send(
                        message.chat().id(),
                        Messages.text(renderPrompt(definition, state) + "\n\nКоманды: назад | отмена")
                )
        );
    }

    private static CompletionStage<Void> handleScreenFormInput(
            RuntimeContext context,
            Message message,
            FormEngine formEngine,
            Map<String, FormDefinition> definitions
    ) {
        String text = message.text() == null ? "" : message.text().trim();
        if (text.isEmpty()) {
            context.messaging().send(message.chat().id(), Messages.text("Пустое сообщение. Введите значение шага формы."));
            return CompletableFuture.completedFuture(null);
        }

        return formEngine.current(context.fsm(SCREEN_FORM_NAMESPACE)).thenCompose(stateOpt -> {
            if (stateOpt.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            FormState state = stateOpt.orElseThrow();
            FormDefinition definition = definitions.get(state.formId());
            if (definition == null) {
                return formEngine.cancel(context.fsm(SCREEN_FORM_NAMESPACE), FormDefinition.of(
                        state.formId(),
                        FormStep.of("cleanup", "cleanup")
                )).thenAccept(ignored ->
                        context.messaging().send(
                                message.chat().id(),
                                Messages.text("Найдена неизвестная форма. Текущее состояние очищено.")
                        )
                );
            }

            String lowered = text.toLowerCase();
            if ("отмена".equals(lowered) || "cancel".equals(lowered)) {
                return formEngine.cancel(context.fsm(SCREEN_FORM_NAMESPACE), definition).thenAccept(result ->
                        context.messaging().send(message.chat().id(), Messages.text("Форма отменена."))
                );
            }
            if ("назад".equals(lowered) || "back".equals(lowered)) {
                return formEngine.back(context.fsm(SCREEN_FORM_NAMESPACE), definition).thenAccept(result ->
                        sendTransitionFeedback(context, message.chat().id(), definition, result)
                );
            }

            if (definition.isLastStep(state.stepIndex())) {
                return formEngine.submit(context.fsm(SCREEN_FORM_NAMESPACE), definition, text).thenAccept(result ->
                        sendTransitionFeedback(context, message.chat().id(), definition, result)
                );
            }

            return formEngine.next(context.fsm(SCREEN_FORM_NAMESPACE), definition, text).thenAccept(result ->
                    sendTransitionFeedback(context, message.chat().id(), definition, result)
            );
        });
    }

    private static void sendTransitionFeedback(
            RuntimeContext context,
            ChatId chatId,
            FormDefinition definition,
            FormEngine.TransitionResult result
    ) {
        if (result.blocked()) {
            FormState state = result.state();
            String prompt = state == null ? "" : "\n\n" + renderPrompt(definition, state);
            context.messaging().send(chatId, Messages.text("Переход отклонен: " + result.error() + prompt));
            return;
        }
        if (result.finished()) {
            if (result.state() != null && result.state().status() == FormState.Status.SUBMITTED) {
                context.messaging().send(chatId, Messages.text(renderSummary(result.state())));
            } else {
                context.messaging().send(chatId, Messages.text("Форма завершена."));
            }
            return;
        }
        if (result.state() != null) {
            context.messaging().send(
                    chatId,
                    Messages.text(renderPrompt(definition, result.state()) + "\n\nКоманды: назад | отмена")
            );
        }
    }

    private static String renderPrompt(FormDefinition definition, FormState state) {
        FormStep step = definition.step(state.stepIndex());
        return "[" + (state.stepIndex() + 1) + "/" + definition.steps().size() + "] " + step.prompt();
    }

    private static String renderSummary(FormState state) {
        StringBuilder builder = new StringBuilder("Форма сохранена: ").append(state.formId());
        LinkedHashMap<String, String> ordered = new LinkedHashMap<>(state.values());
        for (Map.Entry<String, String> entry : ordered.entrySet()) {
            builder.append("\n- ").append(entry.getKey()).append(": ").append(entry.getValue());
        }
        return builder.toString();
    }
}
