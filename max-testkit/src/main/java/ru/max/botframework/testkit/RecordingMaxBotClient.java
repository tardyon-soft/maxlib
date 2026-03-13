package ru.max.botframework.testkit;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import ru.max.botframework.client.MaxBotClient;
import ru.max.botframework.client.MaxRequest;
import ru.max.botframework.client.method.GetMeRequest;
import ru.max.botframework.model.BotInfo;
import ru.max.botframework.model.Chat;
import ru.max.botframework.model.ChatId;
import ru.max.botframework.model.ChatType;
import ru.max.botframework.model.Message;
import ru.max.botframework.model.MessageId;
import ru.max.botframework.model.User;
import ru.max.botframework.model.UserId;
import ru.max.botframework.model.response.GetUpdatesResponse;
import ru.max.botframework.model.response.MessageResponse;
import ru.max.botframework.model.response.OperationStatusResponse;
import ru.max.botframework.model.response.SubscriptionsResponse;

/**
 * In-memory {@link MaxBotClient} double that records executed requests and returns deterministic defaults.
 */
public final class RecordingMaxBotClient implements MaxBotClient {
    private final List<CapturedApiCall> calls = new CopyOnWriteArrayList<>();
    private final Map<Class<?>, Object> explicitResponses = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    @Override
    public <T> T execute(MaxRequest<T> request) {
        Objects.requireNonNull(request, "request");
        calls.add(new CapturedApiCall(
                request.method(),
                request.path(),
                request.queryParameters(),
                request.body(),
                request
        ));

        Object explicit = explicitResponses.get(request.responseType());
        if (explicit != null) {
            return request.responseType().cast(explicit);
        }
        return request.responseType().cast(defaultResponse(request));
    }

    public List<CapturedApiCall> calls() {
        return List.copyOf(calls);
    }

    public void clearCalls() {
        calls.clear();
    }

    public <T> RecordingMaxBotClient respondWith(Class<T> responseType, T response) {
        explicitResponses.put(Objects.requireNonNull(responseType, "responseType"), Objects.requireNonNull(response, "response"));
        return this;
    }

    public <T extends MaxRequest<?>> List<CapturedApiCall> callsOfType(Class<T> requestType) {
        Objects.requireNonNull(requestType, "requestType");
        List<CapturedApiCall> matched = new ArrayList<>();
        for (CapturedApiCall call : calls) {
            if (requestType.isInstance(call.request())) {
                matched.add(call);
            }
        }
        return List.copyOf(matched);
    }

    private Object defaultResponse(MaxRequest<?> request) {
        Class<?> responseType = request.responseType();
        if (responseType == OperationStatusResponse.class) {
            return new OperationStatusResponse(true);
        }
        if (responseType == MessageResponse.class) {
            return new MessageResponse(syntheticMessage(request));
        }
        if (responseType == Message.class) {
            return syntheticMessage(request);
        }
        if (responseType == GetUpdatesResponse.class) {
            return new GetUpdatesResponse(List.of(), null);
        }
        if (responseType == BotInfo.class || request instanceof GetMeRequest) {
            return new BotInfo(new UserId("bot-test"), "testbot", "Test Bot", null, null);
        }
        if (responseType == SubscriptionsResponse.class) {
            return new SubscriptionsResponse(List.of());
        }
        throw new IllegalStateException(
                "No default response for %s. Configure explicit response via respondWith(...).".formatted(responseType.getName())
        );
    }

    private Message syntheticMessage(MaxRequest<?> request) {
        long index = sequence.incrementAndGet();
        String chatIdValue = request.queryParameters().getOrDefault("chat_id", "test-chat");
        return new Message(
                new MessageId("m-test-" + index),
                new Chat(new ChatId(chatIdValue), ChatType.PRIVATE, "Test Chat", null, null),
                new User(new UserId("bot-test"), "testbot", "Test", "Bot", "Test Bot", true, "en"),
                "test-message-" + index,
                Instant.parse("2026-03-13T00:00:00Z"),
                null,
                List.of(),
                List.of()
        );
    }
}
