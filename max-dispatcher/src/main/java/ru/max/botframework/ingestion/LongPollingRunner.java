package ru.max.botframework.ingestion;

/**
 * Runtime loop boundary for long polling ingestion.
 */
public interface LongPollingRunner {

    void run(PollingUpdateSource source, UpdateSink sink);
}
