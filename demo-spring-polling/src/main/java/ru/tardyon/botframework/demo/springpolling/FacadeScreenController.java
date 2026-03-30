package ru.tardyon.botframework.demo.springpolling;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.screen.ScreenButton;
import ru.tardyon.botframework.screen.ScreenContext;
import ru.tardyon.botframework.screen.ScreenModel;
import ru.tardyon.botframework.screen.Widgets;
import ru.tardyon.botframework.spring.screen.annotation.OnScreenAction;
import ru.tardyon.botframework.spring.screen.annotation.OnScreenText;
import ru.tardyon.botframework.spring.screen.annotation.ScreenController;
import ru.tardyon.botframework.spring.screen.annotation.ScreenView;

/**
 * Controller-facade example for screen API in spring starter.
 */
@ScreenController
public final class FacadeScreenController {

    @ScreenView(screen = "facade.home")
    public ScreenModel home(ScreenContext context) {
        return ScreenModel.builder()
                .title("Facade Screen: Home")
                .widget(Widgets.text("Это пример @ScreenController + @ScreenView + @OnScreenAction + @OnScreenText"))
                .widget(Widgets.ref("demo.counter"))
                .widget(Widgets.buttonRow(ScreenButton.of("Открыть профиль", "open_profile")))
                .showBackButton(false)
                .build();
    }

    @OnScreenAction(screen = "facade.home", action = "open_profile")
    public CompletionStage<Void> openProfile(ScreenContext context) {
        return context.nav().push("facade.profile", Map.of("name", "Гость"));
    }

    @ScreenView(screen = "facade.profile")
    public ScreenModel profile(ScreenContext context) {
        String name = String.valueOf(context.params().getOrDefault("name", "Гость"));
        return ScreenModel.builder()
                .title("Facade Screen: Profile")
                .widget(Widgets.text("Имя: " + name))
                .widget(Widgets.text("Отправьте текст, чтобы изменить имя"))
                .widget(Widgets.buttonRow(ScreenButton.of("Сбросить имя", "reset_name")))
                .showBackButton(true)
                .build();
    }

    @OnScreenAction(screen = "facade.profile", action = "reset_name")
    public CompletionStage<Void> resetProfile(ScreenContext context) {
        return context.nav().replace("facade.profile", Map.of("name", "Гость"));
    }

    @OnScreenText(screen = "facade.profile")
    public CompletionStage<Void> profileText(ScreenContext context, String text) {
        String next = text == null || text.isBlank() ? "Гость" : text.trim();
        return context.nav().replace("facade.profile", Map.of("name", next));
    }
}
