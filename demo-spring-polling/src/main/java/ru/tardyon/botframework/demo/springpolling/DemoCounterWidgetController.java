package ru.tardyon.botframework.demo.springpolling;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.screen.ScreenButton;
import ru.tardyon.botframework.screen.WidgetContext;
import ru.tardyon.botframework.screen.WidgetEffect;
import ru.tardyon.botframework.screen.WidgetView;
import ru.tardyon.botframework.spring.widget.annotation.OnWidgetAction;
import ru.tardyon.botframework.spring.widget.annotation.Widget;
import ru.tardyon.botframework.spring.widget.annotation.WidgetController;

/**
 * Demo widget controller used from facade screen.
 */
@WidgetController
public final class DemoCounterWidgetController {
    private static final String COUNTER_KEY = "demo.counter.value";

    @Widget(id = "demo.counter")
    public CompletionStage<WidgetView> render(WidgetContext context) {
        return context.screen().fsm().data().thenApply(data -> {
            int value = data.get(COUNTER_KEY).map(DemoCounterWidgetController::toInt).orElse(0);
            return WidgetView.of(
                    List.of("Widget counter value: " + value),
                    List.of(List.of(ScreenButton.of("Increment", "increment")))
            );
        });
    }

    @OnWidgetAction(widget = "demo.counter", action = "increment")
    public CompletionStage<WidgetEffect> increment(WidgetContext context) {
        return context.screen().fsm().data()
                .thenCompose(data -> {
                    int current = data.get(COUNTER_KEY).map(DemoCounterWidgetController::toInt).orElse(0);
                    return context.screen().fsm().updateData(Map.of(COUNTER_KEY, current + 1));
                })
                .thenApply(ignored -> WidgetEffect.RERENDER);
    }

    private static int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception exception) {
            return 0;
        }
    }
}

