package ru.tardyon.botframework.spring.autoconfigure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
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
import ru.tardyon.botframework.screen.ScreenContext;
import ru.tardyon.botframework.screen.ScreenDefinition;
import ru.tardyon.botframework.screen.ScreenModel;
import ru.tardyon.botframework.screen.ScreenNavigator;
import ru.tardyon.botframework.screen.ScreenSession;
import ru.tardyon.botframework.spring.screen.annotation.OnScreenAction;
import ru.tardyon.botframework.spring.screen.annotation.OnScreenText;
import ru.tardyon.botframework.spring.screen.annotation.ScreenController;
import ru.tardyon.botframework.spring.screen.annotation.ScreenView;

class SpringScreenControllerRegistrarTest {

    @Test
    void oneControllerCanDefineMultipleScreens() {
        SpringScreenControllerRegistrar registrar = new SpringScreenControllerRegistrar();
        DemoScreenController controller = new DemoScreenController();

        List<ScreenDefinition> definitions = registrar.register(controller);
        assertEquals(2, definitions.size());
        assertTrue(definitions.stream().anyMatch(def -> "controller.home".equals(def.id())));
        assertTrue(definitions.stream().anyMatch(def -> "controller.settings".equals(def.id())));

        ScreenDefinition home = definitions.stream().filter(def -> "controller.home".equals(def.id())).findFirst().orElseThrow();
        ScreenContext textContext = testScreenContext(messageUpdate("hello"));
        home.onText(textContext, "hello").toCompletableFuture().join();
        assertEquals("hello", controller.lastText.get());
        assertNotNull(controller.lastMessage.get());

        ScreenContext callbackContext = testScreenContext(callbackUpdate("ui:act:save"));
        home.onAction(callbackContext, "save", Map.of("id", "42")).toCompletableFuture().join();
        assertEquals("42", controller.lastActionArgs.get().get("id"));
        assertNotNull(controller.lastRuntime.get());
        assertNotNull(controller.lastCallback.get());
    }

    @Test
    void invalidSignaturesFailAtRegistrationTime() {
        SpringScreenControllerRegistrar registrar = new SpringScreenControllerRegistrar();

        assertThrows(IllegalStateException.class, () -> registrar.register(new InvalidViewReturnController()));
        assertThrows(IllegalStateException.class, () -> registrar.register(new InvalidParameterController()));
    }

    @ScreenController
    static final class DemoScreenController {
        final AtomicReference<String> lastText = new AtomicReference<>();
        final AtomicReference<Map<String, String>> lastActionArgs = new AtomicReference<>();
        final AtomicReference<RuntimeContext> lastRuntime = new AtomicReference<>();
        final AtomicReference<Message> lastMessage = new AtomicReference<>();
        final AtomicReference<Callback> lastCallback = new AtomicReference<>();

        @ScreenView(screen = "controller.home")
        public ScreenModel home(ScreenContext context) {
            return ScreenModel.builder().title("Home").build();
        }

        @OnScreenText(screen = "controller.home")
        public CompletionStage<Void> onText(ScreenContext context, RuntimeContext runtimeContext, Message message, String text) {
            lastRuntime.set(runtimeContext);
            lastMessage.set(message);
            lastText.set(text);
            return CompletableFuture.completedFuture(null);
        }

        @OnScreenAction(screen = "controller.home", action = "save")
        public CompletionStage<Void> onAction(ScreenContext context, RuntimeContext runtimeContext, Callback callback, Map<String, String> args) {
            lastRuntime.set(runtimeContext);
            lastCallback.set(callback);
            lastActionArgs.set(args);
            return CompletableFuture.completedFuture(null);
        }

        @ScreenView(screen = "controller.settings")
        public CompletionStage<ScreenModel> settings() {
            return CompletableFuture.completedFuture(ScreenModel.builder().title("Settings").build());
        }
    }

    @ScreenController(autoRegister = false)
    static final class InvalidViewReturnController {
        @ScreenView(screen = "invalid")
        public String invalidView() {
            return "bad";
        }
    }

    @ScreenController(autoRegister = false)
    static final class InvalidParameterController {
        @ScreenView(screen = "invalid")
        public ScreenModel invalid(Integer value) {
            return ScreenModel.builder().title(String.valueOf(value)).build();
        }
    }

    private static ScreenContext testScreenContext(Update update) {
        RuntimeContext runtimeContext = new RuntimeContext(update);
        return new ScreenContext() {
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
    }

    private static Update messageUpdate(String text) {
        return new Update(
                new UpdateId("u-screen-msg"),
                UpdateType.MESSAGE,
                message(text),
                null,
                null,
                Instant.parse("2026-03-30T00:00:00Z")
        );
    }

    private static Update callbackUpdate(String data) {
        Message message = message("callback-source");
        return new Update(
                new UpdateId("u-screen-cb"),
                UpdateType.CALLBACK,
                null,
                new Callback(
                        new CallbackId("cb-screen-1"),
                        data,
                        user(),
                        message,
                        Instant.parse("2026-03-30T00:00:00Z")
                ),
                null,
                Instant.parse("2026-03-30T00:00:00Z")
        );
    }

    private static Message message(String text) {
        return new Message(
                new MessageId("m-screen-1"),
                new Chat(new ChatId("c-screen-1"), ChatType.PRIVATE, "chat", null, null),
                user(),
                text,
                Instant.parse("2026-03-30T00:00:00Z"),
                null,
                List.of(),
                List.of()
        );
    }

    private static User user() {
        return new User(new UserId("u-screen-1"), "demo", "Demo", "User", "Demo User", false, "en");
    }
}
