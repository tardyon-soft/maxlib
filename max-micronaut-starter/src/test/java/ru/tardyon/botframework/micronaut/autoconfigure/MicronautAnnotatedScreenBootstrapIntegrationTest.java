package ru.tardyon.botframework.micronaut.autoconfigure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micronaut.context.ApplicationContext;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.screen.ScreenRegistry;

class MicronautAnnotatedScreenBootstrapIntegrationTest {

    @Test
    void autoRegistersAnnotatedScreenBeansIntoScreenRegistry() {
        try (ApplicationContext context = context("screen-auto", Map.of())) {
            assertTrue(context.getBean(ScreenRegistry.class).find("sample").isPresent());
        }
    }

    @Test
    void doesNotAutoRegisterAnnotatedScreenBeansWhenDisabled() {
        try (ApplicationContext context = context("screen-disabled", Map.of())) {
            assertFalse(context.getBean(ScreenRegistry.class).find("disabled").isPresent());
        }
    }

    @Test
    void explicitAnnotatedScreenBeanStillRegistersWhenComponentScanIsDisabled() {
        try (ApplicationContext context = context("screen-auto", Map.of(
                "max.bot.route-component-scan.enabled", "false"
        ))) {
            assertTrue(context.getBean(ScreenRegistry.class).find("sample").isPresent());
        }
    }

    @Test
    void failsStartupWhenAnnotatedScreenBeanIsInvalid() {
        try (ApplicationContext context = ApplicationContext.builder()
                .properties(Map.of(
                        "spec.name", "screen-invalid",
                        "max.bot.token", "test-token"
                ))
                .build()) {
            Exception failure = null;
            try {
                context.start();
            } catch (Exception e) {
                failure = e;
            }
            assertNotNull(failure);
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
