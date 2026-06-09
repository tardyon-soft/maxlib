package ru.tardyon.botframework.micronaut.autoconfigure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.dispatcher.RuntimeContext;
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
import ru.tardyon.botframework.micronaut.widget.annotation.Widget;
import ru.tardyon.botframework.micronaut.widget.annotation.WidgetController;
import ru.tardyon.botframework.screen.ScreenContext;
import ru.tardyon.botframework.screen.ScreenNavigator;
import ru.tardyon.botframework.screen.ScreenSession;
import ru.tardyon.botframework.screen.WidgetContext;
import ru.tardyon.botframework.screen.WidgetView;
import ru.tardyon.botframework.screen.WidgetViewResolver;

class MicronautWidgetControllerFacadeWiringIntegrationTest {

    @Test
    void widgetControllerFacadeIsAutoRegisteredIntoRuntimeResolver() {
        try (ApplicationContext context = context("widget-wiring").start()) {
            WidgetViewResolver widgetViewResolver = context.getBean(WidgetViewResolver.class);
            WidgetContext widgetContext = new WidgetContext(testScreenContext(), "autodetected.widget", Map.of(), null, null);
            WidgetView view = widgetViewResolver.resolve(widgetContext, Map.of()).toCompletableFuture().join();
            assertEquals("autodetected widget", view.textLines().get(0));
            assertTrue(view.buttons().isEmpty());
        }
    }

    @Test
    void explicitWidgetControllerStillRegistersWhenComponentScanIsDisabled() {
        try (ApplicationContext context = ApplicationContext.builder()
                .properties(Map.of(
                        "spec.name", "widget-wiring",
                        "max.bot.token", "test-token",
                        "max.bot.route-component-scan.enabled", "false"
                ))
                .start()) {
            WidgetViewResolver widgetViewResolver = context.getBean(WidgetViewResolver.class);
            WidgetContext widgetContext = new WidgetContext(testScreenContext(), "widget.home", Map.of(), null, null);
            WidgetView view = widgetViewResolver.resolve(widgetContext, Map.of()).toCompletableFuture().join();
            assertEquals("widget home", view.textLines().get(0));
        }
    }

    @Test
    void widgetControllerAutoRegisterFalseIsIgnored() {
        try (ApplicationContext context = context("widget-disabled").start()) {
            WidgetViewResolver widgetViewResolver = context.getBean(WidgetViewResolver.class);
            WidgetView view = widgetViewResolver.resolve(new WidgetContext(testScreenContext(), "disabled.widget", Map.of(), null, null), Map.of())
                    .toCompletableFuture()
                    .join();
            assertTrue(view.textLines().get(0).contains("Widget not found"));
        }
    }

    @Test
    void invalidWidgetControllerCausesStartupFailure() {
        assertThrows(Throwable.class, () -> context("widget-invalid").start());
    }

    private static ApplicationContext context(String specName) {
        return ApplicationContext.builder()
                .properties(Map.of(
                        "spec.name", specName,
                        "max.bot.token", "test-token"
                ))
                .build();
    }

    private static ScreenContext testScreenContext() {
        Update update = new Update(
                new UpdateId("u-widget-wiring-1"),
                UpdateType.MESSAGE,
                new Message(
                        new MessageId("m-widget-wiring-1"),
                        new Chat(new ChatId("c-widget-wiring-1"), ChatType.PRIVATE, "chat", null, null),
                        new User(new UserId("u-widget-wiring-1"), "demo", "Demo", "User", "Demo User", false, "en"),
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
        RuntimeContext runtimeContext = new RuntimeContext(update);
        return new ScreenContext() {
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
    }

    @Singleton
    @Requires(property = "spec.name", value = "widget-wiring")
    @WidgetController
    static final class SampleWidgetController {
        @Widget(id = "widget.home")
        public WidgetView render() {
            return WidgetView.of(List.of("widget home"), List.of());
        }
    }

    @Singleton
    @Requires(property = "spec.name", value = "widget-disabled")
    @WidgetController(autoRegister = false)
    static final class DisabledWidgetController {
        @Widget(id = "disabled.widget")
        public WidgetView render() {
            return WidgetView.of(List.of("disabled"), List.of());
        }
    }

    @Singleton
    @Requires(property = "spec.name", value = "widget-invalid")
    @WidgetController
    static final class InvalidWidgetController {
        @Widget(id = "invalid.widget")
        public String render() {
            return "bad";
        }
    }
}
