package ru.tardyon.botframework.quarkus.route;

import java.time.Instant;
import java.util.List;
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

final class QuarkusRouteTestSupport {
    private QuarkusRouteTestSupport() {
    }

    static Update sampleUpdate(String text) {
        return new Update(
                new UpdateId("u-quarkus-route-1"),
                UpdateType.MESSAGE,
                new Message(
                        new MessageId("m-quarkus-route-1"),
                        new Chat(new ChatId("c-quarkus-route-1"), ChatType.PRIVATE, "chat", null, null),
                        new User(new UserId("u-quarkus-route-1"), "demo", "Demo", "User", "Demo User", false, "en"),
                        text,
                        Instant.parse("2026-03-13T00:00:00Z"),
                        null,
                        List.of(),
                        List.of()
                ),
                null,
                null,
                Instant.parse("2026-03-13T00:00:00Z")
        );
    }
}
