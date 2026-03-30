package ru.tardyon.botframework.spring.autoconfigure;

import ru.tardyon.botframework.screen.ScreenContext;
import ru.tardyon.botframework.screen.ScreenModel;
import ru.tardyon.botframework.spring.screen.annotation.OnScreenAction;
import ru.tardyon.botframework.spring.screen.annotation.ScreenController;
import ru.tardyon.botframework.spring.screen.annotation.ScreenView;

@ScreenController
public final class AutoDetectedScreenController {

    @ScreenView(screen = "autodetected.facade.home")
    public ScreenModel home(ScreenContext context) {
        return ScreenModel.builder().title("autodetected").build();
    }

    @OnScreenAction(screen = "autodetected.facade.home", action = "noop")
    public void onAction() {
        // no-op
    }
}

