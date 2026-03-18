package ru.tardyon.botframework.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.tardyon.botframework.client.MaxBotClient;
import ru.tardyon.botframework.client.error.MaxApiException;
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
        assertSame(update, batch.updates().getFirst());
        assertEquals(11L, batch.nextMarker());
        verify(client).getUpdates(new GetUpdatesRequest(10L, 10, 100, List.of(UpdateEventType.MESSAGE_CALLBACK)));
    }

    @Test
    void pollMapsTransportUpdatesWhenApiShapeResponseIsReturned() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        SdkPollingUpdateSource source = new SdkPollingUpdateSource(client);
        PollingFetchRequest request = new PollingFetchRequest(1L, 10, 10, List.of(UpdateEventType.MESSAGE_CREATED));

        ApiUpdate apiUpdate = new ApiUpdate(
                55L,
                "message_created",
                1735689600L,
                new ApiMessage(
                        "101",
                        new ApiUser(7L, "Alice", null, "alice", false, null, "Alice"),
                        new ApiRecipient(10L, null, "chat"),
                        1735689600L,
                        null,
                        new ApiMessageBody("hello", List.of()),
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
        assertEquals("55", batch.updates().getFirst().updateId().value());
        assertEquals(UpdateType.MESSAGE, batch.updates().getFirst().type());
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
