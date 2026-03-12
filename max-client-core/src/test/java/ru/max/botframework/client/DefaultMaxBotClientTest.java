package ru.max.botframework.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.max.botframework.client.error.MaxNotFoundException;
import ru.max.botframework.client.error.MaxRateLimitException;
import ru.max.botframework.client.error.MaxServiceUnavailableException;
import ru.max.botframework.client.error.MaxTransportException;
import ru.max.botframework.client.http.HttpMethod;
import ru.max.botframework.client.http.MaxHttpClient;
import ru.max.botframework.client.http.MaxHttpResponse;
import ru.max.botframework.client.http.okhttp.OkHttpMaxHttpClient;
import ru.max.botframework.client.serialization.JacksonJsonCodec;

class DefaultMaxBotClientTest {

    private MockWebServer server;
    private DefaultMaxBotClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        client = createClient(RetryPolicy.none(), createTransport(
                MaxApiClientConfig.builder()
                        .baseUri(URI.create(server.url("/").toString()))
                        .token("test-token")
                        .connectTimeout(Duration.ofSeconds(2))
                        .readTimeout(Duration.ofSeconds(2))
                        .userAgent("max-client-core-test/1.0")
                        .retryPolicy(RetryPolicy.none())
                        .build()
        ));
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void shouldExecuteGetWithoutBodyAndDeserializeJsonResponse() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"ok\":true,\"message\":\"pong\"}"));

        EchoResponse response = client.execute(new EchoRequest(HttpMethod.GET, null));

        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("GET");
        assertThat(recorded.getPath()).isEqualTo("/v1/ping?limit=10");
        assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer test-token");
        assertThat(recorded.getHeader("User-Agent")).isEqualTo("max-client-core-test/1.0");
        assertThat(recorded.getBodySize()).isZero();

        assertThat(response.ok()).isTrue();
        assertThat(response.message()).isEqualTo("pong");
    }

    @Test
    void shouldSerializeJsonBodyAndSupportMutatingHttpMethods() throws Exception {
        for (HttpMethod method : new HttpMethod[]{HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE}) {
            server.enqueue(new MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"ok\":true,\"message\":\"" + method + "\"}"));

            EchoResponse response = client.execute(new EchoRequest(method, new Payload("hello")));

            RecordedRequest recorded = server.takeRequest();
            assertThat(recorded.getMethod()).isEqualTo(method.name());
            assertThat(recorded.getPath()).isEqualTo("/v1/ping?limit=10");
            assertThat(recorded.getHeader("Content-Type")).startsWith("application/json");
            assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer test-token");
            assertThat(recorded.getHeader("User-Agent")).isEqualTo("max-client-core-test/1.0");
            assertThat(recorded.getBody().readUtf8()).contains("\"value\":\"hello\"");
            assertThat(response.message()).isEqualTo(method.name());
        }
    }

    @Test
    void shouldMap429ToRateLimitException() {
        server.enqueue(new MockResponse()
                .setResponseCode(429)
                .setHeader("Retry-After", "7")
                .setBody("{\"error_code\":\"RATE_LIMIT\",\"message\":\"Slow down\",\"details\":{\"scope\":\"global\"}}"));

        assertThatThrownBy(() -> client.execute(new EchoRequest(HttpMethod.GET, null)))
                .isInstanceOf(MaxRateLimitException.class)
                .satisfies(ex -> {
                    MaxRateLimitException exception = (MaxRateLimitException) ex;
                    assertThat(exception.statusCode()).isEqualTo(429);
                    assertThat(exception.retryAfterSeconds()).isEqualTo(7L);
                    assertThat(exception.responseBody()).contains("RATE_LIMIT");
                    assertThat(exception.errorPayload().errorCode()).isEqualTo("RATE_LIMIT");
                    assertThat(exception.errorPayload().message()).isEqualTo("Slow down");
                });
    }

    @Test
    void shouldMap404ToNotFoundException() {
        server.enqueue(new MockResponse().setResponseCode(404).setBody("{\"error\":\"not_found\"}"));

        assertThatThrownBy(() -> client.execute(new EchoRequest(HttpMethod.GET, null)))
                .isInstanceOf(MaxNotFoundException.class);
    }

    @Test
    void shouldRetrySafeGetForTransientStatusWhenPolicyAllows() throws Exception {
        client = createClient(RetryPolicy.fixed(2, Duration.ZERO), createTransport(buildConfig(RetryPolicy.fixed(2, Duration.ZERO))));

        server.enqueue(new MockResponse().setResponseCode(503).setBody("{\"error\":\"service_unavailable\"}"));
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"ok\":true,\"message\":\"recovered\"}"));

        EchoResponse response = client.execute(new EchoRequest(HttpMethod.GET, null));

        assertThat(response.message()).isEqualTo("recovered");
        RecordedRequest first = server.takeRequest();
        RecordedRequest second = server.takeRequest();
        assertThat(first.getMethod()).isEqualTo("GET");
        assertThat(second.getMethod()).isEqualTo("GET");
    }

    @Test
    void shouldNotRetryUnsafeMethodsByDefault() {
        client = createClient(RetryPolicy.fixed(3, Duration.ZERO), createTransport(buildConfig(RetryPolicy.fixed(3, Duration.ZERO))));
        server.enqueue(new MockResponse().setResponseCode(503).setBody("{\"error\":\"service_unavailable\"}"));

        assertThatThrownBy(() -> client.execute(new EchoRequest(HttpMethod.POST, new Payload("x"))))
                .isInstanceOf(MaxServiceUnavailableException.class);

        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void shouldRetryTransportFailureForSafeMethods() {
        AtomicInteger attempts = new AtomicInteger();
        MaxHttpClient failingTransport = request -> {
            if (attempts.incrementAndGet() == 1) {
                throw new MaxTransportException("temporary failure", new IOException("io"));
            }
            return new MaxHttpResponse(
                    200,
                    Map.of("Content-Type", List.of("application/json")),
                    "{\"ok\":true,\"message\":\"after-retry\"}".getBytes()
            );
        };

        client = createClient(RetryPolicy.fixed(2, Duration.ZERO), failingTransport);

        EchoResponse response = client.execute(new EchoRequest(HttpMethod.GET, null));

        assertThat(response.message()).isEqualTo("after-retry");
        assertThat(attempts.get()).isEqualTo(2);
    }

    private MaxApiClientConfig buildConfig(RetryPolicy retryPolicy) {
        return MaxApiClientConfig.builder()
                .baseUri(URI.create(server.url("/").toString()))
                .token("test-token")
                .connectTimeout(Duration.ofSeconds(2))
                .readTimeout(Duration.ofSeconds(2))
                .userAgent("max-client-core-test/1.0")
                .retryPolicy(retryPolicy)
                .build();
    }

    private MaxHttpClient createTransport(MaxApiClientConfig config) {
        return new OkHttpMaxHttpClient(
                config.baseUri(),
                new OkHttpClient.Builder()
                        .connectTimeout(config.connectTimeout())
                        .readTimeout(config.readTimeout())
                        .build()
        );
    }

    private DefaultMaxBotClient createClient(RetryPolicy retryPolicy, MaxHttpClient transport) {
        MaxApiClientConfig config = buildConfig(retryPolicy);
        return new DefaultMaxBotClient(config, transport, new JacksonJsonCodec());
    }

    private record EchoRequest(HttpMethod method, Object payload) implements MaxRequest<EchoResponse> {

        @Override
        public String path() {
            return "/v1/ping";
        }

        @Override
        public Class<EchoResponse> responseType() {
            return EchoResponse.class;
        }

        @Override
        public java.util.Optional<Object> body() {
            return java.util.Optional.ofNullable(payload);
        }

        @Override
        public Map<String, String> queryParameters() {
            return Map.of("limit", "10");
        }
    }

    private record EchoResponse(boolean ok, String message) {
    }

    private record Payload(String value) {
    }
}
