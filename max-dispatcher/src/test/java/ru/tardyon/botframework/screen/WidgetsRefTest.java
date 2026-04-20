package ru.tardyon.botframework.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.dispatcher.RuntimeContext;
import ru.tardyon.botframework.fsm.FSMContext;
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

class WidgetsRefTest {

    @Test
    void resolvesWidgetByIdThroughRuntimeResolver() {
        RuntimeContext runtimeContext = new RuntimeContext(sampleUpdate());
        runtimeContext.putData(
                WidgetRuntimeSupport.WIDGET_VIEW_RESOLVER_KEY,
                (context, params) -> CompletableFuture.completedFuture(
                        WidgetView.of(List.of("resolved:" + context.widgetId()), List.of())
                )
        );

        ScreenContext screenContext = new ScreenContext() {
            @Override
            public RuntimeContext runtime() {
                return runtimeContext;
            }

            @Override
            public FSMContext fsm() {
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

        CompletionStage<WidgetRender> stage = Widgets.ref("demo.widget").render(screenContext);
        WidgetRender render = stage.toCompletableFuture().join();
        assertEquals("resolved:demo.widget", render.textLines().get(0));
    }

    private static Update sampleUpdate() {
        return new Update(
                new UpdateId("u-widget-ref-1"),
                UpdateType.MESSAGE,
                new Message(
                        new MessageId("m-widget-ref-1"),
                        new Chat(new ChatId("c-widget-ref-1"), ChatType.PRIVATE, "chat", null, null),
                        new User(new UserId("u-widget-ref-1"), "demo", "Demo", "User", "Demo User", false, "en"),
                        "hello",
                        Instant.parse("2026-03-30T00:00:00Z"),
                        null,
                        List.of(),
                        List.of()
                ),
                null,
                null,
                Instant.parse("2026-03-30T00:00:00Z")
        );
    }
}

