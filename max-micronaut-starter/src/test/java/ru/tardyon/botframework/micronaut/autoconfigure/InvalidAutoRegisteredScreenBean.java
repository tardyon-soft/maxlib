package ru.tardyon.botframework.micronaut.autoconfigure;

import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import ru.tardyon.botframework.screen.ScreenContext;
import ru.tardyon.botframework.screen.ScreenModel;
import ru.tardyon.botframework.screen.annotation.Screen;

@Singleton
@Requires(property = "spec.name", value = "screen-invalid")
@Screen("invalid")
public final class InvalidAutoRegisteredScreenBean {
    public ScreenModel render(ScreenContext context) {
        return ScreenModel.builder().title("invalid").build();
    }
}
