package ru.tardyon.botframework.quarkus.route;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.dispatcher.DispatchResult;
import ru.tardyon.botframework.dispatcher.DispatchStatus;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.quarkus.runtime.MaxBotProducer;

@QuarkusComponentTest({MaxBotProducer.class, QuarkusRouteAutoRegistrationBootstrap.class})
@TestConfigProperty(key = "max.bot.token", value = "test-token")
@TestConfigProperty(key = "max.bot.polling.enabled", value = "false")
@TestConfigProperty(key = "max.bot.route-component-scan.enabled", value = "false")
class QuarkusRouteManualRoutersTest {
    @Inject
    Dispatcher dispatcher;

    @Test
    void manualRouterIsRegisteredAndInvoked() {
        assertEquals(1, dispatcher.routers().size());

        DispatchResult result = dispatcher.feedUpdate(QuarkusRouteTestSupport.sampleUpdate("hello")).toCompletableFuture().join();
        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals(1, RouterConfig.CALLS.get());
    }

    @Singleton
    static final class RouterConfig {
        static final AtomicInteger CALLS = new AtomicInteger();

        @Produces
        Router manualRouter() {
            Router router = new Router("manual");
            router.message((message, context) -> {
                CALLS.incrementAndGet();
                return CompletableFuture.completedFuture(null);
            });
            return router;
        }
    }
}
