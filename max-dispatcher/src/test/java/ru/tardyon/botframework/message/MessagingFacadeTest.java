package ru.tardyon.botframework.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.tardyon.botframework.client.MaxBotClient;
import ru.tardyon.botframework.model.Chat;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.ChatType;
import ru.tardyon.botframework.model.Message;
import ru.tardyon.botframework.model.MessageId;
import ru.tardyon.botframework.model.TextFormat;
import ru.tardyon.botframework.model.User;
import ru.tardyon.botframework.model.UserId;
import ru.tardyon.botframework.model.request.EditMessageRequest;
import ru.tardyon.botframework.model.request.SendMessageRequest;

class MessagingFacadeTest {

    @Test
    void sendToChatMapsBuilderToLowLevelSendRequest() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        Message sent = sampleMessage("m-out", "hello");
        when(client.sendMessage(any(SendMessageRequest.class))).thenReturn(sent);
        MessagingFacade facade = new MessagingFacade(client);

        Message result = facade.send(new ChatId("chat-1"), Messages.markdown("*hello*").canNotify(false));

        assertEquals(sent, result);
        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(client).sendMessage(captor.capture());
        SendMessageRequest request = captor.getValue();
        assertEquals("chat-1", request.chatId().value());
        assertEquals("*hello*", request.body().text());
        assertEquals(TextFormat.MARKDOWN, request.body().format());
        assertEquals(Boolean.FALSE, request.sendNotification());
    }

    @Test
    void sendToUserUsesProvidedUserToChatResolver() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        when(client.sendMessage(any(SendMessageRequest.class))).thenReturn(sampleMessage("m-out", "hello"));
        MessagingFacade facade = new MessagingFacade(client, userId -> new ChatId("chat-for-" + userId.value()));

        facade.send(new UserId("user-1"), Messages.text("hello"));

        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(client).sendMessage(captor.capture());
        assertEquals("chat-for-user-1", captor.getValue().chatId().value());
    }

    @Test
    void sendToUserWithoutResolverFailsFast() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        MessagingFacade facade = new MessagingFacade(client);

        assertThrows(
                IllegalStateException.class,
                () -> facade.send(new UserId("user-1"), Messages.text("hello"))
        );
        verify(client, never()).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void replyMapsReplyFlowToLowLevelSendRequest() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        when(client.sendMessage(any(SendMessageRequest.class))).thenReturn(sampleMessage("m-reply", "ok"));
        MessagingFacade facade = new MessagingFacade(client);
        Message source = sampleMessage("m-src", "incoming");

        facade.reply(source, Messages.html("<b>ok</b>").canNotify(false));

        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(client).sendMessage(captor.capture());
        SendMessageRequest request = captor.getValue();
        assertEquals(source.chat().id().value(), request.chatId().value());
        assertEquals(source.messageId().value(), request.replyToMessageId().value());
        assertEquals("<b>ok</b>", request.body().text());
        assertEquals(TextFormat.HTML, request.body().format());
        assertEquals(Boolean.FALSE, request.sendNotification());
    }

    @Test
    void replySkipsReplyToWhenSourceMessageIdIsSyntheticUnknown() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        when(client.sendMessage(any(SendMessageRequest.class))).thenReturn(sampleMessage("m-reply", "ok"));
        MessagingFacade facade = new MessagingFacade(client);
        Message source = sampleMessage("msg-unknown", "incoming");

        facade.reply(source, Messages.text("ok"));

        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(client).sendMessage(captor.capture());
        SendMessageRequest request = captor.getValue();
        assertEquals(source.chat().id().value(), request.chatId().value());
        assertEquals(null, request.replyToMessageId());
    }

    @Test
    void editMapsBuilderToLowLevelEditRequest() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        when(client.editMessage(any(EditMessageRequest.class))).thenReturn(true);
        MessagingFacade facade = new MessagingFacade(client);
        Message source = sampleMessage("m-edit", "old");

        boolean result = facade.edit(source, Messages.text("new").markdown().canNotify(false));

        assertTrue(result);
        ArgumentCaptor<EditMessageRequest> captor = ArgumentCaptor.forClass(EditMessageRequest.class);
        verify(client).editMessage(captor.capture());
        EditMessageRequest request = captor.getValue();
        assertEquals(source.chat().id().value(), request.chatId().value());
        assertEquals(source.messageId().value(), request.messageId().value());
        assertEquals("new", request.body().text());
        assertEquals(TextFormat.MARKDOWN, request.body().format());
        assertEquals(Boolean.FALSE, request.sendNotification());
    }

    @Test
    void deleteDelegatesToLowLevelClient() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        when(client.deleteMessage(any(MessageId.class))).thenReturn(true);
        MessagingFacade facade = new MessagingFacade(client);
        Message source = sampleMessage("m-del", "bye");

        boolean deletedFromMessage = facade.delete(source);
        boolean deletedById = facade.delete(new MessageId("m-other"));

        assertTrue(deletedFromMessage);
        assertTrue(deletedById);
        verify(client).deleteMessage(new MessageId("m-del"));
        verify(client).deleteMessage(new MessageId("m-other"));
    }

    @Test
    void deleteSkipsSyntheticUnknownMessageId() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        MessagingFacade facade = new MessagingFacade(client);

        boolean deleted = facade.delete(new MessageId("msg-unknown"));

        assertEquals(false, deleted);
        verify(client, never()).deleteMessage(any(MessageId.class));
    }

    private static Message sampleMessage(String messageId, String text) {
        return new Message(
                new MessageId(messageId),
                new Chat(new ChatId("chat-1"), ChatType.PRIVATE, "chat", null, null),
                new User(new UserId("user-1"), "demo", "Demo", "User", "Demo User", false, "en"),
                text,
                Instant.parse("2026-03-12T00:00:00Z"),
                null,
                List.of(),
                List.of()
        );
    }
}
