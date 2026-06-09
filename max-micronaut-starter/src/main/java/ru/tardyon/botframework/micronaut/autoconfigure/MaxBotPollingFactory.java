package ru.tardyon.botframework.micronaut.autoconfigure;

import io.micronaut.context.BeanProvider;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import ru.tardyon.botframework.client.MaxBotClient;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.ingestion.DefaultLongPollingRunner;
import ru.tardyon.botframework.ingestion.LongPollingRunner;
import ru.tardyon.botframework.ingestion.LongPollingRunnerConfig;
import ru.tardyon.botframework.ingestion.PollingFetchRequest;
import ru.tardyon.botframework.ingestion.PollingUpdateSource;
import ru.tardyon.botframework.ingestion.SdkPollingUpdateSource;
import ru.tardyon.botframework.micronaut.polling.MicronautPollingBootstrap;
import ru.tardyon.botframework.micronaut.polling.MicronautPollingLifecycle;
import ru.tardyon.botframework.micronaut.properties.MaxBotProperties;

/**
 * Polling-specific Micronaut bean wiring.
 */
@Factory
public final class MaxBotPollingFactory {
    @Singleton
    @Requires(missingBeans = MicronautPollingBootstrap.class)
    MicronautPollingBootstrap micronautPollingBootstrapMax(BeanProvider<LongPollingRunner> runnerProvider) {
        return new MicronautPollingBootstrap(runnerProvider.isPresent() ? runnerProvider.get() : null);
    }

    @Singleton
    @Requires(property = "max.bot.mode", value = "POLLING", defaultValue = "POLLING")
    @Requires(property = "max.bot.polling.enabled", value = "true", defaultValue = "true")
    @Requires(missingBeans = PollingUpdateSource.class)
    PollingUpdateSource pollingUpdateSourceMax(MaxBotClient maxBotClient) {
        return new SdkPollingUpdateSource(maxBotClient);
    }

    @Singleton
    @Requires(property = "max.bot.mode", value = "POLLING", defaultValue = "POLLING")
    @Requires(property = "max.bot.polling.enabled", value = "true", defaultValue = "true")
    @Requires(missingBeans = LongPollingRunnerConfig.class)
    LongPollingRunnerConfig longPollingRunnerConfigMax(MaxBotProperties properties) {
        Integer timeoutSeconds = null;
        if (properties.getPolling().getTimeout() != null) {
            timeoutSeconds = Math.toIntExact(properties.getPolling().getTimeout().toSeconds());
        }
        PollingFetchRequest request = new PollingFetchRequest(
                null,
                timeoutSeconds,
                properties.getPolling().getLimit(),
                properties.getPolling().getTypes()
        );
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

    @Singleton
    @Requires(property = "max.bot.mode", value = "POLLING", defaultValue = "POLLING")
    @Requires(property = "max.bot.polling.enabled", value = "true", defaultValue = "true")
    @Requires(missingBeans = LongPollingRunner.class)
    LongPollingRunner longPollingRunnerMax(
            PollingUpdateSource source,
            Dispatcher dispatcher,
            LongPollingRunnerConfig config
    ) {
        return new DefaultLongPollingRunner(source, dispatcher, config);
    }

    @Singleton
    @Requires(beans = LongPollingRunner.class)
    @Requires(property = "max.bot.mode", value = "POLLING", defaultValue = "POLLING")
    @Requires(property = "max.bot.polling.enabled", value = "true", defaultValue = "true")
    @Requires(missingBeans = MicronautPollingLifecycle.class)
    MicronautPollingLifecycle micronautPollingLifecycleMax(LongPollingRunner longPollingRunner) {
        return new MicronautPollingLifecycle(longPollingRunner);
    }
}
