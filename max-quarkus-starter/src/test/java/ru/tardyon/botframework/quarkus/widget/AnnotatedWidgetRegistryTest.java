package ru.tardyon.botframework.quarkus.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import ru.tardyon.botframework.quarkus.widget.annotation.OnWidgetAction;
import ru.tardyon.botframework.quarkus.widget.annotation.Widget;
import ru.tardyon.botframework.quarkus.widget.annotation.WidgetController;
import ru.tardyon.botframework.screen.ScreenButton;
import ru.tardyon.botframework.screen.ScreenContext;
import ru.tardyon.botframework.screen.ScreenNavigator;
import ru.tardyon.botframework.screen.ScreenSession;
import ru.tardyon.botframework.screen.WidgetActions;
import ru.tardyon.botframework.screen.WidgetContext;
import ru.tardyon.botframework.screen.WidgetEffect;
import ru.tardyon.botframework.screen.WidgetView;

class AnnotatedWidgetRegistryTest {

    @Test
    void registersWidgetsAndDispatchesActions() {
        AnnotatedWidgetRegistry registry = new AnnotatedWidgetRegistry();
        DemoWidgetController controller = new DemoWidgetController();

        registry.register(controller);

        WidgetContext context = widgetContext("demo.widget");
        WidgetView view = registry.resolve(context, Map.of("seed", "value")).toCompletableFuture().join();
        assertEquals("Demo Widget", view.textLines().get(0));
        assertEquals(WidgetActions.callbackAction("demo.widget", "increment"), view.buttons().get(0).get(0).action());
        assertEquals(ScreenButton.Kind.CALLBACK, view.buttons().get(0).get(0).kind());
        assertEquals("https://example.com/docs", view.buttons().get(0).get(1).payload());
        assertEquals(ScreenButton.Kind.LINK, view.buttons().get(0).get(1).kind());

        WidgetEffect effect = registry.dispatch(context, "increment", Map.of("delta", "1")).toCompletableFuture().join();
        assertEquals(WidgetEffect.RERENDER, effect);
        assertEquals(1, controller.actionCalls.get());
        assertEquals("1", controller.lastDelta.get());
        assertEquals("increment", controller.lastAction.get());
        assertNotNull(controller.lastRuntime.get());
        assertNotNull(controller.lastMessage.get());
        assertNotNull(controller.lastCallback.get());
    }

    @Test
    void resolvesFallbacksForMissingWidgetAndAction() {
        AnnotatedWidgetRegistry registry = new AnnotatedWidgetRegistry();
        registry.register(new DemoWidgetController());

        WidgetView missingView = registry.resolve(widgetContext("missing.widget"), Map.of()).toCompletableFuture().join();
        WidgetEffect missingAction = registry.dispatch(widgetContext("demo.widget"), "missing", Map.of()).toCompletableFuture().join();
        WidgetEffect missingWidgetAction = registry.dispatch(widgetContext("missing.widget"), "missing", Map.of()).toCompletableFuture().join();

        assertTrue(missingView.textLines().get(0).contains("Widget not found: missing.widget"));
        assertTrue(missingView.buttons().isEmpty());
        assertEquals(WidgetEffect.NONE, missingAction);
        assertEquals(WidgetEffect.NONE, missingWidgetAction);
    }

    @Test
    void rejectsInvalidMappingsAndSignatures() {
        AnnotatedWidgetRegistry registry = new AnnotatedWidgetRegistry();

        assertThrows(IllegalArgumentException.class, () -> registry.register(new PlainObject()));
        assertThrows(IllegalStateException.class, () -> registry.register(new DuplicateWidgetController()));
        assertThrows(IllegalStateException.class, () -> registry.register(new DuplicateActionController()));
        assertThrows(IllegalStateException.class, () -> registry.register(new MissingRenderController()));
        assertThrows(IllegalStateException.class, () -> registry.register(new InvalidRenderReturnController()));
        assertThrows(IllegalStateException.class, () -> registry.register(new InvalidActionReturnController()));
        assertThrows(IllegalStateException.class, () -> registry.register(new InvalidParameterController()));
    }

