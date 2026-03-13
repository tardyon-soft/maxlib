package ru.tardyon.botframework.examples.sprint9;

import java.util.concurrent.CompletableFuture;
import ru.tardyon.botframework.dispatcher.BuiltInFilters;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.message.Buttons;
import ru.tardyon.botframework.message.Keyboards;
import ru.tardyon.botframework.message.Messages;

/**
 * Messaging + keyboard + callback answer ergonomics.
 */
public final class MessagingCallbacksExample {

    public static void main(String[] args) {
        Dispatcher dispatcher = new Dispatcher()
                .withBotClient(createConfiguredBotClient());

        Router router = new Router("messaging-callbacks");

        router.message(BuiltInFilters.command("start"), (message, ctx) -> {
            ctx.reply(
                    Messages.text("Выберите действие")
                            .keyboard(Keyboards.inline(k -> k.row(
                                    Buttons.callback("Оплатить", "pay:1"),
                                    Buttons.link("Сайт", "https://example.com")
                            )))
            );
            return CompletableFuture.completedFuture(null);
        });

        router.callback((callback, ctx) -> {
            ctx.answerCallback("OK");
            ctx.callbacks().updateCurrentMessage(callback, Messages.markdown("*Оплачено*"));
            return CompletableFuture.completedFuture(null);
        });

        dispatcher.includeRouter(router);
    }

    private static ru.tardyon.botframework.client.MaxBotClient createConfiguredBotClient() {
        throw new UnsupportedOperationException("Provide configured MaxBotClient instance");
    }
}
