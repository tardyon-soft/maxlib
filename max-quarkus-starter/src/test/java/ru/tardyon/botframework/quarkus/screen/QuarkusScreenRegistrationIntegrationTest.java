package ru.tardyon.botframework.quarkus.screen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.dispatcher.DispatchStatus;
import ru.tardyon.botframework.screen.ScreenRegistry;
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
import ru.tardyon.botframework.quarkus.runtime.MaxBotProducer;

@QuarkusComponentTest({MaxBotProducer.class, QuarkusScreenAutoRegistrationBootstrap.class})
@TestConfigProperty(key = "max.bot.token", value = "test-token")
@TestConfigProperty(key = "max.bot.polling.enabled", value = "false")
@TestConfigProperty(key = "max.bot.route-component-scan.enabled", value = "true")
class QuarkusScreenRegistrationIntegrationTest {
    @Inject
    Dispatcher dispatcher;

    @Inject
    ScreenRegistry screenRegistry;

    @Test
    void autoRegisteredScreenIsRegisteredIntoScreenRegistry() {
        assertEquals(DispatchStatus.IGNORED, dispatcher.feedUpdate(sampleUpdate("ping")).toCompletableFuture().join().status());
        assertTrue(screenRegistry.find("sample").isPresent());
        assertTrue(dispatcher.applicationData().containsKey(ru.tardyon.botframework.dispatcher.RuntimeDataKey.application(
                "service:" + ScreenRegistry.class.getName(), ScreenRegistry.class
        )));
    }

    @Test
    void nonAutoRegisterScreenIsIgnored() {
        dispatcher.feedUpdate(sampleUpdate("ping")).toCompletableFuture().join();
        assertFalse(screenRegistry.find("disabled").isPresent());
    }

    @Test
    void autoDiscoveredScreenBeanIsRegisteredWhenScanEnabled() {
        dispatcher.feedUpdate(sampleUpdate("ping")).toCompletableFuture().join();
        assertTrue(screenRegistry.find("autodetected").isPresent());
    }

    private static Update sampleUpdate(String text) {
        return new Update(
                new UpdateId("u-quarkus-screen-1"),
                UpdateType.MESSAGE,
                new Message(
                        new MessageId("m-quarkus-screen-1"),
                        new Chat(new ChatId("c-quarkus-screen-1"), ChatType.PRIVATE, "chat", null, null),
                        new User(new UserId("u-quarkus-screen-1"), "demo", "Demo", "User", "Demo User", false, "en"),
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

    @Singleton
    @ru.tardyon.botframework.screen.annotation.Screen("sample")
    static final class AutoRegisteredScreenBean {
        @ru.tardyon.botframework.screen.annotation.Render
        public ru.tardyon.botframework.screen.ScreenModel render(ru.tardyon.botframework.screen.ScreenContext context) {
            return ru.tardyon.botframework.screen.ScreenModel.builder().title("sample").build();
        }
    }

    @Singleton
    @ru.tardyon.botframework.screen.annotation.Screen(value = "disabled", autoRegister = false)
    static final class DisabledScreenBean {
        @ru.tardyon.botframework.screen.annotation.Render
        public ru.tardyon.botframework.screen.ScreenModel render(ru.tardyon.botframework.screen.ScreenContext context) {
            return ru.tardyon.botframework.screen.ScreenModel.builder().title("disabled").build();
        }
    }
}
