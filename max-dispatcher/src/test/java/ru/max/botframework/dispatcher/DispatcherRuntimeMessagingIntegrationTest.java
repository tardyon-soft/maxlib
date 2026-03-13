package ru.max.botframework.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.max.botframework.client.MaxBotClient;
import ru.max.botframework.message.MessagingFacade;
import ru.max.botframework.message.Messages;
import ru.max.botframework.model.Callback;
import ru.max.botframework.model.CallbackId;
import ru.max.botframework.model.Chat;
import ru.max.botframework.model.ChatAction;
import ru.max.botframework.model.ChatId;
import ru.max.botframework.model.ChatType;
import ru.max.botframework.model.Message;
import ru.max.botframework.model.MessageId;
import ru.max.botframework.model.Update;
import ru.max.botframework.model.UpdateId;
import ru.max.botframework.model.UpdateType;
import ru.max.botframework.model.User;
import ru.max.botframework.model.UserId;
import ru.max.botframework.model.request.AnswerCallbackRequest;
import ru.max.botframework.model.request.SendMessageRequest;

class DispatcherRuntimeMessagingIntegrationTest {

    @Test
    void messageHandlerCanReplyViaRuntimeContext() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        when(client.sendMessage(any(SendMessageRequest.class))).thenReturn(sampleMessage("m-out", "reply"));

        Dispatcher dispatcher = new Dispatcher().withBotClient(client);
        Router router = new Router("main");
        router.message((message, context) -> {
            context.reply(Messages.text("pong"));
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate("m-in", "ping")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(client).sendMessage(requestCaptor.capture());
        SendMessageRequest request = requestCaptor.getValue();
        assertEquals("chat-1", request.chatId().value());
        assertEquals("m-in", request.replyToMessageId().value());
        assertEquals("pong", request.body().text());
    }

    @Test
    void callbackHandlerCanAnswerViaRuntimeContext() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        when(client.answerCallback(any(AnswerCallbackRequest.class))).thenReturn(true);

        Dispatcher dispatcher = new Dispatcher().withBotClient(client);
        Router router = new Router("callbacks");
        router.callback((callback, context) -> {
            context.answerCallback("OK");
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(callbackUpdate("cb-1", "m-cb")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        ArgumentCaptor<AnswerCallbackRequest> requestCaptor = ArgumentCaptor.forClass(AnswerCallbackRequest.class);
        verify(client).answerCallback(requestCaptor.capture());
        AnswerCallbackRequest request = requestCaptor.getValue();
        assertEquals("cb-1", request.callbackId().value());
        assertEquals("OK", request.text());
    }

    @Test
    void handlerCanSendChatActionViaRuntimeContext() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        when(client.sendChatAction(new ChatId("chat-1"), ChatAction.TYPING)).thenReturn(true);

        Dispatcher dispatcher = new Dispatcher().withBotClient(client);
        Router router = new Router("actions");
        router.message((message, context) -> {
            boolean sent = context.chatAction(ChatAction.TYPING);
            assertTrue(sent);
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate("m-act", "hello")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        verify(client).sendChatAction(new ChatId("chat-1"), ChatAction.TYPING);
    }

    @Test
    void reflectiveHandlerCanResolveMessagingFacadeParameter() throws Exception {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        when(client.sendMessage(any(SendMessageRequest.class))).thenReturn(sampleMessage("m-out", "done"));

        Dispatcher dispatcher = new Dispatcher().withBotClient(client);
        Router router = new Router("reflective");
        ReflectiveProbe probe = new ReflectiveProbe();
        Method method = ReflectiveProbe.class.getDeclaredMethod("onMessage", Message.class, MessagingFacade.class);
        router.message(probe, method);
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate("m-ref", "hello")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertTrue(probe.invoked);
        verify(client).sendMessage(any(SendMessageRequest.class));
    }

    private static Update messageUpdate(String messageId, String text) {
        return new Update(
                new UpdateId("upd-" + messageId),
                UpdateType.MESSAGE,
                sampleMessage(messageId, text),
                null,
                null,
                Instant.parse("2026-03-12T00:00:00Z")
        );
    }

    private static Update callbackUpdate(String callbackId, String messageId) {
        return new Update(
                new UpdateId("upd-" + callbackId),
                UpdateType.CALLBACK,
                null,
                new Callback(
                        new CallbackId(callbackId),
                        "button:data",
                        sampleUser(),
                        sampleMessage(messageId, "source"),
                        Instant.parse("2026-03-12T00:00:01Z")
                ),
                null,
                Instant.parse("2026-03-12T00:00:01Z")
        );
    }

    private static Message sampleMessage(String messageId, String text) {
        return new Message(
                new MessageId(messageId),
                new Chat(new ChatId("chat-1"), ChatType.PRIVATE, "chat", null, null),
                sampleUser(),
                text,
                Instant.parse("2026-03-12T00:00:00Z"),
                null,
                List.of(),
                List.of()
        );
    }

    private static User sampleUser() {
        return new User(new UserId("user-1"), "demo", "Demo", "User", "Demo User", false, "en");
    }

    private static final class ReflectiveProbe {
        private boolean invoked;

        @SuppressWarnings("unused")
        public CompletableFuture<Void> onMessage(Message message, MessagingFacade messaging) {
            invoked = true;
            messaging.reply(message, Messages.text("from-reflective"));
            return CompletableFuture.completedFuture(null);
        }
    }
}
