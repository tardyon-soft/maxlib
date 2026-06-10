package ru.tardyon.botframework.quarkus.polling;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import org.eclipse.microprofile.config.Config;
import ru.tardyon.botframework.client.MaxBotClient;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.ingestion.DefaultLongPollingRunner;
import ru.tardyon.botframework.ingestion.LongPollingRunner;
import ru.tardyon.botframework.ingestion.LongPollingRunnerConfig;
import ru.tardyon.botframework.ingestion.PollingFetchRequest;
import ru.tardyon.botframework.ingestion.PollingUpdateSource;
import ru.tardyon.botframework.ingestion.SdkPollingUpdateSource;
import ru.tardyon.botframework.model.UpdateEventType;

/**
 * Polling-specific CDI wiring for MAX Quarkus starter.
 */
@ApplicationScoped
public class QuarkusPollingFactory {
    @Inject
    Config config;

    @Produces
    @Singleton
    @DefaultBean
    PollingUpdateSource pollingUpdateSource(MaxBotClient maxBotClient) {
        if (!pollingEnabled()) {
            return null;
        }
        return new SdkPollingUpdateSource(maxBotClient);
    }

    @Produces
    @Singleton
    @DefaultBean
    LongPollingRunnerConfig longPollingRunnerConfig(Config config) {
        if (!pollingEnabled()) {
            return null;
        }
        Integer timeoutSeconds = config.getOptionalValue("max.bot.polling.timeout", Duration.class)
                .map(duration -> Math.toIntExact(duration.toSeconds()))
                .orElse(null);
        Integer limit = config.getOptionalValue("max.bot.polling.limit", Integer.class).orElse(100);
        List<UpdateEventType> types = config.getOptionalValues("max.bot.polling.types", UpdateEventType.class)
                .orElse(List.of());
        PollingFetchRequest request = new PollingFetchRequest(null, timeoutSeconds, limit, types);
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "max-long-polling-runner");
            thread.setDaemon(false);
            return thread;
        };
        ExecutorService executor = Executors.newSingleThreadExecutor(threadFactory);
        return LongPollingRunnerConfig.builder()
                .request(request)
                .executor(executor, true)
                .build();
    }

    @Produces
    @Singleton
    @DefaultBean
    LongPollingRunner longPollingRunner(
            PollingUpdateSource source,
            Dispatcher dispatcher,
            LongPollingRunnerConfig config
    ) {
        if (source == null || config == null || !pollingEnabled()) {
            return null;
        }
        return new DefaultLongPollingRunner(source, dispatcher, config);
    }

    private boolean pollingEnabled() {
        return config.getOptionalValue("max.bot.mode", String.class).map("POLLING"::equals).orElse(true)
                && config.getOptionalValue("max.bot.polling.enabled", Boolean.class).orElse(true);
    }

}
