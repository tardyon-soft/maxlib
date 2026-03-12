package ru.max.botframework.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.max.botframework.client.error.MaxTransportException;
import ru.max.botframework.client.http.HttpMethod;
import ru.max.botframework.client.http.MaxHttpRequest;
import ru.max.botframework.client.http.MaxHttpResponse;

class RetryPolicyTest {

    @Test
    void shouldBeConservativeByDefault() {
        RetryPolicy retryPolicy = RetryPolicy.none();
        MaxHttpRequest getRequest = new MaxHttpRequest(HttpMethod.GET, "/v1/test", Map.of(), new byte[0]);
        MaxHttpResponse serviceUnavailable = new MaxHttpResponse(503, Map.of(), new byte[0]);

        assertThat(retryPolicy.maxAttempts()).isEqualTo(1);
        assertThat(retryPolicy.delay()).isEqualTo(Duration.ZERO);
        assertThat(retryPolicy.shouldRetry(getRequest, serviceUnavailable, 1)).isFalse();
        assertThat(retryPolicy.shouldRetry(getRequest, new MaxTransportException("io", new RuntimeException("boom")), 1))
                .isFalse();
    }

    @Test
    void shouldRetryOnlySafeMethodsForTransientFailures() {
        RetryPolicy retryPolicy = RetryPolicy.fixed(3, Duration.ZERO);
        MaxHttpRequest getRequest = new MaxHttpRequest(HttpMethod.GET, "/v1/test", Map.of(), new byte[0]);
        MaxHttpRequest postRequest = new MaxHttpRequest(HttpMethod.POST, "/v1/test", Map.of(), new byte[0]);

        assertThat(retryPolicy.shouldRetry(getRequest, new MaxHttpResponse(429, Map.of(), new byte[0]), 1)).isTrue();
        assertThat(retryPolicy.shouldRetry(getRequest, new MaxHttpResponse(503, Map.of(), new byte[0]), 2)).isTrue();
        assertThat(retryPolicy.shouldRetry(getRequest, new MaxHttpResponse(500, Map.of(), new byte[0]), 1)).isFalse();
        assertThat(retryPolicy.shouldRetry(postRequest, new MaxHttpResponse(503, Map.of(), new byte[0]), 1)).isFalse();
    }
}
