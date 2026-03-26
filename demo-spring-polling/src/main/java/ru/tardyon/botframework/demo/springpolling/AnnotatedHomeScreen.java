package ru.tardyon.botframework.demo.springpolling;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.screen.ScreenButton;
import ru.tardyon.botframework.screen.ScreenContext;
import ru.tardyon.botframework.screen.ScreenModel;
import ru.tardyon.botframework.screen.Widgets;
import ru.tardyon.botframework.screen.annotation.OnAction;
import ru.tardyon.botframework.screen.annotation.Render;
import ru.tardyon.botframework.screen.annotation.Screen;

/**
 * Annotation-based screen demo with media attachment.
 */
@Screen("annotated.home")
public final class AnnotatedHomeScreen {

    @Render
    public ScreenModel render(ScreenContext context) {
        return ScreenModel.builder()
                .title("Аннотационный экран: Главная")
                .widget(Widgets.text("Это пример @Screen + @Render + @OnAction + @OnText"))
                .widget(Widgets.text("Медиа-виджеты поддерживаются, но для MAX используйте валидный upload_ref/file_id."))
                .widget(Widgets.buttonRow(
                        ScreenButton.of("Открыть профиль", "open_profile")
                ))
                .showBackButton(false)
                .build();
    }

    @OnAction("open_profile")
    public CompletionStage<Void> openProfile(ScreenContext context) {
        return context.nav().push("annotated.profile", Map.of("name", "Гость"));
    }
}
