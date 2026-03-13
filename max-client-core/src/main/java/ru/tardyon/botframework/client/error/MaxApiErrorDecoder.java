package ru.tardyon.botframework.client.error;

import ru.tardyon.botframework.client.http.MaxHttpRequest;
import ru.tardyon.botframework.client.http.MaxHttpResponse;

/**
 * Maps HTTP error responses to typed MAX API exceptions.
 */
public interface MaxApiErrorDecoder {

    MaxApiException decode(MaxHttpRequest request, MaxHttpResponse response);
}
