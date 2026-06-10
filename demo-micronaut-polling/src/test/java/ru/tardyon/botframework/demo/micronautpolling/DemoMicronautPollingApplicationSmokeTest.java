package ru.tardyon.botframework.demo.micronautpolling;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.micronaut.context.ApplicationContext;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.screen.ScreenRegistry;

class DemoMicronautPollingApplicationSmokeTest {

    @Test
    void contextLoads() {
        LinkedHashMap<String, Object> properties = new LinkedHashMap<>();
        properties.put("max.bot.token", "test-token");
        properties.put("max.bot.mode", "POLLING");
        properties.put("max.bot.polling.enabled", "false");

        try (ApplicationContext context = ApplicationContext.builder().properties(properties).start()) {
            Dispatcher dispatcher = context.getBean(Dispatcher.class);
            Router demoRouter = context.getBean(Router.class);
            ScreenRegistry screenRegistry = context.getBean(ScreenRegistry.class);

            assertNotNull(dispatcher);
            assertNotNull(demoRouter);
            assertNotNull(screenRegistry);
            assertNotNull(screenRegistry.find("facade.home").orElse(null));
        }
    }
}
