package ru.tardyon.botframework.spring.autoconfigure;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import ru.tardyon.botframework.dispatcher.annotation.Command;
import ru.tardyon.botframework.dispatcher.annotation.Route;

@Route(value = "autodetected-route", autoRegister = true)
public final class AutoDetectedRouteController {
    static final AtomicInteger CALLS = new AtomicInteger();

    @Command("autodetected")
    public CompletionStage<Void> onCommand() {
        CALLS.incrementAndGet();
        return CompletableFuture.completedFuture(null);
    }
}
