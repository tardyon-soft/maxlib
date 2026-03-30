package ru.tardyon.botframework.spring.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.dispatcher.RuntimeContext;
import ru.tardyon.botframework.fsm.FSMContext;
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
import ru.tardyon.botframework.screen.ScreenButton;
import ru.tardyon.botframework.screen.ScreenContext;
import ru.tardyon.botframework.screen.ScreenNavigator;
import ru.tardyon.botframework.screen.ScreenSession;
import ru.tardyon.botframework.screen.WidgetContext;
import ru.tardyon.botframework.screen.WidgetEffect;
import ru.tardyon.botframework.screen.WidgetView;
import ru.tardyon.botframework.spring.widget.annotation.OnWidgetAction;
import ru.tardyon.botframework.spring.widget.annotation.Widget;
import ru.tardyon.botframework.spring.widget.annotation.WidgetController;

class AnnotatedWidgetRegistryTest {

    @Test
    void resolvesWidgetViewAndDispatchesAction() {
        AnnotatedWidgetRegistry registry = new AnnotatedWidgetRegistry();
        DemoWidgetController controller = new DemoWidgetController();
        registry.register(controller);

        WidgetContext context = widgetContext("demo.widget");
        WidgetView view = registry.resolve(context, Map.of("key", "value")).toCompletableFuture().join();
        assertEquals("Demo Widget", view.textLines().getFirst());
        assertEquals(1, view.buttons().size());
        assertNotNull(view.buttons().getFirst().getFirst().action());

        WidgetEffect effect = registry.dispatch(context, "increment", Map.of("delta", "1")).toCompletableFuture().join();
        assertEquals(WidgetEffect.RERENDER, effect);
        assertEquals(1, controller.actionCalls.get());
    }

    @Test
    void validatesDuplicateWidgetAndActionMappings() {
        AnnotatedWidgetRegistry registry = new AnnotatedWidgetRegistry();
        assertThrows(IllegalStateException.class, () -> registry.register(new DuplicateWidgetController()));
        assertThrows(IllegalStateException.class, () -> registry.register(new DuplicateActionController()));
    }

    @WidgetController
    static final class DemoWidgetController {
        final AtomicInteger actionCalls = new AtomicInteger();

        @Widget(id = "demo.widget")
        public WidgetView render(WidgetContext context) {
            return WidgetView.of(
                    List.of("Demo Widget"),
                    List.of(List.of(ScreenButton.of("Increment", "increment")))
            );
        }

        @OnWidgetAction(widget = "demo.widget", action = "increment")
        public CompletionStage<WidgetEffect> increment(Map<String, String> args) {
            actionCalls.incrementAndGet();
            return CompletableFuture.completedFuture(WidgetEffect.RERENDER);
        }
    }

    @WidgetController(autoRegister = false)
    static final class DuplicateWidgetController {
        @Widget(id = "dup")
        public WidgetView a() {
            return WidgetView.of(List.of("a"), List.of());
        }

        @Widget(id = "dup")
        public WidgetView b() {
            return WidgetView.of(List.of("b"), List.of());
        }
    }

    @WidgetController(autoRegister = false)
    static final class DuplicateActionController {
        @Widget(id = "dup.action")
        public WidgetView base() {
            return WidgetView.of(List.of("base"), List.of());
        }

        @OnWidgetAction(widget = "dup.action", action = "go")
        public void first() {
        }

        @OnWidgetAction(widget = "dup.action", action = "go")
        public void second() {
        }
    }

    private static WidgetContext widgetContext(String widgetId) {
        Update update = new Update(
                new UpdateId("u-widget-1"),
                UpdateType.CALLBACK,
                null,
                new Callback(
                        new CallbackId("cb-widget-1"),
                        "payload",
                        new User(new UserId("u-widget-1"), "demo", "Demo", "User", "Demo User", false, "en"),
                        new Message(
                                new MessageId("m-widget-1"),
                                new Chat(new ChatId("c-widget-1"), ChatType.PRIVATE, "chat", null, null),
                                new User(new UserId("u-widget-1"), "demo", "Demo", "User", "Demo User", false, "en"),
                                "text",
                                Instant.parse("2026-03-30T00:00:00Z"),
                                null,
                                List.of(),
                                List.of()
                        ),
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
        return new WidgetContext(screenContext, widgetId, Map.of(), runtimeContext.update().message(), runtimeContext.update().callback());
    }
}

