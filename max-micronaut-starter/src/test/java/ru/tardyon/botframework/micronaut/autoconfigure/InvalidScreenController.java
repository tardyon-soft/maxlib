package ru.tardyon.botframework.micronaut.autoconfigure;

import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import ru.tardyon.botframework.micronaut.screen.annotation.ScreenController;
import ru.tardyon.botframework.micronaut.screen.annotation.ScreenView;

@Singleton
@Requires(property = "spec.name", value = "screen-controller-invalid")
@ScreenController
public final class InvalidScreenController {
    @ScreenView(screen = "invalid")
    public String invalidView() {
        return "bad";
    }
}
