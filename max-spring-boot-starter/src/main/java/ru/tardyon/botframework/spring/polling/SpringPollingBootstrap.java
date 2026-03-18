package ru.tardyon.botframework.spring.polling;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tardyon.botframework.ingestion.LongPollingRunner;

/**
 * Thin Spring-facing wrapper around {@link LongPollingRunner}.
 */
public final class SpringPollingBootstrap {
    private static final Logger log = LoggerFactory.getLogger(SpringPollingBootstrap.class);
    private final LongPollingRunner longPollingRunner;

    public SpringPollingBootstrap(LongPollingRunner longPollingRunner) {
        this.longPollingRunner = longPollingRunner;
    }

    public Optional<LongPollingRunner> longPollingRunner() {
        return Optional.ofNullable(longPollingRunner);
    }

    public void start() {
        log.debug("SpringPollingBootstrap.start invoked");
        if (longPollingRunner == null) {
            log.debug("SpringPollingBootstrap.start skipped: LongPollingRunner is not configured");
            return;
        }
        longPollingRunner.start();
    }

    public void stop() {
        log.debug("SpringPollingBootstrap.stop invoked");
        if (longPollingRunner == null) {
            log.debug("SpringPollingBootstrap.stop skipped: LongPollingRunner is not configured");
            return;
        }
        longPollingRunner.stop();
    }

    public void shutdown() {
        log.debug("SpringPollingBootstrap.shutdown invoked");
        if (longPollingRunner == null) {
            log.debug("SpringPollingBootstrap.shutdown skipped: LongPollingRunner is not configured");
            return;
        }
        longPollingRunner.shutdown();
    }

    public boolean isRunning() {
        return longPollingRunner != null && longPollingRunner.isRunning();
    }
}
