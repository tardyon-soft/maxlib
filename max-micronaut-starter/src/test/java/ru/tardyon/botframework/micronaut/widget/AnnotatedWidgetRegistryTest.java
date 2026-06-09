package ru.tardyon.botframework.micronaut.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
import ru.tardyon.botframework.screen.WidgetActions;
import ru.tardyon.botframework.screen.WidgetContext;
import ru.tardyon.botframework.screen.WidgetEffect;
import ru.tardyon.botframework.screen.WidgetView;
import ru.tardyon.botframework.micronaut.widget.annotation.OnWidgetAction;
import ru.tardyon.botframework.micronaut.widget.annotation.Widget;
import ru.tardyon.botframework.micronaut.widget.annotation.WidgetController;

class AnnotatedWidgetRegistryTest {

    @Test
    void resolvesWidgetViewAndDispatchesAction() {
        AnnotatedWidgetRegistry registry = new AnnotatedWidgetRegistry();
        DemoWidgetController controller = new DemoWidgetController();
        registry.register(controller);

        WidgetContext context = widgetContext("demo.widget");
        WidgetView view = registry.resolve(context, Map.of("key", "value")).toCompletableFuture().join();
        assertEquals("Demo Widget", view.textLines().get(0));
        assertEquals(1, view.buttons().size());
        assertEquals(WidgetActions.callbackAction("demo.widget", "increment"), view.buttons().get(0).get(0).action());

        WidgetEffect effect = registry.dispatch(context, "increment", Map.of("delta", "1")).toCompletableFuture().join();
        assertEquals(WidgetEffect.RERENDER, effect);
        assertEquals(1, controller.actionCalls.get());
        assertEquals("1", controller.lastDelta.get());
    }

    @Test
    void keepsNonCallbackButtonsUntouched() {
        AnnotatedWidgetRegistry registry = new AnnotatedWidgetRegistry();
        registry.register(new MixedButtonsWidgetController());

        WidgetView view = registry.resolve(widgetContext("demo.mixed"), Map.of()).toCompletableFuture().join();

        assertEquals("https://example.com/docs", view.buttons().get(0).get(1).payload());
        assertEquals(ScreenButton.Kind.LINK, view.buttons().get(0).get(1).kind());
        assertEquals("hello", view.buttons().get(0).get(2).payload());
        assertEquals(ScreenButton.Kind.MESSAGE, view.buttons().get(0).get(2).kind());
    }

    @Test
    void resolvesWidgetAndActionFallbacks() {
        AnnotatedWidgetRegistry registry = new AnnotatedWidgetRegistry();
        registry.register(new DemoWidgetController());

        WidgetView missingView = registry.resolve(widgetContext("missing.widget"), Map.of()).toCompletableFuture().join();
        WidgetEffect missingAction = registry.dispatch(widgetContext("demo.widget"), "unknown", Map.of()).toCompletableFuture().join();
        WidgetEffect missingWidgetAction = registry.dispatch(widgetContext("missing.widget"), "unknown", Map.of()).toCompletableFuture().join();

        assertTrue(missingView.textLines().get(0).contains("Widget not found: missing.widget"));
        assertEquals(WidgetEffect.NONE, missingAction);
        assertEquals(WidgetEffect.NONE, missingWidgetAction);
    }

    @Test
    void validatesDuplicateWidgetAndActionMappings() {
        AnnotatedWidgetRegistry registry = new AnnotatedWidgetRegistry();
        assertThrows(IllegalStateException.class, () -> registry.register(new DuplicateWidgetController()));
        assertThrows(IllegalStateException.class, () -> registry.register(new DuplicateActionController()));
    }

    @Test
    void validatesSignaturesAndParameters() {
        AnnotatedWidgetRegistry registry = new AnnotatedWidgetRegistry();
        assertThrows(IllegalStateException.class, () -> registry.register(new InvalidRenderReturnController()));
        assertThrows(IllegalStateException.class, () -> registry.register(new InvalidActionReturnController()));
        assertThrows(IllegalStateException.class, () -> registry.register(new InvalidParameterController()));
        assertThrows(IllegalStateException.class, () -> registry.register(new MissingRenderController()));
    }

