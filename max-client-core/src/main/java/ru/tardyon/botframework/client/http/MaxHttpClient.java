package ru.tardyon.botframework.client.http;

/**
 * Raw HTTP transport abstraction used by MAX client-core internals.
 */
public interface MaxHttpClient {
    MaxHttpResponse execute(MaxHttpRequest request);
}
