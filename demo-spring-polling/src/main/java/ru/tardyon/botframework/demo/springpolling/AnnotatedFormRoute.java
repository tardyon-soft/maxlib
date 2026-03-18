package ru.tardyon.botframework.demo.springpolling;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import org.springframework.stereotype.Component;
import ru.tardyon.botframework.dispatcher.RuntimeContext;
import ru.tardyon.botframework.dispatcher.annotation.Command;
import ru.tardyon.botframework.dispatcher.annotation.Route;
import ru.tardyon.botframework.dispatcher.annotation.State;
import ru.tardyon.botframework.message.Messages;
import ru.tardyon.botframework.model.Message;

/**
 * Annotation-based route demo for FSM flow.
 */
@Component
@Route(value = "annotated-form", autoRegister = true)
public final class AnnotatedFormRoute {
    private static final String FORM_NAME_STATE = "demo.annotated.form.name";
    private static final String FORM_DONE_STATE = "demo.annotated.form.done";

    @Command("a-form")
    public CompletionStage<Void> start(RuntimeContext ctx) {
        return ctx.fsm().setState(FORM_NAME_STATE)
                .thenAccept(ignored -> ctx.reply(Messages.text("Annotated form: введите имя")));
    }

    @State(FORM_NAME_STATE)
    public CompletionStage<Void> collectName(Message message, RuntimeContext ctx) {
        String name = message.text() == null || message.text().isBlank() ? "без имени" : message.text().trim();
        return ctx.fsm().updateData(Map.of("name", name))
                .thenCompose(updated -> ctx.fsm().setState(FORM_DONE_STATE))
                .thenAccept(ignored -> ctx.reply(Messages.text("Annotated form сохранен для " + name)));
    }

    @State(FORM_DONE_STATE)
    public CompletionStage<Void> done(RuntimeContext ctx) {
        return ctx.fsm().clear().thenAccept(ignored ->
                ctx.reply(Messages.text("Annotated form очищен. Повторите /a-form")));
    }
}
