package ru.tardyon.botframework.quarkus.route;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.dispatcher.annotation.Command;
import ru.tardyon.botframework.dispatcher.annotation.Route;

@Route(value = "autodetected-route", autoRegister = true)
public class AutoDetectedRouteController {
    @Command("start")
    public CompletionStage<Void> onStart() {
        QuarkusRouteTestState.AUTO_DETECTED_CALLS.incrementAndGet();
        return CompletableFuture.completedFuture(null);
    }
}
