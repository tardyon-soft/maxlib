package ru.tardyon.botframework.quarkus.screen;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ScreenSkeletonTest {
    @Test
    void registrarClassExists() {
        assertNotNull(QuarkusScreenControllerRegistrar.class);
    }
}
