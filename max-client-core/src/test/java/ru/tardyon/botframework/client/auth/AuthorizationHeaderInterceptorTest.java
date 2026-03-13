package ru.tardyon.botframework.client.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.client.http.MaxHttpRequest;

class AuthorizationHeaderInterceptorTest {

    @Test
    void shouldAddAuthorizationHeaderWhenMissing() {
        AuthorizationHeaderInterceptor interceptor = new AuthorizationHeaderInterceptor(() -> "Bearer token");
        MaxHttpRequest request = new MaxHttpRequest(HttpMethod.GET, "/v1/test", Map.of(), new byte[0]);

        MaxHttpRequest intercepted = interceptor.intercept(request);

        assertThat(intercepted.headers()).containsEntry(AuthorizationHeaderInterceptor.AUTHORIZATION_HEADER, "Bearer token");
    }

    @Test
    void shouldNotOverrideExistingAuthorizationHeader() {
        AuthorizationHeaderInterceptor interceptor = new AuthorizationHeaderInterceptor(() -> "Bearer generated");
        MaxHttpRequest request = new MaxHttpRequest(
                HttpMethod.POST,
                "/v1/test",
                Map.of(AuthorizationHeaderInterceptor.AUTHORIZATION_HEADER, "Bearer existing"),
                "{}".getBytes(StandardCharsets.UTF_8)
        );

        MaxHttpRequest intercepted = interceptor.intercept(request);

        assertThat(intercepted.headers()).containsEntry(AuthorizationHeaderInterceptor.AUTHORIZATION_HEADER, "Bearer existing");
    }

    @Test
    void shouldSkipBlankAuthorizationValue() {
        AuthorizationHeaderInterceptor interceptor = new AuthorizationHeaderInterceptor(() -> "  ");
        MaxHttpRequest request = new MaxHttpRequest(HttpMethod.GET, "/v1/test", Map.of(), new byte[0]);

        MaxHttpRequest intercepted = interceptor.intercept(request);

        assertThat(intercepted.headers()).doesNotContainKey(AuthorizationHeaderInterceptor.AUTHORIZATION_HEADER);
    }
}
