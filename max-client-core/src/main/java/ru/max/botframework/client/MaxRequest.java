package ru.max.botframework.client;

import java.util.Map;
import java.util.Optional;
import ru.max.botframework.client.http.HttpMethod;

/**
 * Typed request contract used by {@link MaxBotClient}.
 */
public interface MaxRequest<T> {

    HttpMethod method();

    String path();

    Class<T> responseType();

    default Optional<Object> body() {
        return Optional.empty();
    }

    default Map<String, String> headers() {
        return Map.of();
    }

    default Map<String, String> queryParameters() {
        return Map.of();
    }
}
