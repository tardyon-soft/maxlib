package ru.tardyon.botframework.fsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.model.Callback;
import ru.tardyon.botframework.model.CallbackId;
import ru.tardyon.botframework.model.Chat;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.ChatType;
import ru.tardyon.botframework.model.Message;
import ru.tardyon.botframework.model.MessageId;
import ru.tardyon.botframework.model.Update;
import ru.tardyon.botframework.model.UpdateId;
import ru.tardyon.botframework.model.UpdateType;
import ru.tardyon.botframework.model.User;
import ru.tardyon.botframework.model.UserId;

class StateKeyStrategiesTest {

    @Test
    void resolvesKeysForUserChatAndUserInChatScopesFromMessageUpdate() {
        Update update = messageUpdate("u-1", "c-1");

        StateKey userKey = StateKeyStrategies.user().resolve(update);
        StateKey chatKey = StateKeyStrategies.chat().resolve(update);
        StateKey userInChatKey = StateKeyStrategies.userInChat().resolve(update);

        assertEquals(new StateKey(StateScope.USER, new UserId("u-1"), null), userKey);
        assertEquals(new StateKey(StateScope.CHAT, null, new ChatId("c-1")), chatKey);
        assertEquals(
                new StateKey(StateScope.USER_IN_CHAT, new UserId("u-1"), new ChatId("c-1")),
                userInChatKey
        );
    }

    @Test
    void resolvesUserAndChatFromCallbackUpdate() {
        Update update = callbackUpdate("u-2", "c-2");

        StateKey key = StateKeyStrategies.userInChat().resolve(update);

        assertEquals(new UserId("u-2"), key.userId());
        assertEquals(new ChatId("c-2"), key.chatId());
    }

    @Test
    void failsWhenScopeDataCannotBeResolved() {
        Update update = new Update(
                new UpdateId("upd-3"),
                UpdateType.CHAT_MEMBER,
                null,
                null,
                null,
                Instant.parse("2026-01-01T00:00:00Z")
        );

        assertThrows(StateKeyResolutionException.class, () -> StateKeyStrategies.user().resolve(update));
        assertThrows(StateKeyResolutionException.class, () -> StateKeyStrategies.chat().resolve(update));
        assertThrows(StateKeyResolutionException.class, () -> StateKeyStrategies.userInChat().resolve(update));
    }

    @Test
    void separatesStateByScopeStrategy() {
        Update userAChatOne = messageUpdate("u-1", "c-1");
        Update userBChatOne = messageUpdate("u-2", "c-1");

        StateKey userScopeA = StateKeyStrategies.user().resolve(userAChatOne);
        StateKey userScopeB = StateKeyStrategies.user().resolve(userBChatOne);
        StateKey chatScopeA = StateKeyStrategies.chat().resolve(userAChatOne);
        StateKey chatScopeB = StateKeyStrategies.chat().resolve(userBChatOne);
        StateKey userInChatA = StateKeyStrategies.userInChat().resolve(userAChatOne);
        StateKey userInChatB = StateKeyStrategies.userInChat().resolve(userBChatOne);

        assertEquals(new UserId("u-1"), userScopeA.userId());
        assertEquals(new UserId("u-2"), userScopeB.userId());
        assertEquals(chatScopeA, chatScopeB);
        assertEquals(new ChatId("c-1"), chatScopeA.chatId());
        assertNull(chatScopeA.userId());

        assertEquals(new ChatId("c-1"), userInChatA.chatId());
        assertEquals(new ChatId("c-1"), userInChatB.chatId());
        assertEquals(new UserId("u-1"), userInChatA.userId());
        assertEquals(new UserId("u-2"), userInChatB.userId());
    }

    private static Update messageUpdate(String userId, String chatId) {
        Message message = new Message(
                new MessageId("msg-" + userId + "-" + chatId),
                new Chat(new ChatId(chatId), ChatType.PRIVATE, null, null, null),
                new User(new UserId(userId), null, null, null, null, false, null),
                "text",
                Instant.parse("2026-01-01T00:00:00Z"),
                null,
                null,
                null
        );

        return new Update(
                new UpdateId("upd-" + userId + "-" + chatId),
                UpdateType.MESSAGE,
                message,
                null,
                null,
                Instant.parse("2026-01-01T00:00:01Z")
        );
    }

    private static Update callbackUpdate(String userId, String chatId) {
        Message callbackMessage = new Message(
                new MessageId("msg-cb-" + chatId),
                new Chat(new ChatId(chatId), ChatType.PRIVATE, null, null, null),
                null,
                "callback",
                Instant.parse("2026-01-01T00:00:02Z"),
                null,
                null,
                null
        );

        Callback callback = new Callback(
                new CallbackId("cb-1"),
                "action:1",
                new User(new UserId(userId), null, null, null, null, false, null),
                callbackMessage,
                Instant.parse("2026-01-01T00:00:03Z")
        );

        return new Update(
                new UpdateId("upd-cb-" + userId + "-" + chatId),
                UpdateType.CALLBACK,
                null,
                callback,
                null,
                Instant.parse("2026-01-01T00:00:03Z")
        );
    }
}
