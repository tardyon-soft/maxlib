package ru.tardyon.botframework.quarkus.widget;

import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.quarkus.widget.annotation.OnWidgetAction;
import ru.tardyon.botframework.quarkus.widget.annotation.Widget;
import ru.tardyon.botframework.quarkus.widget.annotation.WidgetController;
import ru.tardyon.botframework.screen.ScreenButton;
import ru.tardyon.botframework.screen.WidgetContext;
import ru.tardyon.botframework.screen.WidgetEffect;
import ru.tardyon.botframework.screen.WidgetView;

@Singleton
@WidgetController
public final class SampleWidgetController {
    @Widget(id = "widget.home")
    public WidgetView home() {
        return WidgetView.of(
                List.of("widget home"),
                List.of(List.of(ScreenButton.of("Go", "go")))
        );
    }

    @OnWidgetAction(widget = "widget.home", action = "go")
    public CompletionStage<WidgetEffect> go(WidgetContext context, Map<String, String> args) {
        return CompletableFuture.completedFuture(WidgetEffect.RERENDER);
    }
}
