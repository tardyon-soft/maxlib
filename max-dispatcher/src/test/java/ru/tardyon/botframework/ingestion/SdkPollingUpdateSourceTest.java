package ru.tardyon.botframework.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.tardyon.botframework.client.MaxBotClient;
import ru.tardyon.botframework.client.error.MaxApiException;
import ru.tardyon.botframework.client.error.MaxTransportException;
import ru.tardyon.botframework.model.Update;
import ru.tardyon.botframework.model.UpdateEventType;
import ru.tardyon.botframework.model.UpdateId;
import ru.tardyon.botframework.model.UpdateType;
import ru.tardyon.botframework.model.request.GetUpdatesRequest;
import ru.tardyon.botframework.model.response.GetUpdatesResponse;
import ru.tardyon.botframework.model.transport.ApiGetUpdatesResponse;
import ru.tardyon.botframework.model.transport.ApiMessage;
import ru.tardyon.botframework.model.transport.ApiMessageBody;
import ru.tardyon.botframework.model.transport.ApiRecipient;
import ru.tardyon.botframework.model.transport.ApiUpdate;
import ru.tardyon.botframework.model.transport.ApiUser;

class SdkPollingUpdateSourceTest {

    @Test
    void pollReturnsEmptyBatchWhenApiReturnsNoUpdates() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        SdkPollingUpdateSource source = new SdkPollingUpdateSource(client);
        PollingFetchRequest request = new PollingFetchRequest(100L, 30, 50, List.of(UpdateEventType.MESSAGE_CREATED));
        when(client.getUpdates(new GetUpdatesRequest(100L, 30, 50, List.of(UpdateEventType.MESSAGE_CREATED))))
                .thenReturn(new GetUpdatesResponse(List.of(), 101L));

        PollingBatch batch = source.poll(request);

        assertEquals(0, batch.updates().size());
        assertEquals(101L, batch.nextMarker());
        verify(client).getUpdates(new GetUpdatesRequest(100L, 30, 50, List.of(UpdateEventType.MESSAGE_CREATED)));
    }

    @Test
    void pollReturnsUpdatesBatch() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        SdkPollingUpdateSource source = new SdkPollingUpdateSource(client);
        PollingFetchRequest request = new PollingFetchRequest(10L, 10, 100, List.of(UpdateEventType.MESSAGE_CALLBACK));
        Update update = sampleUpdate("upd-1");
        when(client.getUpdates(new GetUpdatesRequest(10L, 10, 100, List.of(UpdateEventType.MESSAGE_CALLBACK))))
                .thenReturn(new GetUpdatesResponse(List.of(update), 11L));

        PollingBatch batch = source.poll(request);

        assertEquals(1, batch.updates().size());
        assertSame(update, batch.updates().get(0));
        assertEquals(11L, batch.nextMarker());
        verify(client).getUpdates(new GetUpdatesRequest(10L, 10, 100, List.of(UpdateEventType.MESSAGE_CALLBACK)));
    }

    @Test
    void pollMapsTransportUpdatesWhenApiShapeResponseIsReturned() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        SdkPollingUpdateSource source = new SdkPollingUpdateSource(client);
        PollingFetchRequest request = new PollingFetchRequest(1L, 10, 10, List.of(UpdateEventType.MESSAGE_CREATED));

        ApiUpdate apiUpdate = new ApiUpdate(
                "message_created",
                1735689600L,
                new ApiMessage(
                        "101",
                        new ApiUser(7L, "Alice", null, "alice", false, null, "Alice"),
                        new ApiRecipient(10L, null, "chat"),
                        1735689600L,
                        null,
                        new ApiMessageBody("101", null, "hello", List.of(), List.of()),
                        null,
                        null
                ),
                null,
                "ru-RU"
        );
        when(client.getUpdatesApi(new GetUpdatesRequest(1L, 10, 10, List.of(UpdateEventType.MESSAGE_CREATED))))
                .thenReturn(new ApiGetUpdatesResponse(List.of(apiUpdate), 2L));

        PollingBatch batch = source.poll(request);

        assertEquals(1, batch.updates().size());
        assertEquals("upd-msg-101", batch.updates().get(0).updateId().value());
        assertEquals(UpdateType.MESSAGE, batch.updates().get(0).type());
        assertEquals(2L, batch.nextMarker());
    }

    @Test
    void pollPropagatesSdkErrors() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        SdkPollingUpdateSource source = new SdkPollingUpdateSource(client);
        PollingFetchRequest request = PollingFetchRequest.defaults();
        MaxApiException expected = new MaxApiException(503, "temporary unavailable");
        when(client.getUpdates(new GetUpdatesRequest(null, null, null, List.of())))
                .thenThrow(expected);

        MaxApiException actual = assertThrows(MaxApiException.class, () -> source.poll(request));

        assertSame(expected, actual);
    }

    @Test
    void pollTreatsApiTransportTimeoutAsEmptyBatch() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        SdkPollingUpdateSource source = new SdkPollingUpdateSource(client);
        PollingFetchRequest request = new PollingFetchRequest(50L, 30, 100, List.of());
        when(client.getUpdatesApi(new GetUpdatesRequest(50L, 30, 100, List.of())))
                .thenThrow(new MaxTransportException("transport timeout", new SocketTimeoutException("Read timed out")));

        PollingBatch batch = source.poll(request);

        assertEquals(0, batch.updates().size());
        assertEquals(50L, batch.nextMarker());
        verify(client).getUpdatesApi(new GetUpdatesRequest(50L, 30, 100, List.of()));
        verifyNoMoreInteractions(client);
    }

    @Test
    void pollTreatsLegacyTransportTimeoutAsEmptyBatch() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        SdkPollingUpdateSource source = new SdkPollingUpdateSource(client);
        PollingFetchRequest request = new PollingFetchRequest(60L, 30, 100, List.of());
        when(client.getUpdatesApi(new GetUpdatesRequest(60L, 30, 100, List.of())))
                .thenReturn(new ApiGetUpdatesResponse(List.of(), 60L));
        when(client.getUpdates(new GetUpdatesRequest(60L, 30, 100, List.of())))
                .thenThrow(new MaxTransportException("Read timeout", new RuntimeException("timeout waiting for response")));

        PollingBatch batch = source.poll(request);

        assertEquals(0, batch.updates().size());
        assertEquals(60L, batch.nextMarker());
        verify(client).getUpdatesApi(new GetUpdatesRequest(60L, 30, 100, List.of()));
        verify(client).getUpdates(new GetUpdatesRequest(60L, 30, 100, List.of()));
    }

    private static Update sampleUpdate(String updateId) {
        return new Update(
                new UpdateId(updateId),
                UpdateType.MESSAGE,
                null,
                null,
                null,
                Instant.parse("2026-01-01T00:00:00Z")
        );
    }
}
