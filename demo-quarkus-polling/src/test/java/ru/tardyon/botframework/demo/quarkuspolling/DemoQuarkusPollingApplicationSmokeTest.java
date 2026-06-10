package ru.tardyon.botframework.demo.quarkuspolling;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.demo.springpolling.DemoCounterWidgetController;
import ru.tardyon.botframework.demo.springpolling.FacadeScreenController;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.quarkus.route.QuarkusRouteAutoRegistrationBootstrap;
import ru.tardyon.botframework.quarkus.runtime.MaxBotProducer;
import ru.tardyon.botframework.quarkus.screen.QuarkusScreenAutoRegistrationBootstrap;
import ru.tardyon.botframework.quarkus.widget.QuarkusWidgetAutoRegistrationBootstrap;
import ru.tardyon.botframework.screen.ScreenRegistry;

@QuarkusComponentTest({
        MaxBotProducer.class,
        QuarkusRouteAutoRegistrationBootstrap.class,
        QuarkusScreenAutoRegistrationBootstrap.class,
        QuarkusWidgetAutoRegistrationBootstrap.class,
        DemoQuarkusRouterFactory.class,
        FacadeScreenController.class,
        DemoCounterWidgetController.class
})
@TestConfigProperty(key = "max.bot.token", value = "test-token")
@TestConfigProperty(key = "max.bot.mode", value = "POLLING")
@TestConfigProperty(key = "max.bot.polling.enabled", value = "false")
class DemoQuarkusPollingApplicationSmokeTest {
    @Inject
    Dispatcher dispatcher;

    @Inject
    Router demoRouterMax;

    @Inject
    ScreenRegistry screenRegistry;

    @Test
    void contextLoads() {
        assertNotNull(dispatcher);
        assertNotNull(demoRouterMax);
        assertNotNull(screenRegistry);
        assertNotNull(screenRegistry.find("facade.home").orElse(null));
    }
}
