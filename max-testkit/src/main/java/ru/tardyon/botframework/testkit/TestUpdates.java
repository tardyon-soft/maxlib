package ru.tardyon.botframework.testkit;

import java.util.Objects;
import ru.tardyon.botframework.model.Update;

/**
 * Backward-compatible fixture shortcuts.
 *
 * <p>Prefer using {@link UpdateFixtures} for richer builder-style scenarios.</p>
 */
public final class TestUpdates {
    private TestUpdates() {
    }

    public static Update message(String text) {
        return UpdateFixtures.message()
                .text(text)
                .build();
    }

    public static Update message(String userId, String chatId, String text) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(chatId, "chatId");
        return UpdateFixtures.message()
                .userId(userId)
                .chatId(chatId)
                .text(text)
                .updateId("upd-msg-" + userId + "-" + chatId)
                .messageId("msg-" + userId + "-" + chatId)
                .build();
    }

    public static Update callback(String callbackData) {
        return UpdateFixtures.callback()
                .data(callbackData)
                .build();
    }

    public static Update callback(String userId, String chatId, String callbackData) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(chatId, "chatId");
        return UpdateFixtures.callback()
                .userId(userId)
                .chatId(chatId)
                .data(callbackData)
                .updateId("upd-cb-" + userId + "-" + chatId)
                .callbackId("cb-" + userId + "-" + chatId)
                .sourceMessageId("msg-" + userId + "-" + chatId)
                .build();
    }
}
