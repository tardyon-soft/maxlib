package ru.tardyon.botframework.micronaut.autoconfigure;

import ru.tardyon.botframework.micronaut.screen.annotation.OnScreenAction;
import ru.tardyon.botframework.micronaut.screen.annotation.ScreenController;
import ru.tardyon.botframework.micronaut.screen.annotation.ScreenView;

@ScreenController
public final class AutoDetectedScreenController {

    @ScreenView(screen = "autodetected.facade.home")
    public ru.tardyon.botframework.screen.ScreenModel home() {
        return ru.tardyon.botframework.screen.ScreenModel.builder().title("autodetected").build();
    }

    @OnScreenAction(screen = "autodetected.facade.home", action = "noop")
    public void onAction() {
        // no-op
    }
}
