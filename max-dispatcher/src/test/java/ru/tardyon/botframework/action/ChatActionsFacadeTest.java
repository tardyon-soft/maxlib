package ru.tardyon.botframework.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.tardyon.botframework.client.MaxBotClient;
import ru.tardyon.botframework.dispatcher.RuntimeContext;
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

class ChatActionsFacadeTest {

    @Test
    void dispatchesExplicitActionToChat() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        when(client.sendChatAction(new ChatId("chat-1"), ChatAction.SENDING_FILE)).thenReturn(true);
        ChatActionsFacade facade = new ChatActionsFacade(client);

        boolean result = facade.send(new ChatId("chat-1"), ChatAction.SENDING_FILE);

        assertTrue(result);
        verify(client).sendChatAction(new ChatId("chat-1"), ChatAction.SENDING_FILE);
    }

    @Test
    void typingHelperMapsToTypingAction() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        when(client.sendChatAction(new ChatId("chat-1"), ChatAction.TYPING)).thenReturn(true);
        ChatActionsFacade facade = new ChatActionsFacade(client);

        boolean result = facade.typing(new ChatId("chat-1"));

        assertTrue(result);
        verify(client).sendChatAction(new ChatId("chat-1"), ChatAction.TYPING);
    }

    @Test
    void mediaHelpersMapToTypedActions() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        when(client.sendChatAction(new ChatId("chat-1"), ChatAction.SENDING_VIDEO)).thenReturn(true);
        when(client.sendChatAction(new ChatId("chat-1"), ChatAction.SENDING_AUDIO)).thenReturn(true);
        when(client.sendChatAction(new ChatId("chat-1"), ChatAction.SENDING_FILE)).thenReturn(true);
        ChatActionsFacade facade = new ChatActionsFacade(client);

        assertTrue(facade.sendingVideo(new ChatId("chat-1")));
        assertTrue(facade.sendingAudio(new ChatId("chat-1")));
        assertTrue(facade.sendingFile(new ChatId("chat-1")));

        verify(client).sendChatAction(new ChatId("chat-1"), ChatAction.SENDING_VIDEO);
        verify(client).sendChatAction(new ChatId("chat-1"), ChatAction.SENDING_AUDIO);
        verify(client).sendChatAction(new ChatId("chat-1"), ChatAction.SENDING_FILE);
    }

    @Test
    void resolvesTargetFromRuntimeContextMessageUpdate() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        when(client.sendChatAction(new ChatId("chat-ctx"), ChatAction.SENDING_PHOTO)).thenReturn(true);
        ChatActionsFacade facade = new ChatActionsFacade(client);
        RuntimeContext context = new RuntimeContext(new Update(
                new UpdateId("upd-1"),
                UpdateType.MESSAGE,
                sampleMessage("chat-ctx", "m-1"),
                null,
                null,
                Instant.parse("2026-03-12T00:00:00Z")
        ));

        boolean result = facade.sendingPhoto(context);

        assertTrue(result);
        verify(client).sendChatAction(new ChatId("chat-ctx"), ChatAction.SENDING_PHOTO);
    }

    @Test
    void resolvesTargetFromRuntimeContextCallbackUpdate() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        when(client.sendChatAction(new ChatId("chat-cb"), ChatAction.TYPING)).thenReturn(true);
        ChatActionsFacade facade = new ChatActionsFacade(client);
        RuntimeContext context = new RuntimeContext(new Update(
                new UpdateId("upd-2"),
                UpdateType.CALLBACK,
                null,
                new Callback(
                        new CallbackId("cb-1"),
                        "button:1",
                        sampleUser(),
                        sampleMessage("chat-cb", "m-cb"),
                        Instant.parse("2026-03-12T00:00:01Z")
                ),
                null,
                Instant.parse("2026-03-12T00:00:01Z")
        ));

        boolean result = facade.typing(context);

        assertTrue(result);
        verify(client).sendChatAction(new ChatId("chat-cb"), ChatAction.TYPING);
    }

    @Test
    void failsWhenRuntimeContextCannotResolveTargetChat() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        ChatActionsFacade facade = new ChatActionsFacade(client);
        RuntimeContext context = new RuntimeContext(new Update(
                new UpdateId("upd-3"),
                UpdateType.UNKNOWN,
                null,
                null,
                null,
                Instant.parse("2026-03-12T00:00:02Z")
        ));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> facade.typing(context)
        );

        assertEquals("chat action target cannot be resolved from update", exception.getMessage());
    }

    @Test
    void dispatchesActionFromCallbackPayload() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        when(client.sendChatAction(new ChatId("chat-cb"), ChatAction.TYPING)).thenReturn(true);
        ChatActionsFacade facade = new ChatActionsFacade(client);

        boolean result = facade.send(
                new Callback(
                        new CallbackId("cb-2"),
                        "button:1",
                        sampleUser(),
                        sampleMessage("chat-cb", "m-cb"),
                        Instant.parse("2026-03-12T00:00:01Z")
                ),
                ChatAction.TYPING
        );

        assertTrue(result);
        verify(client).sendChatAction(new ChatId("chat-cb"), ChatAction.TYPING);
    }

    @Test
    void failsWhenCallbackWithoutMessageUsedAsTarget() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        ChatActionsFacade facade = new ChatActionsFacade(client);
        Callback callback = new Callback(new CallbackId("cb-empty"), "button:1", sampleUser(), null, Instant.now());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> facade.send(callback, ChatAction.TYPING)
        );

        assertEquals("callback message is required to resolve chat action target", exception.getMessage());
    }

    private static Message sampleMessage(String chatId, String messageId) {
        return new Message(
                new MessageId(messageId),
                new Chat(new ChatId(chatId), ChatType.PRIVATE, "chat", null, null),
                sampleUser(),
                "text",
                Instant.parse("2026-03-12T00:00:00Z"),
                null,
                List.of(),
                List.of()
        );
    }

    private static User sampleUser() {
        return new User(new UserId("user-1"), "demo", "Demo", "User", "Demo User", false, "en");
    }
}
