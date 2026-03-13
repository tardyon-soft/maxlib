package ru.max.botframework.testkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import ru.max.botframework.client.method.GetMeRequest;
import ru.max.botframework.client.method.SendMessageMethodRequest;
import ru.max.botframework.message.Messages;
import ru.max.botframework.model.ChatId;
import ru.max.botframework.model.Message;
import ru.max.botframework.model.UserId;
import ru.max.botframework.model.request.SendMessageRequest;
import ru.max.botframework.model.response.MessageResponse;

class RecordingMaxBotClientTest {

    @Test
    void recordsCallsAndReturnsDeterministicDefaults() {
        RecordingMaxBotClient client = new RecordingMaxBotClient();

        Message message = client.sendMessage(
                new SendMessageRequest(
                        new ChatId("chat-1"),
                        Messages.text("hello").toNewMessageBody(),
                        Boolean.TRUE,
                        null
                )
        );

        assertEquals("chat-1", message.chat().id().value());
        assertEquals(1, client.calls().size());
        assertEquals("/messages", client.calls().getFirst().path());
        assertFalse(client.calls().getFirst().body().isEmpty());
    }

    @Test
    void supportsExplicitResponseOverrides() {
        RecordingMaxBotClient client = new RecordingMaxBotClient();
        Message expected = TestUpdates.message("override").message();
        client.respondWith(MessageResponse.class, new MessageResponse(expected));

        Message actual = client.sendMessage(
                new SendMessageRequest(
                        new ChatId("chat-2"),
                        Messages.text("ignored").toNewMessageBody(),
                        Boolean.TRUE,
                        null
                )
        );

        assertEquals(expected.messageId(), actual.messageId());
        assertEquals(expected.chat().id(), actual.chat().id());
    }

    @Test
    void returnsDefaultGetMeResult() {
        RecordingMaxBotClient client = new RecordingMaxBotClient();

        var me = client.execute(GetMeRequest.INSTANCE);

        assertEquals(new UserId("bot-test"), me.id());
        assertTrue(client.calls().stream().anyMatch(call -> "/me".equals(call.path())));
    }

    @Test
    void filtersCallsByRequestType() {
        RecordingMaxBotClient client = new RecordingMaxBotClient();
        client.getMe();
        client.sendMessage(new SendMessageRequest(
                new ChatId("chat-3"),
                Messages.text("hi").toNewMessageBody(),
                Boolean.TRUE,
                null
        ));

        assertEquals(1, client.callsOfType(SendMessageMethodRequest.class).size());
    }
}
