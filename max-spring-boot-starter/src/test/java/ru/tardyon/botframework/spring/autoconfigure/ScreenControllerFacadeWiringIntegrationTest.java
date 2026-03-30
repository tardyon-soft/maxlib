package ru.tardyon.botframework.spring.autoconfigure;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import ru.tardyon.botframework.screen.ScreenRegistry;

@SpringBootTest(
        classes = ScreenControllerFacadeWiringIntegrationTest.TestApp.class,
        properties = {
                "max.bot.token=test-token",
                "max.bot.polling.enabled=false",
                "max.bot.route-component-scan.enabled=true"
        },
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class ScreenControllerFacadeWiringIntegrationTest {

    @Autowired
    private ScreenRegistry screenRegistry;

    @Test
    void screenControllerFacadeIsAutoRegisteredIntoScreenRegistry() {
        assertTrue(screenRegistry.find("autodetected.facade.home").isPresent());
    }

    @Configuration
    @EnableAutoConfiguration
    static class TestApp {
    }
}
