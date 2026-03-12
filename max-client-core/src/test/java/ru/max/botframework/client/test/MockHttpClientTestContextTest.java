package ru.max.botframework.client.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import ru.max.botframework.client.DefaultMaxBotClient;
import ru.max.botframework.client.RetryPolicy;

class MockHttpClientTestContextTest {

    @Test
    void shouldCreateConfiguredClientAndRecordRequests() {
        try (MockHttpClientTestContext context = MockHttpClientTestContext.start()) {
            DefaultMaxBotClient client = context.createClient(RetryPolicy.none());
            context.enqueueJsonFixture("bot-info-response.json");

            var me = client.getMe();

            assertThat(me.username()).isEqualTo("max_helper_bot");
            assertThat(context.requestCount()).isEqualTo(1);
            assertThat(context.takeRequest().getPath()).isEqualTo("/me");
        }
    }
}