    @WidgetController
    static final class DemoWidgetController {
        final AtomicInteger actionCalls = new AtomicInteger();
        final AtomicReference<String> lastDelta = new AtomicReference<>();

        @Widget(id = "demo.widget")
        public WidgetView render(WidgetContext context, ScreenContext screenContext, RuntimeContext runtimeContext, Message message) {
            assertEquals("payload", message.text());
            assertEquals(screenContext, context.screen());
            assertEquals(runtimeContext, context.runtime());
            return WidgetView.of(
                    List.of("Demo Widget"),
                    List.of(List.of(ScreenButton.of("Increment", "increment")))
            );
        }

        @OnWidgetAction(widget = "demo.widget", action = "increment")
        public CompletionStage<WidgetEffect> increment(WidgetContext context, String action, Map<String, String> args) {
            actionCalls.incrementAndGet();
            lastDelta.set(args.get("delta"));
            assertEquals("increment", action);
            assertEquals("demo.widget", context.widgetId());
            return CompletableFuture.completedFuture(WidgetEffect.RERENDER);
        }
    }

    @WidgetController
    static final class MixedButtonsWidgetController {
        @Widget(id = "demo.mixed")
        public WidgetView render() {
            return WidgetView.of(
                    List.of("Mixed"),
                    List.of(List.of(
                            ScreenButton.of("Increment", "increment"),
                            ScreenButton.link("Docs", "https://example.com/docs"),
                            ScreenButton.message("Send", "hello")
                    ))
            );
        }

        @OnWidgetAction(widget = "demo.mixed", action = "increment")
        public void increment() {
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

    @WidgetController(autoRegister = false)
    static final class InvalidRenderReturnController {
        @Widget(id = "invalid.render")
        public String render() {
            return "bad";
        }
    }

    @WidgetController(autoRegister = false)
    static final class InvalidActionReturnController {
        @Widget(id = "invalid.action")
        public WidgetView render() {
            return WidgetView.of(List.of("bad"), List.of());
        }

        @OnWidgetAction(widget = "invalid.action", action = "go")
        public String invalid() {
            return "bad";
        }
    }

    @WidgetController(autoRegister = false)
    static final class InvalidParameterController {
        @Widget(id = "invalid.param")
        public WidgetView render(Integer value) {
            return WidgetView.of(List.of(String.valueOf(value)), List.of());
        }
    }

    @WidgetController(autoRegister = false)
    static final class MissingRenderController {
        @OnWidgetAction(widget = "missing.render", action = "go")
        public void invalid() {
        }
    }

    private static WidgetContext widgetContext(String widgetId) {
        Message message = new Message(
                new MessageId("m-widget-1"),
                new Chat(new ChatId("c-widget-1"), ChatType.PRIVATE, "chat", null, null),
                new User(new UserId("u-widget-1"), "demo", "Demo", "User", "Demo User", false, "en"),
                "payload",
                Instant.parse("2026-03-30T00:00:00Z"),
                null,
                List.of(),
                List.of()
        );
        Update update = new Update(
                new UpdateId("u-widget-1"),
                UpdateType.CALLBACK,
                message,
                new Callback(
                        new CallbackId("cb-widget-1"),
                        "payload",
                        new User(new UserId("u-widget-1"), "demo", "Demo", "User", "Demo User", false, "en"),
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
            public FSMContext fsm() {
                return null;
            }

            @Override
            public ScreenSession session() {
                return new ScreenSession("scope", List.of(), null, Instant.now());
            }

            @Override
            public Map<String, Object> params() {
                return Map.of("key", "value");
            }

            @Override
            public ScreenNavigator nav() {
                return null;
            }
        };
        return new WidgetContext(screenContext, widgetId, Map.of("key", "value"), runtimeContext.update().message(), runtimeContext.update().callback());
    }
}
