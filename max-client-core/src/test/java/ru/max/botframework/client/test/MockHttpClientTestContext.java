package ru.max.botframework.client.test;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import ru.max.botframework.client.DefaultMaxBotClient;
import ru.max.botframework.client.MaxApiClientConfig;
import ru.max.botframework.client.RequestRateLimiter;
import ru.max.botframework.client.RetryPolicy;
import ru.max.botframework.client.http.MaxHttpClient;
import ru.max.botframework.client.http.okhttp.OkHttpMaxHttpClient;
import ru.max.botframework.client.serialization.JacksonJsonCodec;

/**
 * Reusable mocked HTTP test context for client SDK tests.
 */
public final class MockHttpClientTestContext implements AutoCloseable {
    private final MockWebServer server;

    private MockHttpClientTestContext(MockWebServer server) {
        this.server = server;
    }

    public static MockHttpClientTestContext start() {
        MockWebServer server = new MockWebServer();
        try {
            server.start();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start MockWebServer", e);
        }
        return new MockHttpClientTestContext(server);
    }

    public MaxApiClientConfig buildConfig(RetryPolicy retryPolicy) {
        return buildConfig(retryPolicy, RequestRateLimiter.noop());
    }

    public MaxApiClientConfig buildConfig(RetryPolicy retryPolicy, RequestRateLimiter rateLimiter) {
        return MaxApiClientConfig.builder()
                .baseUri(URI.create(server.url("/").toString()))
                .token("test-token")
                .connectTimeout(Duration.ofSeconds(2))
                .readTimeout(Duration.ofSeconds(2))
                .userAgent("max-client-core-test/1.0")
                .retryPolicy(Objects.requireNonNull(retryPolicy, "retryPolicy"))
                .rateLimiter(Objects.requireNonNull(rateLimiter, "rateLimiter"))
                .build();
    }

    public MaxHttpClient createTransport(MaxApiClientConfig config) {
        return new OkHttpMaxHttpClient(
                config.baseUri(),
                new OkHttpClient.Builder()
                        .connectTimeout(config.connectTimeout())
                        .readTimeout(config.readTimeout())
                        .build()
        );
    }

    public DefaultMaxBotClient createClient(MaxApiClientConfig config, MaxHttpClient transport) {
        return new DefaultMaxBotClient(config, transport, new JacksonJsonCodec());
    }

    public DefaultMaxBotClient createClient(RetryPolicy retryPolicy) {
        MaxApiClientConfig config = buildConfig(retryPolicy);
        return createClient(config, createTransport(config));
    }

    public void enqueue(MockResponse response) {
        server.enqueue(Objects.requireNonNull(response, "response"));
    }

    public void enqueueJsonFixture(String fixtureName) {
        server.enqueue(JsonFixtures.jsonResponse(fixtureName));
    }

    public void enqueueJsonFixture(int statusCode, String fixtureName) {
        server.enqueue(JsonFixtures.jsonResponse(statusCode, fixtureName));
    }

    public RecordedRequest takeRequest() {
        try {
            return server.takeRequest();
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for recorded request", interruptedException);
        }
    }

    public int requestCount() {
        return server.getRequestCount();
    }

    @Override
    public void close() {
        try {
            server.shutdown();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to shutdown MockWebServer", e);
        }
    }
}
