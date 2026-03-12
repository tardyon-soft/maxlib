package ru.max.botframework.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
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
import ru.max.botframework.client.test.JsonFixtures;
import ru.max.botframework.model.BotInfo;
import ru.max.botframework.model.ChatId;
import ru.max.botframework.model.Message;
import ru.max.botframework.model.MessageId;
import ru.max.botframework.model.TextFormat;
import ru.max.botframework.model.CallbackId;
import ru.max.botframework.model.request.AnswerCallbackRequest;
import ru.max.botframework.model.request.CreateSubscriptionRequest;
import ru.max.botframework.model.request.DeleteSubscriptionRequest;
import ru.max.botframework.model.request.EditMessageRequest;
import ru.max.botframework.model.request.GetUpdatesRequest;
import ru.max.botframework.model.request.NewMessageBody;
import ru.max.botframework.model.request.SendMessageRequest;
import ru.max.botframework.model.response.GetUpdatesResponse;
import ru.max.botframework.model.UpdateEventType;
import ru.max.botframework.model.Subscription;

class DefaultMaxBotClientTest {

    private MockWebServer server;
    private DefaultMaxBotClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        MaxApiClientConfig config = buildConfig(RetryPolicy.none());
        client = createClient(config, createTransport(config));
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
    void shouldCallGetMeAndReturnTypedBotInfo() throws Exception {
        server.enqueue(JsonFixtures.jsonResponse("bot-info-response.json"));

        BotInfo botInfo = client.getMe();

        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("GET");
        assertThat(recorded.getPath()).isEqualTo("/me");
        assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer test-token");

        assertThat(botInfo.id().value()).isEqualTo("b-1");
        assertThat(botInfo.username()).isEqualTo("max_helper_bot");
        assertThat(botInfo.displayName()).isEqualTo("MAX Helper Bot");
    }

