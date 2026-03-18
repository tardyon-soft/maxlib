package ru.tardyon.botframework.demo.springpolling;

import ru.tardyon.botframework.dispatcher.RuntimeContext;
import ru.tardyon.botframework.dispatcher.annotation.Callback;
import ru.tardyon.botframework.dispatcher.annotation.Command;
import ru.tardyon.botframework.dispatcher.annotation.Message;
import ru.tardyon.botframework.dispatcher.annotation.Route;
import ru.tardyon.botframework.message.Buttons;
import ru.tardyon.botframework.message.Keyboards;
import ru.tardyon.botframework.message.Messages;

/**
 * Interactive smoke route for manual MAX API verification from chat commands.
 */
@Route(value = "api-smoke", autoRegister = true)
public final class ApiSmokeRoute {
    private final ApiSmokeService smokeService;

    public ApiSmokeRoute(ApiSmokeService smokeService) {
        this.smokeService = smokeService;
    }

    @Command("qa")
    public void qaHelp(RuntimeContext ctx) {
        ctx.reply(Messages.text("""
                QA commands:
                /qa_run_all - run API smoke checks and print report
                /qa_callback - send callback button for POST /answers check
                /qa_set_video <token> - save video token for GET /videos/{token}
                """));
    }

    @Command("qa_run_all")
    public void runAll(ru.tardyon.botframework.model.Message message, RuntimeContext ctx) {
        String report = smokeService.runAll(message);
        ctx.reply(Messages.text(report));
    }

    @Command("qa_callback")
    public void callbackProbe(RuntimeContext ctx) {
        ctx.reply(Messages.text("Click button to test POST /answers")
                .keyboard(Keyboards.inline(keyboard -> keyboard.row(
                        Buttons.callback("QA callback ack", "qa:ack")
                ))));
    }

    @Callback("qa:ack")
    public void callbackAck(ru.tardyon.botframework.model.Callback callback, RuntimeContext ctx) {
        ctx.answerCallback("QA callback accepted");
        boolean updated = ctx.callbacks().updateCurrentMessage(callback, Messages.text("QA callback: processed"));
        if (!updated) {
            ctx.reply(Messages.text("QA callback processed (fallback send mode)"));
        }
    }

    @Message(text = "/qa_set_video ", startsWith = true)
    public void setVideoToken(ru.tardyon.botframework.model.Message message, RuntimeContext ctx) {
        String text = message.text() == null ? "" : message.text();
        String token = text.substring("/qa_set_video ".length()).trim();
        if (token.isEmpty()) {
            ctx.reply(Messages.text("Usage: /qa_set_video <video-token>"));
            return;
        }
        smokeService.setVideoToken(message, token);
        ctx.reply(Messages.text("Saved video token for smoke checks"));
    }
}
