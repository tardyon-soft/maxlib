package ru.tardyon.botframework.quarkus.screen;

import ru.tardyon.botframework.screen.ScreenContext;
import ru.tardyon.botframework.screen.ScreenModel;
import ru.tardyon.botframework.screen.annotation.Render;
import ru.tardyon.botframework.screen.annotation.Screen;

@Screen("autodetected")
public final class AutoDetectedScreenBean {
    @Render
    public ScreenModel render(ScreenContext context) {
        return ScreenModel.builder().title("autodetected").build();
    }
}
