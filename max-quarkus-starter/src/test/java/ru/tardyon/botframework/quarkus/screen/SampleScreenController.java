package ru.tardyon.botframework.quarkus.screen;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import jakarta.inject.Singleton;
import ru.tardyon.botframework.quarkus.screen.annotation.OnScreenAction;
import ru.tardyon.botframework.quarkus.screen.annotation.OnScreenText;
import ru.tardyon.botframework.quarkus.screen.annotation.ScreenController;
import ru.tardyon.botframework.quarkus.screen.annotation.ScreenView;
import ru.tardyon.botframework.screen.ScreenModel;

@Singleton
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
    public CompletionStage<Void> onAction(Map<String, String> args) {
        return CompletableFuture.completedFuture(null);
    }

    @ScreenView(screen = "controller.profile")
    public ScreenModel profile() {
        return ScreenModel.builder().title("profile").build();
    }
}