    @Test
    void shouldSendMessageViaDomainMethod() throws Exception {
        server.enqueue(JsonFixtures.jsonResponse("message-envelope-response.json"));

        Message message = client.sendMessage(new SendMessageRequest(
                new ChatId("c-100"),
                new NewMessageBody("hello", TextFormat.PLAIN, List.of()),
                false,
                null
        ));

        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getPath()).isEqualTo("/messages?chat_id=c-100");
        String body = recorded.getBody().readUtf8();
        assertThat(body).contains("\"text\":\"hello\"");
        assertThat(body).contains("\"notify\":false");
        assertThat(message.messageId().value()).isEqualTo("m-101");
    }

    @Test
    void shouldEditMessageViaDomainMethod() {
        server.enqueue(JsonFixtures.jsonResponse("operation-success-response.json"));

        boolean success = client.editMessage(new EditMessageRequest(
                new ChatId("c-100"),
                new MessageId("m-101"),
                new NewMessageBody("edited", TextFormat.MARKDOWN, List.of()),
                true
        ));

        RecordedRequest recorded = takeRecordedRequestUnchecked();
        assertThat(recorded.getMethod()).isEqualTo("PUT");
        assertThat(recorded.getPath()).isEqualTo("/messages?message_id=m-101");
        assertThat(recorded.getBody().readUtf8()).contains("\"text\":\"edited\"");
        assertThat(success).isTrue();
    }

    @Test
    void shouldDeleteMessageViaDomainMethod() {
        server.enqueue(JsonFixtures.jsonResponse("operation-success-response.json"));

        boolean success = client.deleteMessage(new MessageId("m-102"));

        RecordedRequest recorded = takeRecordedRequestUnchecked();
        assertThat(recorded.getMethod()).isEqualTo("DELETE");
        assertThat(recorded.getPath()).isEqualTo("/messages?message_id=m-102");
        assertThat(success).isTrue();
    }

    @Test
    void shouldGetMessageViaDomainMethod() {
        server.enqueue(JsonFixtures.jsonResponse("message-single-response.json"));

        Message message = client.getMessage(new MessageId("m-200"));

        RecordedRequest recorded = takeRecordedRequestUnchecked();
        assertThat(recorded.getMethod()).isEqualTo("GET");
        assertThat(recorded.getPath()).isEqualTo("/messages/m-200");
        assertThat(message.text()).isEqualTo("single");
    }

    @Test
    void shouldGetMessagesByChatIdViaDomainMethod() {
        server.enqueue(JsonFixtures.jsonResponse("messages-list-response.json"));

        List<Message> messages = client.getMessages(new ChatId("c-100"));

        RecordedRequest recorded = takeRecordedRequestUnchecked();
        assertThat(recorded.getMethod()).isEqualTo("GET");
        assertThat(recorded.getPath()).isEqualTo("/messages?chat_id=c-100");
        assertThat(messages).hasSize(2);
        assertThat(messages.getFirst().messageId().value()).isEqualTo("m-1");
    }

    @Test
    void shouldGetMessagesByIdsViaDomainMethod() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"messages":[]}
                        """));

        List<Message> messages = client.getMessages(List.of(new MessageId("m-1"), new MessageId("m-2")));

        RecordedRequest recorded = takeRecordedRequestUnchecked();
        assertThat(recorded.getMethod()).isEqualTo("GET");
        assertThat(recorded.getPath()).isEqualTo("/messages?message_ids=m-1%2Cm-2");
        assertThat(messages).isEmpty();
    }

    @Test
    void shouldAnswerCallbackViaDomainMethod() {
        server.enqueue(JsonFixtures.jsonResponse("operation-success-response.json"));

        boolean success = client.answerCallback(new AnswerCallbackRequest(
                new CallbackId("cb-1"),
                "OK",
                false,
                5
        ));

        RecordedRequest recorded = takeRecordedRequestUnchecked();
        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getPath()).isEqualTo("/answers");
        String body = recorded.getBody().readUtf8();
        assertThat(body).contains("\"callbackId\":\"cb-1\"");
        assertThat(body).contains("\"text\":\"OK\"");
        assertThat(success).isTrue();
    }

    @Test
    void shouldGetUpdatesViaDomainMethodWithPollingParams() {
        server.enqueue(JsonFixtures.jsonResponse("updates-response.json"));

        GetUpdatesResponse response = client.getUpdates(new GetUpdatesRequest(
                100L,
                30,
                50,
                List.of(UpdateEventType.MESSAGE_CREATED, UpdateEventType.MESSAGE_CALLBACK)
        ));

        RecordedRequest recorded = takeRecordedRequestUnchecked();
        assertThat(recorded.getMethod()).isEqualTo("GET");
        assertThat(recorded.getPath()).contains("/updates?");
        assertThat(recorded.getPath()).contains("marker=100");
        assertThat(recorded.getPath()).contains("timeout=30");
        assertThat(recorded.getPath()).contains("limit=50");
        assertThat(recorded.getPath()).contains("types=message_created%2Cmessage_callback");
        assertThat(response.marker()).isEqualTo(101L);
        assertThat(response.updates()).hasSize(1);
        assertThat(response.updates().getFirst().updateId().value()).isEqualTo("upd-1");
    }

    @Test
    void shouldGetWebhookSubscriptions() {
        server.enqueue(JsonFixtures.jsonResponse("subscriptions-response.json"));

        List<Subscription> subscriptions = client.getSubscriptions();

        RecordedRequest recorded = takeRecordedRequestUnchecked();
        assertThat(recorded.getMethod()).isEqualTo("GET");
        assertThat(recorded.getPath()).isEqualTo("/subscriptions");
        assertThat(subscriptions).hasSize(1);
        assertThat(subscriptions.getFirst().url()).isEqualTo("https://example.com/webhook");
        assertThat(subscriptions.getFirst().updateTypes())
                .containsExactly(UpdateEventType.MESSAGE_CREATED, UpdateEventType.MESSAGE_CALLBACK);
    }

    @Test
    void shouldCreateWebhookSubscription() {
        server.enqueue(JsonFixtures.jsonResponse("operation-success-response.json"));

        boolean success = client.createSubscription(new CreateSubscriptionRequest(
                "https://example.com/webhook",
                List.of(UpdateEventType.MESSAGE_CREATED),
                "secret-1"
        ));

        RecordedRequest recorded = takeRecordedRequestUnchecked();
        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getPath()).isEqualTo("/subscriptions");
        String body = recorded.getBody().readUtf8();
        assertThat(body).contains("\"url\":\"https://example.com/webhook\"");
        assertThat(body).contains("\"update_types\":[\"message_created\"]");
        assertThat(body).contains("\"secret\":\"secret-1\"");
        assertThat(success).isTrue();
    }

    @Test
    void shouldDeleteWebhookSubscription() {
        server.enqueue(JsonFixtures.jsonResponse("operation-success-response.json"));

        boolean success = client.deleteSubscription(new DeleteSubscriptionRequest("https://example.com/webhook"));

        RecordedRequest recorded = takeRecordedRequestUnchecked();
        assertThat(recorded.getMethod()).isEqualTo("DELETE");
        assertThat(recorded.getPath()).isEqualTo("/subscriptions?url=https%3A%2F%2Fexample.com%2Fwebhook");
        assertThat(success).isTrue();
    }

    @Test
    void shouldRetrySafeGetForTransientStatusWhenPolicyAllows() throws Exception {
        MaxApiClientConfig config = buildConfig(RetryPolicy.fixed(2, Duration.ZERO));
        client = createClient(config, createTransport(config));

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
        MaxApiClientConfig config = buildConfig(RetryPolicy.fixed(3, Duration.ZERO));
        client = createClient(config, createTransport(config));
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

        MaxApiClientConfig config = buildConfig(RetryPolicy.fixed(2, Duration.ZERO));
        client = createClient(config, failingTransport);

        EchoResponse response = client.execute(new EchoRequest(HttpMethod.GET, null));

        assertThat(response.message()).isEqualTo("after-retry");
        assertThat(attempts.get()).isEqualTo(2);
    }

    @Test
    void shouldNotifyRateLimiterWhen429IsReceived() {
        CapturingRateLimiter limiter = new CapturingRateLimiter();
        MaxApiClientConfig config = buildConfig(RetryPolicy.none(), limiter);
        client = createClient(
                config,
                createTransport(config)
        );
        server.enqueue(new MockResponse().setResponseCode(429).setHeader("Retry-After", "3").setBody("{\"error\":\"rate_limit\"}"));

        assertThatThrownBy(() -> client.execute(new EchoRequest(HttpMethod.GET, null)))
                .isInstanceOf(MaxRateLimitException.class);

        assertThat(limiter.beforeRequestCalls.get()).isEqualTo(1);
        assertThat(limiter.rateLimitedCalls.get()).isEqualTo(1);
        assertThat(limiter.lastRetryAfterSeconds.get()).isEqualTo(3L);
    }

    @Test
    void shouldCallRateLimiterBeforeEachRetryAttempt() {
        CapturingRateLimiter limiter = new CapturingRateLimiter();
        MaxApiClientConfig config = buildConfig(RetryPolicy.fixed(2, Duration.ZERO), limiter);
        client = createClient(
                config,
                createTransport(config)
        );

        server.enqueue(new MockResponse().setResponseCode(503).setBody("{\"error\":\"temporary\"}"));
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"ok\":true,\"message\":\"done\"}"));

        EchoResponse response = client.execute(new EchoRequest(HttpMethod.GET, null));

        assertThat(response.message()).isEqualTo("done");
        assertThat(limiter.beforeRequestCalls.get()).isEqualTo(2);
    }

    @Test
    void shouldRespectRetryAfterHeaderWhenRetrying429() {
        MaxApiClientConfig config = buildConfig(RetryPolicy.fixed(2, Duration.ZERO));
        client = createClient(config, createTransport(config));
        server.enqueue(new MockResponse().setResponseCode(429).setHeader("Retry-After", "1").setBody("{\"error\":\"rate_limit\"}"));
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"ok\":true,\"message\":\"after-rate-limit\"}"));

        long startedAt = System.currentTimeMillis();
        EchoResponse response = client.execute(new EchoRequest(HttpMethod.GET, null));
        long elapsedMillis = System.currentTimeMillis() - startedAt;

        assertThat(response.message()).isEqualTo("after-rate-limit");
        assertThat(elapsedMillis).isGreaterThanOrEqualTo(800L);
    }

    private MaxApiClientConfig buildConfig(RetryPolicy retryPolicy) {
        return buildConfig(retryPolicy, RequestRateLimiter.noop());
    }

    private MaxApiClientConfig buildConfig(RetryPolicy retryPolicy, RequestRateLimiter rateLimiter) {
        return MaxApiClientConfig.builder()
                .baseUri(URI.create(server.url("/").toString()))
                .token("test-token")
                .connectTimeout(Duration.ofSeconds(2))
                .readTimeout(Duration.ofSeconds(2))
                .userAgent("max-client-core-test/1.0")
                .retryPolicy(retryPolicy)
                .rateLimiter(rateLimiter)
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

    private RecordedRequest takeRecordedRequestUnchecked() {
        try {
            return server.takeRequest();
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(interruptedException);
        }
    }

    private DefaultMaxBotClient createClient(MaxApiClientConfig config, MaxHttpClient transport) {
        return new DefaultMaxBotClient(config, transport, new JacksonJsonCodec());
    }

    private static final class CapturingRateLimiter implements RequestRateLimiter {
        private final AtomicInteger beforeRequestCalls = new AtomicInteger();
        private final AtomicInteger rateLimitedCalls = new AtomicInteger();
        private final AtomicLong lastRetryAfterSeconds = new AtomicLong(-1);

        @Override
        public void beforeRequest(ru.max.botframework.client.http.MaxHttpRequest request) {
            beforeRequestCalls.incrementAndGet();
        }

        @Override
        public void onRateLimited(
                ru.max.botframework.client.http.MaxHttpRequest request,
                MaxHttpResponse response,
                Long retryAfterSeconds
        ) {
            rateLimitedCalls.incrementAndGet();
            lastRetryAfterSeconds.set(retryAfterSeconds == null ? -1 : retryAfterSeconds);
        }
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
