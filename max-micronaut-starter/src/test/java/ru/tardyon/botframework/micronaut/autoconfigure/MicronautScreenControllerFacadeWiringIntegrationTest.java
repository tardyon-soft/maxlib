package ru.tardyon.botframework.micronaut.autoconfigure;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.screen.ScreenRegistry;

class MicronautScreenControllerFacadeWiringIntegrationTest {

    @Test
    void screenControllerFacadeIsAutoRegisteredIntoScreenRegistry() {
        try (ApplicationContext context = context("screen-controller-wiring", Map.of())) {
            ScreenRegistry screenRegistry = context.getBean(ScreenRegistry.class);
            assertTrue(screenRegistry.find("controller.home").isPresent());
            assertTrue(screenRegistry.find("controller.profile").isPresent());
        }
    }

    @Test
    void explicitScreenControllerStillRegistersWhenComponentScanIsDisabled() {
        try (ApplicationContext context = context("screen-controller-wiring", Map.of(
                "max.bot.route-component-scan.enabled", "false"
        ))) {
            ScreenRegistry screenRegistry = context.getBean(ScreenRegistry.class);
            assertTrue(screenRegistry.find("controller.home").isPresent());
            assertTrue(screenRegistry.find("controller.profile").isPresent());
        }
    }

    @Test
    void autoDetectedScreenControllerIsDiscoveredAndRegistered() {
        try (ApplicationContext context = context("screen-controller-autodetected", Map.of())) {
            ScreenRegistry screenRegistry = context.getBean(ScreenRegistry.class);
            assertTrue(screenRegistry.find("autodetected.facade.home").isPresent());
        }
    }

    @Test
    void invalidScreenControllerCausesStartupFailure() {
        try (ApplicationContext context = ApplicationContext.builder()
                .properties(Map.of(
                        "spec.name", "screen-controller-invalid",
                        "max.bot.token", "test-token"
                ))
                .build()) {
            Throwable failure = null;
            try {
                context.start();
            } catch (Throwable t) {
                failure = t;
            }
            assertTrue(failure != null);
        }
    }

    private static ApplicationContext context(String specName, Map<String, Object> extraProperties) {
        java.util.LinkedHashMap<String, Object> properties = new java.util.LinkedHashMap<>();
        properties.put("spec.name", specName);
        properties.put("max.bot.token", "test-token");
        properties.putAll(extraProperties);
        return ApplicationContext.builder()
                .properties(properties)
                .start();
    }
}
