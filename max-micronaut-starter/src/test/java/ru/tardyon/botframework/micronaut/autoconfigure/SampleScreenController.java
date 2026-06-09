package ru.tardyon.botframework.micronaut.autoconfigure;

import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import ru.tardyon.botframework.micronaut.screen.annotation.OnScreenAction;
import ru.tardyon.botframework.micronaut.screen.annotation.OnScreenText;
import ru.tardyon.botframework.micronaut.screen.annotation.ScreenController;
import ru.tardyon.botframework.micronaut.screen.annotation.ScreenView;
import ru.tardyon.botframework.screen.ScreenModel;

@Singleton
@Requires(property = "spec.name", value = "screen-controller-wiring")
@ScreenController
public final class SampleScreenController {
    @ScreenView(screen = "controller.home")
    public ScreenModel home() {
        return ScreenModel.builder().title("home").build();
    }

    @OnScreenText(screen = "controller.home")
    public void onText(String text) {
    }

    @OnScreenAction(screen = "controller.home", action = "go")
    public java.util.concurrent.CompletionStage<Void> onAction(java.util.Map<String, String> args) {
        return java.util.concurrent.CompletableFuture.completedFuture(null);
    }

    @ScreenView(screen = "controller.profile")
    public ScreenModel profile() {
        return ScreenModel.builder().title("profile").build();
    }
}
