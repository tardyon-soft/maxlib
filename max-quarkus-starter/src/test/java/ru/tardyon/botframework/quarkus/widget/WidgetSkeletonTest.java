package ru.tardyon.botframework.quarkus.widget;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class WidgetSkeletonTest {
    @Test
    void registryClassExists() {
        assertNotNull(AnnotatedWidgetRegistry.class);
    }
}
