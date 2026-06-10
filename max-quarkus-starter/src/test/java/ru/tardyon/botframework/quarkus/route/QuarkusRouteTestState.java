package ru.tardyon.botframework.quarkus.route;

import java.util.concurrent.atomic.AtomicInteger;

final class QuarkusRouteTestState {
    static final AtomicInteger AUTO_DETECTED_CALLS = new AtomicInteger();

    private QuarkusRouteTestState() {
    }
}
