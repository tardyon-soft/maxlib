package ru.tardyon.botframework.quarkus.polling;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusComponentTest({ru.tardyon.botframework.quarkus.runtime.MaxBotProducer.class, QuarkusPollingFactory.class, QuarkusPollingBootstrap.class, QuarkusPollingLifecycle.class})
@TestConfigProperty(key = "max.bot.token", value = "test-token")
@TestConfigProperty(key = "max.bot.mode", value = "WEBHOOK")
@TestConfigProperty(key = "max.bot.polling.enabled", value = "false")
class QuarkusPollingDisabledTest {
    @Inject
    QuarkusPollingBootstrap bootstrap;

    @Inject
    QuarkusPollingLifecycle lifecycle;

    @Test
    void disabledPollingDoesNotCreateRuntimeBeans() {
        assertFalse(bootstrap.longPollingRunner().isPresent());
        assertFalse(bootstrap.isRunning());
        assertFalse(lifecycle.isRunning());
    }
}
