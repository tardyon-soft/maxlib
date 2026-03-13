package ru.tardyon.botframework.examples.sprint6;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import ru.tardyon.botframework.action.ChatActionsFacade;
import ru.tardyon.botframework.callback.CallbackFacade;
import ru.tardyon.botframework.client.MaxBotClient;
import ru.tardyon.botframework.dispatcher.DispatchResult;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.message.MessagingFacade;
import ru.tardyon.botframework.message.Messages;
import ru.tardyon.botframework.model.Callback;
import ru.tardyon.botframework.model.CallbackId;
import ru.tardyon.botframework.model.Chat;
import ru.tardyon.botframework.model.ChatAction;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.ChatType;
import ru.tardyon.botframework.model.Message;
import ru.tardyon.botframework.model.MessageId;
import ru.tardyon.botframework.model.Update;
import ru.tardyon.botframework.model.UpdateId;
import ru.tardyon.botframework.model.UpdateType;
import ru.tardyon.botframework.model.User;
import ru.tardyon.botframework.model.UserId;

/**
 * Sprint 6 runtime handler ergonomics example:
 * RuntimeContext shortcuts + facade parameter resolution.
 */
public final class RuntimeMessagingHandlersExample {

    public static void main(String[] args) throws Exception {
        Dispatcher dispatcher = new Dispatcher()
                .withBotClient(createConfiguredBotClient());
        Router router = new Router("messaging-runtime");
        ExampleHandlers handlers = new ExampleHandlers();

        // RuntimeContext convenience API in handlers.
        router.message((message, ctx) -> {
            ctx.reply(Messages.text("Pong"));
            ctx.chatAction(ChatAction.TYPING);
            return CompletableFuture.completedFuture(null);
        });
        router.callback((callback, ctx) -> {
            ctx.answerCallback("OK");
            return CompletableFuture.completedFuture(null);
        });

        // Reflective handler parameter resolution of high-level facades.
        Method messageMethod = ExampleHandlers.class.getDeclaredMethod(
                "onMessage",
                Message.class,
                MessagingFacade.class,
                ChatActionsFacade.class
        );
        Method callbackMethod = ExampleHandlers.class.getDeclaredMethod(
                "onCallback",
                Callback.class,
                CallbackFacade.class
        );
        router.message(handlers, messageMethod);
        router.callback(handlers, callbackMethod);

        dispatcher.includeRouter(router);

        DispatchResult messageResult = dispatcher.feedUpdate(sampleMessageUpdate()).toCompletableFuture().join();
        DispatchResult callbackResult = dispatcher.feedUpdate(sampleCallbackUpdate()).toCompletableFuture().join();

        System.out.println("Message dispatch: " + messageResult.status());
        System.out.println("Callback dispatch: " + callbackResult.status());
    }

    public static final class ExampleHandlers {
        @SuppressWarnings("unused")
        public CompletableFuture<Void> onMessage(
                Message message,
                MessagingFacade messaging,
                ChatActionsFacade actions
        ) {
            messaging.reply(message, Messages.markdown("*Обработано*"));
            actions.sendingPhoto(message.chat().id());
            return CompletableFuture.completedFuture(null);
        }

        @SuppressWarnings("unused")
        public CompletableFuture<Void> onCallback(
                Callback callback,
                CallbackFacade callbacks
        ) {
            callbacks.notify(callback, "Принято");
            callbacks.updateCurrentMessage(callback, Messages.text("Статус обновлён"));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static MaxBotClient createConfiguredBotClient() {
        throw new UnsupportedOperationException("Provide configured MaxBotClient instance");
    }

    private static Update sampleMessageUpdate() {
        return new Update(
                new UpdateId("u-s6-msg"),
                UpdateType.MESSAGE,
                new Message(
                        new MessageId("m-s6-msg"),
                        new Chat(new ChatId("c-s6"), ChatType.PRIVATE, "chat", null, null),
                        new User(new UserId("u-s6"), "demo", "Demo", "User", "Demo User", false, "en"),
                        "ping",
                        Instant.parse("2026-03-12T00:00:00Z"),
                        null,
                        List.of(),
                        List.of()
                ),
                null,
                null,
                Instant.parse("2026-03-12T00:00:00Z")
        );
    }

    private static Update sampleCallbackUpdate() {
        Message source = new Message(
                new MessageId("m-s6-cb-source"),
                new Chat(new ChatId("c-s6"), ChatType.PRIVATE, "chat", null, null),
                new User(new UserId("u-s6"), "demo", "Demo", "User", "Demo User", false, "en"),
                "source",
                Instant.parse("2026-03-12T00:00:00Z"),
                null,
                List.of(),
                List.of()
        );
        return new Update(
                new UpdateId("u-s6-cb"),
                UpdateType.CALLBACK,
                null,
                new Callback(
                        new CallbackId("cb-s6"),
                        "order:1",
                        new User(new UserId("u-s6"), "demo", "Demo", "User", "Demo User", false, "en"),
                        source,
                        Instant.parse("2026-03-12T00:00:01Z")
                ),
                null,
                Instant.parse("2026-03-12T00:00:01Z")
        );
    }
}
