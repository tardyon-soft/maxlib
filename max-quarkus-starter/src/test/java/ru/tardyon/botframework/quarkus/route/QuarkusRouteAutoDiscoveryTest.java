package ru.tardyon.botframework.quarkus.route;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.dispatcher.DispatchResult;
import ru.tardyon.botframework.dispatcher.DispatchStatus;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.quarkus.runtime.MaxBotProducer;

@QuarkusComponentTest({MaxBotProducer.class, QuarkusRouteAutoRegistrationBootstrap.class})
@TestConfigProperty(key = "max.bot.token", value = "test-token")
@TestConfigProperty(key = "max.bot.polling.enabled", value = "false")
@TestConfigProperty(key = "max.bot.route-component-scan.enabled", value = "true")
class QuarkusRouteAutoDiscoveryTest {
    @Inject
    Dispatcher dispatcher;

    @BeforeEach
    void resetCounter() {
        QuarkusRouteTestState.AUTO_DETECTED_CALLS.set(0);
    }

    @Test
    void autoDiscoveredRouteBeanIsRegisteredWhenScanEnabled() {
        DispatchResult result = dispatcher.feedUpdate(QuarkusRouteTestSupport.sampleUpdate("/start")).toCompletableFuture().join();
        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals(1, QuarkusRouteTestState.AUTO_DETECTED_CALLS.get());
    }
}
