package ru.max.botframework.client.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.max.botframework.client.http.HttpMethod;
import ru.max.botframework.client.http.MaxHttpRequest;
import ru.max.botframework.client.http.MaxHttpResponse;

class DefaultMaxApiErrorDecoderTest {

    private final DefaultMaxApiErrorDecoder decoder = new DefaultMaxApiErrorDecoder();

    @Test
    void shouldMap400ToBadRequestException() {
        MaxApiException exception = decode(
                400,
                "{\"error_code\":\"VALIDATION_FAILED\",\"message\":\"invalid payload\",\"details\":{\"field\":\"text\"}}",
                Map.of()
        );

        assertThat(exception).isInstanceOf(MaxBadRequestException.class);
        assertThat(exception.statusCode()).isEqualTo(400);
        assertThat(exception.errorPayload().status()).isEqualTo(400);
        assertThat(exception.errorPayload().errorCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(exception.errorPayload().message()).isEqualTo("invalid payload");
        assertThat(exception.errorPayload().details()).isInstanceOf(Map.class);
        assertThat(exception.errorPayload().rawBody()).contains("VALIDATION_FAILED");
    }

    @Test
    void shouldMap401ToUnauthorizedException() {
        MaxApiException exception = decode(401, "unauthorized", Map.of());

        assertThat(exception).isInstanceOf(MaxUnauthorizedException.class);
        assertThat(exception.statusCode()).isEqualTo(401);
    }

    @Test
    void shouldMap404ToNotFoundException() {
        MaxApiException exception = decode(404, "not found", Map.of());

        assertThat(exception).isInstanceOf(MaxNotFoundException.class);
        assertThat(exception.statusCode()).isEqualTo(404);
    }

    @Test
    void shouldMap429ToRateLimitExceptionAndParseRetryAfterHeader() {
        MaxApiException exception = decode(
                429,
                "{\"error\":\"RATE_LIMIT\",\"message\":\"too many requests\"}",
                Map.of("Retry-After", List.of("12"))
        );

        assertThat(exception).isInstanceOf(MaxRateLimitException.class);
        MaxRateLimitException rateLimitException = (MaxRateLimitException) exception;
        assertThat(rateLimitException.retryAfterSeconds()).isEqualTo(12L);
        assertThat(rateLimitException.statusCode()).isEqualTo(429);
        assertThat(rateLimitException.errorPayload().errorCode()).isEqualTo("RATE_LIMIT");
        assertThat(rateLimitException.errorPayload().message()).isEqualTo("too many requests");
    }

    @Test
    void shouldKeepRawBodyWhenPayloadIsNotJson() {
        MaxApiException exception = decode(500, "gateway failed", Map.of());

        assertThat(exception).isInstanceOf(MaxServerErrorException.class);
        assertThat(exception.errorPayload().status()).isEqualTo(500);
        assertThat(exception.errorPayload().errorCode()).isNull();
        assertThat(exception.errorPayload().message()).isNull();
        assertThat(exception.errorPayload().details()).isNull();
        assertThat(exception.errorPayload().rawBody()).isEqualTo("gateway failed");
    }

    @Test
    void shouldMap503ToServiceUnavailableException() {
        MaxApiException exception = decode(503, "service unavailable", Map.of());

        assertThat(exception).isInstanceOf(MaxServiceUnavailableException.class);
        assertThat(exception.statusCode()).isEqualTo(503);
    }

    @Test
    void shouldMapOther4xxToGenericClientErrorException() {
        MaxApiException exception = decode(418, "teapot", Map.of());

        assertThat(exception).isInstanceOf(MaxClientErrorException.class);
        assertThat(exception.statusCode()).isEqualTo(418);
    }

    @Test
    void shouldMapOther5xxToGenericServerErrorException() {
        MaxApiException exception = decode(502, "bad gateway", Map.of());

        assertThat(exception).isInstanceOf(MaxServerErrorException.class);
        assertThat(exception.statusCode()).isEqualTo(502);
    }

    private MaxApiException decode(int statusCode, String body, Map<String, List<String>> headers) {
        MaxHttpRequest request = new MaxHttpRequest(HttpMethod.GET, "/v1/test", Map.of(), new byte[0]);
        MaxHttpResponse response = new MaxHttpResponse(statusCode, headers, body.getBytes(StandardCharsets.UTF_8));
        return decoder.decode(request, response);
    }
}
