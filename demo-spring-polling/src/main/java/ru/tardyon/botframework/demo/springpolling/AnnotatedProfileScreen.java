package ru.tardyon.botframework.demo.springpolling;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.screen.ScreenButton;
import ru.tardyon.botframework.screen.ScreenContext;
import ru.tardyon.botframework.screen.ScreenModel;
import ru.tardyon.botframework.screen.Widgets;
import ru.tardyon.botframework.screen.annotation.OnAction;
import ru.tardyon.botframework.screen.annotation.OnText;
import ru.tardyon.botframework.screen.annotation.Render;
import ru.tardyon.botframework.screen.annotation.Screen;

/**
 * Annotation-based profile screen.
 */
@Screen("annotated.profile")
public final class AnnotatedProfileScreen {

    @Render
    public ScreenModel render(ScreenContext context) {
        String name = String.valueOf(context.params().getOrDefault("name", "Гость"));
        return ScreenModel.builder()
                .title("Аннотационный экран: Профиль")
                .widget(Widgets.text("Имя: " + name))
                .widget(Widgets.text("Отправьте текст, чтобы изменить имя"))
                .widget(Widgets.buttonRow(ScreenButton.of("Сбросить имя", "reset_name")))
                .showBackButton(true)
                .build();
    }

    @OnAction("reset_name")
    public CompletionStage<Void> reset(ScreenContext context) {
        return context.nav().replace("annotated.profile", Map.of("name", "Гость"));
    }

    @OnText
    public CompletionStage<Void> onText(ScreenContext context, String text) {
        String next = text == null || text.isBlank() ? "Гость" : text.trim();
        return context.nav().replace("annotated.profile", Map.of("name", next));
    }
}

