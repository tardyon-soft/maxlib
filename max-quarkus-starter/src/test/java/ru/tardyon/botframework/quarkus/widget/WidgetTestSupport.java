package ru.tardyon.botframework.quarkus.widget;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import ru.tardyon.botframework.dispatcher.RuntimeContext;
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
import ru.tardyon.botframework.screen.ScreenContext;
import ru.tardyon.botframework.screen.ScreenNavigator;
import ru.tardyon.botframework.screen.ScreenSession;
import ru.tardyon.botframework.screen.WidgetContext;

final class WidgetTestSupport {
    private WidgetTestSupport() {
    }

    static WidgetContext widgetContext(String widgetId) {
        Message message = new Message(
                new MessageId("m-widget-wiring-1"),
                new Chat(new ChatId("c-widget-wiring-1"), ChatType.PRIVATE, "chat", null, null),
                new User(new UserId("u-widget-wiring-1"), "demo", "Demo", "User", "Demo User", false, "en"),
                "hello",
                Instant.parse("2026-03-30T00:00:00Z"),
                null,
                List.of(),
                List.of()
        );
        Update update = new Update(
                new UpdateId("u-widget-wiring-1"),
                UpdateType.CALLBACK,
                message,
                new Callback(
                        new CallbackId("cb-widget-wiring-1"),
                        "payload",
                        new User(new UserId("u-widget-wiring-1"), "demo", "Demo", "User", "Demo User", false, "en"),
                        message,
                        Instant.parse("2026-03-30T00:00:00Z")
                ),
                null,
                Instant.parse("2026-03-30T00:00:00Z")
        );
        RuntimeContext runtimeContext = new RuntimeContext(update);
        ScreenContext screenContext = new ScreenContext() {
            @Override
            public RuntimeContext runtime() {
                return runtimeContext;
            }

            @Override
            public ru.tardyon.botframework.fsm.FSMContext fsm() {
                return null;
            }

            @Override
            public ScreenSession session() {
                return new ScreenSession("scope", List.of(), null, Instant.now());
            }

            @Override
            public Map<String, Object> params() {
                return Map.of();
            }

            @Override
            public ScreenNavigator nav() {
                return null;
            }
        };
        return new WidgetContext(screenContext, widgetId, Map.of(), message, update.callback());
    }
}
