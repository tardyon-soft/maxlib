package ru.tardyon.botframework.callback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.tardyon.botframework.client.MaxBotClient;
import ru.tardyon.botframework.message.Messages;
import ru.tardyon.botframework.model.Callback;
import ru.tardyon.botframework.model.CallbackId;
import ru.tardyon.botframework.model.Chat;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.ChatType;
import ru.tardyon.botframework.model.Message;
import ru.tardyon.botframework.model.MessageId;
import ru.tardyon.botframework.model.TextFormat;
import ru.tardyon.botframework.model.User;
import ru.tardyon.botframework.model.UserId;
import ru.tardyon.botframework.model.request.AnswerCallbackRequest;
import ru.tardyon.botframework.model.request.EditMessageRequest;

class CallbackFacadeTest {

    @Test
    void notificationOnlyAnswerMapsToLowLevelAnswerCallbackRequest() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        when(client.answerCallback(any(AnswerCallbackRequest.class))).thenReturn(true);
        CallbackFacade facade = new CallbackFacade(client);
        Callback callback = sampleCallbackWithMessage("cb-1", "src-1");

        boolean result = facade.notify(callback, "Done");

        assertTrue(result);
        ArgumentCaptor<AnswerCallbackRequest> captor = ArgumentCaptor.forClass(AnswerCallbackRequest.class);
        verify(client).answerCallback(captor.capture());
        AnswerCallbackRequest request = captor.getValue();
        assertEquals("cb-1", request.callbackId().value());
        assertEquals("Done", request.text());
        assertEquals(Boolean.TRUE, request.sendNotification());
        assertEquals(0, request.cacheSeconds());
    }

    @Test
    void customAnswerBuilderMapsNotifyAndCacheSeconds() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        when(client.answerCallback(any(AnswerCallbackRequest.class))).thenReturn(true);
        CallbackFacade facade = new CallbackFacade(client);
        Callback callback = sampleCallbackWithMessage("cb-3", "src-3");

        boolean result = facade.answer(callback, CallbackAnswers.text("Queued").notify(false).cacheSeconds(30));

        assertTrue(result);
        ArgumentCaptor<AnswerCallbackRequest> captor = ArgumentCaptor.forClass(AnswerCallbackRequest.class);
        verify(client).answerCallback(captor.capture());
        AnswerCallbackRequest request = captor.getValue();
        assertEquals("Queued", request.text());
        assertEquals(Boolean.FALSE, request.sendNotification());
        assertEquals(30, request.cacheSeconds());
    }

    @Test
    void updateCurrentMessageMapsToLowLevelEditRequest() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        when(client.editMessage(any(EditMessageRequest.class))).thenReturn(true);
        CallbackFacade facade = new CallbackFacade(client);
        Callback callback = sampleCallbackWithMessage("cb-1", "src-1");

        boolean result = facade.updateCurrentMessage(callback, Messages.markdown("*updated*").notify(false));

        assertTrue(result);
        ArgumentCaptor<EditMessageRequest> captor = ArgumentCaptor.forClass(EditMessageRequest.class);
        verify(client).editMessage(captor.capture());
        EditMessageRequest request = captor.getValue();
        assertEquals("chat-1", request.chatId().value());
        assertEquals("src-1", request.messageId().value());
        assertEquals("*updated*", request.body().text());
        assertEquals(TextFormat.MARKDOWN, request.body().format());
        assertEquals(Boolean.FALSE, request.sendNotification());
    }

    @Test
    void callbackContextDelegatesToFacadeOperations() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        when(client.answerCallback(any(AnswerCallbackRequest.class))).thenReturn(true);
        when(client.editMessage(any(EditMessageRequest.class))).thenReturn(true);
        CallbackFacade facade = new CallbackFacade(client);
        Callback callback = sampleCallbackWithMessage("cb-2", "src-2");
        CallbackContext context = facade.context(callback);

        assertTrue(context.answer("ok"));
        assertTrue(context.updateCurrentMessage(Messages.text("changed")));

        verify(client).answerCallback(any(AnswerCallbackRequest.class));
        verify(client).editMessage(any(EditMessageRequest.class));
    }

    @Test
    void updateCurrentMessageFailsWhenCallbackHasNoMessage() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        CallbackFacade facade = new CallbackFacade(client);
        Callback callback = new Callback(new CallbackId("cb-1"), "data", null, null, Instant.now());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> facade.updateCurrentMessage(callback, Messages.text("updated"))
        );

        assertEquals("callback message is required to update current message", exception.getMessage());
    }

    private static Callback sampleCallbackWithMessage(String callbackId, String messageId) {
        Message sourceMessage = new Message(
                new MessageId(messageId),
                new Chat(new ChatId("chat-1"), ChatType.PRIVATE, "chat", null, null),
                new User(new UserId("user-1"), "demo", "Demo", "User", "Demo User", false, "en"),
                "original",
                Instant.parse("2026-03-12T00:00:00Z"),
                null,
                List.of(),
                List.of()
        );
        return new Callback(
                new CallbackId(callbackId),
                "button:data",
                new User(new UserId("user-1"), "demo", "Demo", "User", "Demo User", false, "en"),
                sourceMessage,
                Instant.parse("2026-03-12T00:00:01Z")
        );
    }
}
