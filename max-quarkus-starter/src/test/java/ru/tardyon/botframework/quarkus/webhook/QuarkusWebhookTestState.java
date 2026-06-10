package ru.tardyon.botframework.quarkus.webhook;

import java.util.concurrent.atomic.AtomicInteger;

final class QuarkusWebhookTestState {
    static final AtomicInteger HANDLER_CALLS = new AtomicInteger();

    private QuarkusWebhookTestState() {
    }
}
