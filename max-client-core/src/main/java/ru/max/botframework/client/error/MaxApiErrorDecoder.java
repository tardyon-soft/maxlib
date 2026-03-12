package ru.max.botframework.client.error;

import ru.max.botframework.client.http.MaxHttpRequest;
import ru.max.botframework.client.http.MaxHttpResponse;

/**
 * Maps HTTP error responses to typed MAX API exceptions.
 */
public interface MaxApiErrorDecoder {

    MaxApiException decode(MaxHttpRequest request, MaxHttpResponse response);
}
