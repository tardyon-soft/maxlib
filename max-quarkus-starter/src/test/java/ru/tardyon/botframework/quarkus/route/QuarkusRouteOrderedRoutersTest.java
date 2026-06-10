package ru.tardyon.botframework.quarkus.route;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
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
class QuarkusRouteOrderedRoutersTest {
    @Inject
    Dispatcher dispatcher;

    @Test
    void orderedRoutersRespectPriorityAndFirstMatch() {
        assertEquals(List.of("first", "second"), dispatcher.routers().stream().map(Router::name).toList());

        DispatchResult result = dispatcher.feedUpdate(QuarkusRouteTestSupport.sampleUpdate("hello")).toCompletableFuture().join();
        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals(1, OrderedRouterConfig.FIRST_CALLS.get());
        assertEquals(0, OrderedRouterConfig.SECOND_CALLS.get());
    }

    @Singleton
    static final class OrderedRouterConfig {
        static final AtomicInteger FIRST_CALLS = new AtomicInteger();
        static final AtomicInteger SECOND_CALLS = new AtomicInteger();

        @Produces
        @jakarta.annotation.Priority(1)
        Router firstRouter() {
            Router first = new Router("first");
            first.message((message, context) -> {
                FIRST_CALLS.incrementAndGet();
                return CompletableFuture.completedFuture(null);
            });
            return first;
        }

        @Produces
        @jakarta.annotation.Priority(2)
        Router secondRouter() {
            Router second = new Router("second");
            second.message((message, context) -> {
                SECOND_CALLS.incrementAndGet();
                return CompletableFuture.completedFuture(null);
            });
            return second;
        }
    }
}
