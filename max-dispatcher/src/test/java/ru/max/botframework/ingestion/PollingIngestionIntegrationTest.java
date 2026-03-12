package ru.max.botframework.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.max.botframework.client.MaxBotClient;
import ru.max.botframework.model.Update;
import ru.max.botframework.model.request.GetUpdatesRequest;
import ru.max.botframework.model.response.GetUpdatesResponse;

class PollingIngestionIntegrationTest {

    @Test
    void pollingEmptyFixtureDoesNotInvokeSink() throws Exception {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        GetUpdatesResponse empty = IngestionFixtures.read("polling-empty-response.json", GetUpdatesResponse.class);
        CountDownLatch firstPoll = new CountDownLatch(1);

        when(client.getUpdates(any(GetUpdatesRequest.class))).thenAnswer(invocation -> {
            firstPoll.countDown();
            return empty;
        });

        SdkPollingUpdateSource source = new SdkPollingUpdateSource(client);
        List<Update> received = new CopyOnWriteArrayList<>();
        UpdateSink sink = update -> {
            received.add(update);
            return java.util.concurrent.CompletableFuture.completedFuture(UpdateHandlingResult.success());
        };
        DefaultLongPollingRunner runner = new DefaultLongPollingRunner(
                source,
                sink,
                LongPollingRunnerConfig.builder()
                        .idleDelay(Duration.ofMillis(10))
                        .shutdownTimeout(Duration.ofMillis(200))
                        .build()
        );

        runner.start();
        assertTrue(firstPoll.await(1, TimeUnit.SECONDS));
        runner.stop();
        runner.shutdown();

        assertEquals(0, received.size());
    }

    @Test
    void pollingFixtureWithUpdatesFlowsThroughSourceRunnerAndSink() throws Exception {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        GetUpdatesResponse multi = IngestionFixtures.read("polling-multi-updates-response.json", GetUpdatesResponse.class);
        GetUpdatesResponse empty = IngestionFixtures.read("polling-empty-response.json", GetUpdatesResponse.class);
        CountDownLatch twoUpdates = new CountDownLatch(2);

        when(client.getUpdates(any(GetUpdatesRequest.class))).thenAnswer(new org.mockito.stubbing.Answer<GetUpdatesResponse>() {
            private int call;

            @Override
            public GetUpdatesResponse answer(org.mockito.invocation.InvocationOnMock invocation) {
                call++;
                return call == 1 ? multi : empty;
            }
        });

        SdkPollingUpdateSource source = new SdkPollingUpdateSource(client);
        List<Update> received = new CopyOnWriteArrayList<>();
        UpdateSink sink = update -> {
            received.add(update);
            twoUpdates.countDown();
            return java.util.concurrent.CompletableFuture.completedFuture(UpdateHandlingResult.success());
        };
        DefaultLongPollingRunner runner = new DefaultLongPollingRunner(
                source,
                sink,
                LongPollingRunnerConfig.builder()
                        .idleDelay(Duration.ofMillis(10))
                        .shutdownTimeout(Duration.ofMillis(200))
                        .build()
        );

        runner.start();
        assertTrue(twoUpdates.await(1, TimeUnit.SECONDS));
        runner.stop();
        runner.shutdown();

        assertEquals(2, received.size());
        assertEquals("upd-10", received.get(0).updateId().value());
        assertEquals("upd-11", received.get(1).updateId().value());
        verify(client, atLeastOnce()).getUpdates(any(GetUpdatesRequest.class));
    }
}
