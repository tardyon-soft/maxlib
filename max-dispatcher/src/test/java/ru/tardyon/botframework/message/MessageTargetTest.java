package ru.tardyon.botframework.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.UserId;

class MessageTargetTest {

    @Test
    void createsChatTarget() {
        MessageTarget target = MessageTarget.chat(new ChatId("chat-1"));

        assertEquals(MessageTarget.Kind.CHAT, target.kind());
        assertTrue(target.chatId().isPresent());
        assertEquals("chat-1", target.chatId().orElseThrow().value());
        assertTrue(target.userId().isEmpty());
    }

    @Test
    void createsUserTarget() {
        MessageTarget target = MessageTarget.user(new UserId("user-1"));

        assertEquals(MessageTarget.Kind.USER, target.kind());
        assertTrue(target.userId().isPresent());
        assertEquals("user-1", target.userId().orElseThrow().value());
        assertTrue(target.chatId().isEmpty());
    }

    @Test
    void chatTargetConvertsToSdkChatIdWithoutResolverCall() {
        MessageTarget target = MessageTarget.chat(new ChatId("chat-42"));
        AtomicInteger resolverCalls = new AtomicInteger();

        ChatId resolved = target.toChatId(userId -> {
            resolverCalls.incrementAndGet();
            return new ChatId("mapped");
        });

        assertEquals("chat-42", resolved.value());
        assertEquals(0, resolverCalls.get());
    }

    @Test
    void userTargetConvertsToSdkChatIdViaResolver() {
        MessageTarget target = MessageTarget.user(new UserId("user-77"));

        ChatId resolved = target.toChatId(userId -> new ChatId("chat-for-" + userId.value()));

        assertEquals("chat-for-user-77", resolved.value());
    }

    @Test
    void userTargetRequiresResolver() {
        MessageTarget target = MessageTarget.user(new UserId("user-1"));

        assertThrows(NullPointerException.class, () -> target.toChatId(null));
    }

    @Test
    void userTargetFailsWhenResolverReturnsNull() {
        MessageTarget target = MessageTarget.user(new UserId("user-1"));

        assertThrows(NullPointerException.class, () -> target.toChatId(userId -> null));
    }

    @Test
    void rejectsNullTargetIds() {
        assertThrows(NullPointerException.class, () -> MessageTarget.chat(null));
        assertThrows(NullPointerException.class, () -> MessageTarget.user(null));
    }
}
