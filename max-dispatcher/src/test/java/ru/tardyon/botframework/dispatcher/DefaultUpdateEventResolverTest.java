package ru.tardyon.botframework.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

class DefaultUpdateEventResolverTest {

    private final DefaultUpdateEventResolver resolver = new DefaultUpdateEventResolver();

    @Test
    void resolveMapsMessageUpdate() {
        UpdateEventResolution resolution = resolver.resolve(messageUpdate(UpdateType.MESSAGE));

        assertEquals(ResolvedUpdateEventType.MESSAGE, resolution.eventType());
    }

    @Test
    void resolveMapsCallbackUpdate() {
        UpdateEventResolution resolution = resolver.resolve(callbackUpdate(UpdateType.CALLBACK));

        assertEquals(ResolvedUpdateEventType.CALLBACK, resolution.eventType());
    }

    @Test
    void resolveUsesPayloadFallbackForUnknownType() {
        UpdateEventResolution resolution = resolver.resolve(messageUpdate(UpdateType.UNKNOWN));

        assertEquals(ResolvedUpdateEventType.MESSAGE, resolution.eventType());
    }

    @Test
    void resolveReturnsUnsupportedForUnknownWithoutSupportedPayload() {
        Update update = new Update(
                new UpdateId("u-unsupported"),
                UpdateType.CHAT_MEMBER,
                null,
                null,
                null,
                Instant.parse("2026-03-12T00:00:00Z")
        );

        UpdateEventResolution resolution = resolver.resolve(update);

        assertEquals(ResolvedUpdateEventType.UNSUPPORTED, resolution.eventType());
    }

    private static Update messageUpdate(UpdateType type) {
        return new Update(
                new UpdateId("u-message"),
                type,
                new Message(
                        new MessageId("m-1"),
                        new Chat(new ChatId("c-1"), ChatType.PRIVATE, "title", null, null),
                        user(),
                        "hello",
                        Instant.parse("2026-03-12T00:00:00Z"),
                        null,
                        java.util.List.of(),
                        java.util.List.of()
                ),
                null,
                null,
                Instant.parse("2026-03-12T00:00:00Z")
        );
    }

    private static Update callbackUpdate(UpdateType type) {
        return new Update(
                new UpdateId("u-callback"),
                type,
                null,
                new Callback(
                        new CallbackId("cb-1"),
                        "action:1",
                        user(),
                        new Message(
                                new MessageId("m-2"),
                                new Chat(new ChatId("c-2"), ChatType.PRIVATE, "title", null, null),
                                user(),
                                "source",
                                Instant.parse("2026-03-12T00:00:00Z"),
                                null,
                                java.util.List.of(),
                                java.util.List.of()
                        ),
                        Instant.parse("2026-03-12T00:00:01Z")
                ),
                null,
                Instant.parse("2026-03-12T00:00:01Z")
        );
    }

    private static User user() {
        return new User(new UserId("u-1"), "user", "First", "Last", "First Last", false, "ru");
    }
}

