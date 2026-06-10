package ru.tardyon.botframework.quarkus.polling;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import ru.tardyon.botframework.ingestion.LongPollingRunner;

@QuarkusComponentTest({
        ru.tardyon.botframework.quarkus.runtime.MaxBotProducer.class,
        QuarkusPollingFactory.class,
        QuarkusPollingBootstrap.class,
        QuarkusPollingLifecycle.class,
        QuarkusPollingLifecycleTest.RunnerBeans.class
})
@TestConfigProperty(key = "max.bot.token", value = "test-token")
@TestConfigProperty(key = "max.bot.mode", value = "POLLING")
@TestConfigProperty(key = "max.bot.polling.enabled", value = "true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QuarkusPollingLifecycleTest {
    @Inject
    QuarkusPollingBootstrap bootstrap;

    @Inject
    QuarkusPollingLifecycle lifecycle;

    @Test
    @Order(1)
    void autoStartCallsRunnerOnStartup() {
        assertTrue(TrackingRunner.STARTS.get() > 0);
        assertTrue(bootstrap.isRunning());
    }

    @Test
    @Order(2)
    void autoStopAndShutdownCallRunnerOnContainerClose() {
        assertTrue(TrackingRunner.STOPS.get() > 0);
        assertTrue(TrackingRunner.SHUTDOWNS.get() > 0);
    }

    @Singleton
    static final class RunnerBeans {
        @Produces
        LongPollingRunner longPollingRunner() {
            return TrackingRunner.INSTANCE;
        }
    }

    static final class TrackingRunner implements LongPollingRunner {
        static final TrackingRunner INSTANCE = new TrackingRunner();
        static final AtomicInteger STARTS = new AtomicInteger();
        static final AtomicInteger STOPS = new AtomicInteger();
        static final AtomicInteger SHUTDOWNS = new AtomicInteger();
        static final AtomicBoolean RUNNING = new AtomicBoolean();

        @Override
        public void start() {
            STARTS.incrementAndGet();
            RUNNING.set(true);
        }

        @Override
        public void stop() {
            STOPS.incrementAndGet();
            RUNNING.set(false);
        }

        @Override
        public void shutdown() {
            SHUTDOWNS.incrementAndGet();
            RUNNING.set(false);
        }

        @Override
        public boolean isRunning() {
            return RUNNING.get();
        }
    }
}
