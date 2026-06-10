package ru.tardyon.botframework.quarkus.screen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.screen.ScreenRegistry;
import ru.tardyon.botframework.quarkus.runtime.MaxBotProducer;

@QuarkusComponentTest({
        MaxBotProducer.class,
        QuarkusScreenAutoRegistrationBootstrap.class,
        SampleScreenController.class,
        DisabledScreenController.class
})
@TestConfigProperty(key = "max.bot.token", value = "test-token")
@TestConfigProperty(key = "max.bot.polling.enabled", value = "false")
@TestConfigProperty(key = "max.bot.route-component-scan.enabled", value = "false")
class QuarkusScreenControllerWiringTest {
    @Inject
    Dispatcher dispatcher;

    @Inject
    ScreenRegistry screenRegistry;

    @Test
    void screenControllerFacadeIsAutoRegisteredIntoScreenRegistry() {
        assertTrue(screenRegistry.find("controller.home").isPresent());
        assertTrue(screenRegistry.find("controller.profile").isPresent());
        assertFalse(screenRegistry.find("disabled.controller").isPresent());
    }
}
