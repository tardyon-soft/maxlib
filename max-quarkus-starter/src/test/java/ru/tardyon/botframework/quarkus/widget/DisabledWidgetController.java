package ru.tardyon.botframework.quarkus.widget;

import jakarta.inject.Singleton;
import java.util.List;
import ru.tardyon.botframework.quarkus.widget.annotation.Widget;
import ru.tardyon.botframework.quarkus.widget.annotation.WidgetController;
import ru.tardyon.botframework.screen.WidgetView;

@Singleton
@WidgetController(autoRegister = false)
public final class DisabledWidgetController {
    @Widget(id = "disabled.widget")
    public WidgetView render() {
        return WidgetView.of(List.of("disabled"), List.of());
    }
}
