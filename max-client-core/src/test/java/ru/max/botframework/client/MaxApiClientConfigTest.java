package ru.max.botframework.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class MaxApiClientConfigTest {

    @Test
    void shouldUseDefaultsForOptionalValues() {
        MaxApiClientConfig config = MaxApiClientConfig.builder()
                .token("token")
                .build();

        assertThat(config.baseUri()).isEqualTo(MaxApiClientConfig.DEFAULT_BASE_URI);
        assertThat(config.connectTimeout()).isEqualTo(MaxApiClientConfig.DEFAULT_CONNECT_TIMEOUT);
        assertThat(config.readTimeout()).isEqualTo(MaxApiClientConfig.DEFAULT_READ_TIMEOUT);
        assertThat(config.userAgent()).isEqualTo(MaxApiClientConfig.DEFAULT_USER_AGENT);
        assertThat(config.retryPolicy().maxAttempts()).isEqualTo(1);
        assertThat(config.retryPolicy().delay()).isEqualTo(Duration.ZERO);
    }

    @Test
    void shouldFailWhenTokenIsMissing() {
        assertThatThrownBy(() -> MaxApiClientConfig.builder().build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("token");
    }
}
