package ru.tardyon.botframework.spring.autoconfigure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
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
import ru.tardyon.botframework.screen.ScreenContext;
import ru.tardyon.botframework.screen.ScreenNavigator;
import ru.tardyon.botframework.screen.ScreenSession;
import ru.tardyon.botframework.screen.WidgetContext;
import ru.tardyon.botframework.screen.WidgetViewResolver;

@SpringBootTest(
        classes = WidgetControllerFacadeWiringIntegrationTest.TestApp.class,
        properties = {
                "max.bot.token=test-token",
                "max.bot.polling.enabled=false",
                "max.bot.route-component-scan.enabled=true"
        },
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class WidgetControllerFacadeWiringIntegrationTest {

    @Autowired
    private WidgetViewResolver widgetViewResolver;

    @Test
    void widgetControllerFacadeIsAutoRegisteredIntoRuntimeResolver() {
        WidgetContext context = new WidgetContext(testScreenContext(), "autodetected.widget", Map.of(), null, null);
        var view = widgetViewResolver.resolve(context, Map.of()).toCompletableFuture().join();
        assertEquals("autodetected widget", view.textLines().getFirst());
    }

    @Configuration
    @EnableAutoConfiguration
    static class TestApp {
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
}

