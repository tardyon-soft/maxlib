package ru.tardyon.botframework.examples.sprint9;

import java.util.concurrent.CompletableFuture;
import ru.tardyon.botframework.dispatcher.BuiltInFilters;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.fsm.InMemorySceneRegistry;
import ru.tardyon.botframework.fsm.MemorySceneStorage;
import ru.tardyon.botframework.fsm.MemoryStorage;
import ru.tardyon.botframework.fsm.Wizard;
import ru.tardyon.botframework.message.Messages;

/**
 * Minimal FSM + scenes runtime example.
 */
public final class FsmScenesExample {

    public static void main(String[] args) {
        Dispatcher dispatcher = new Dispatcher()
                .withFsmStorage(new MemoryStorage())
                .withSceneRegistry(new InMemorySceneRegistry()
                        .register(Wizard.named("checkout")
                                .step("email")
                                .step("confirm")
                                .build()))
                .withSceneStorage(new MemorySceneStorage())
                .withBotClient(createConfiguredBotClient());

        Router router = new Router("fsm-scenes");

        router.message(BuiltInFilters.command("start"), (message, ctx) ->
                ctx.fsm().setState("form.email").thenCompose(ignored ->
                        ctx.fsm().setData(java.util.Map.of("orderId", "42")).thenCompose(ignored2 -> {
                            ctx.reply(Messages.text("Состояние установлено: form.email"));
                            return CompletableFuture.completedFuture(null);
                        })
                )
        );

        router.message(BuiltInFilters.state("form.email"), (message, ctx) ->
                ctx.fsm().data().thenCompose(data -> {
                    String orderId = data.get("orderId", String.class).orElse("n/a");
                    ctx.reply(Messages.text("Введите email для заказа " + orderId));
                    return CompletableFuture.completedFuture(null);
                })
        );

        router.message(BuiltInFilters.textEquals("/scene-enter"), (message, ctx) ->
                ctx.scenes().enter("checkout")
                        .thenCompose(ignored -> ctx.wizard().next())
                        .thenCompose(ignored -> CompletableFuture.completedFuture(null))
        );

        router.message(BuiltInFilters.textEquals("/scene-exit"), (message, ctx) ->
                ctx.wizard().exit().thenCompose(ignored -> CompletableFuture.completedFuture(null))
        );

        dispatcher.includeRouter(router);
    }

    private static ru.tardyon.botframework.client.MaxBotClient createConfiguredBotClient() {
        throw new UnsupportedOperationException("Provide configured MaxBotClient instance");
    }
}
