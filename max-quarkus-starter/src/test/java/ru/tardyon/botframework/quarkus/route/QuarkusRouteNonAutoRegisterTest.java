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
import ru.tardyon.botframework.dispatcher.annotation.Command;
import ru.tardyon.botframework.dispatcher.annotation.Route;
import ru.tardyon.botframework.quarkus.runtime.MaxBotProducer;

@QuarkusComponentTest({MaxBotProducer.class, QuarkusRouteAutoRegistrationBootstrap.class})
@TestConfigProperty(key = "max.bot.token", value = "test-token")
@TestConfigProperty(key = "max.bot.polling.enabled", value = "false")
@TestConfigProperty(key = "max.bot.route-component-scan.enabled", value = "false")
class QuarkusRouteNonAutoRegisterTest {
    @Inject
    Dispatcher dispatcher;

    @Test
    void nonAutoRegisterRouteBeanIsIgnored() {
        DispatchResult result = dispatcher.feedUpdate(QuarkusRouteTestSupport.sampleUpdate("/start")).toCompletableFuture().join();
        assertEquals(DispatchStatus.IGNORED, result.status());
        assertEquals(0, ManualRouteConfig.CALLS.get());
    }

    @Singleton
    static final class ManualRouteConfig {
        static final AtomicInteger CALLS = new AtomicInteger();

        @Produces
        ManualRouteController manualRoute() {
            return new ManualRouteController();
        }

        @Route(value = "manual", autoRegister = false)
        static class ManualRouteController {
            @Command("start")
            public java.util.concurrent.CompletionStage<Void> onStart() {
                CALLS.incrementAndGet();
                return CompletableFuture.completedFuture(null);
            }
        }
    }
}
