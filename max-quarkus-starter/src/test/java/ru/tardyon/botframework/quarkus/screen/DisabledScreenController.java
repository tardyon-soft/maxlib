package ru.tardyon.botframework.quarkus.screen;

import jakarta.inject.Singleton;
import ru.tardyon.botframework.quarkus.screen.annotation.ScreenController;
import ru.tardyon.botframework.quarkus.screen.annotation.ScreenView;
import ru.tardyon.botframework.screen.ScreenModel;

@Singleton
@ScreenController(autoRegister = false)
public final class DisabledScreenController {
    @ScreenView(screen = "disabled.controller")
    public ScreenModel disabled() {
        return ScreenModel.builder().title("disabled").build();
    }
}