    @Test
    void supportsAsyncRenderAndAsyncActionReturnTypes() {
        AnnotatedWidgetRegistry registry = new AnnotatedWidgetRegistry();
        registry.register(new AsyncWidgetController());

        WidgetView view = registry.resolve(widgetContext("async.widget"), Map.of()).toCompletableFuture().join();
        assertEquals("Async Widget", view.textLines().get(0));
        assertEquals(WidgetActions.callbackAction("async.widget", "go"), view.buttons().get(0).get(0).action());
        assertEquals(WidgetEffect.RERENDER, registry.dispatch(widgetContext("async.widget"), "go", Map.of()).toCompletableFuture().join());
    }

    @WidgetController
    static final class DemoWidgetController {
        final AtomicInteger actionCalls = new AtomicInteger();
        final AtomicReference<String> lastDelta = new AtomicReference<>();
        final AtomicReference<String> lastAction = new AtomicReference<>();
        final AtomicReference<RuntimeContext> lastRuntime = new AtomicReference<>();
        final AtomicReference<Message> lastMessage = new AtomicReference<>();
        final AtomicReference<Callback> lastCallback = new AtomicReference<>();

        @Widget(id = "demo.widget")
        public WidgetView render(WidgetContext context, ScreenContext screenContext, RuntimeContext runtimeContext, Message message) {
            assertEquals(screenContext, context.screen());
            assertEquals(runtimeContext, context.runtime());
            assertEquals(message, context.message());
            return WidgetView.of(
                    List.of("Demo Widget"),
                    List.of(List.of(
                            ScreenButton.of("Increment", "increment"),
                            ScreenButton.link("Docs", "https://example.com/docs")
                    ))
            );
        }

        @OnWidgetAction(widget = "demo.widget", action = "increment")
        public CompletionStage<WidgetEffect> increment(
                WidgetContext context,
                RuntimeContext runtimeContext,
                Message message,
                Callback callback,
                String action,
                Map<String, String> args
        ) {
            actionCalls.incrementAndGet();
            lastRuntime.set(runtimeContext);
            lastMessage.set(message);
            lastCallback.set(callback);
            lastAction.set(action);
            lastDelta.set(args.get("delta"));
            assertEquals("demo.widget", context.widgetId());
            return CompletableFuture.completedFuture(WidgetEffect.RERENDER);
        }
    }

    @WidgetController
    static final class AsyncWidgetController {
        @Widget(id = "async.widget")
        public CompletionStage<WidgetView> render() {
            return CompletableFuture.completedFuture(WidgetView.of(
                    List.of("Async Widget"),
                    List.of(List.of(ScreenButton.of("Go", "go")))
            ));
        }

        @OnWidgetAction(widget = "async.widget", action = "go")
        public CompletionStage<Void> go() {
            return CompletableFuture.completedFuture(null);
        }
    }

    @WidgetController(autoRegister = false)
    static final class DuplicateWidgetController {
        @Widget(id = "dup")
        public WidgetView first() {
            return WidgetView.of(List.of("first"), List.of());
        }

        @Widget(id = "dup")
        public WidgetView second() {
            return WidgetView.of(List.of("second"), List.of());
        }
    }

    @WidgetController(autoRegister = false)
    static final class DuplicateActionController {
        @Widget(id = "dup.action")
        public WidgetView render() {
            return WidgetView.of(List.of("dup"), List.of());
        }

        @OnWidgetAction(widget = "dup.action", action = "go")
        public void first() {
        }

        @OnWidgetAction(widget = "dup.action", action = "go")
        public void second() {
        }
    }

    @WidgetController(autoRegister = false)
    static final class MissingRenderController {
        @OnWidgetAction(widget = "missing.render", action = "go")
        public void invalid() {
        }
    }

    @WidgetController(autoRegister = false)
    static final class InvalidRenderReturnController {
        @Widget(id = "invalid.render")
        public String invalid() {
            return "bad";
        }
    }

    @WidgetController(autoRegister = false)
    static final class InvalidActionReturnController {
        @Widget(id = "invalid.action")
        public WidgetView render() {
            return WidgetView.of(List.of("invalid"), List.of());
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

    static final class PlainObject {
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
                return Map.of("seed", "value");
            }

            @Override
            public ScreenNavigator nav() {
                return null;
            }
        };
        return new WidgetContext(screenContext, widgetId, Map.of("seed", "value"), message, update.callback());
    }
}
