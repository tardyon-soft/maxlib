package ru.tardyon.botframework.quarkus.widget;

import java.util.List;
import ru.tardyon.botframework.quarkus.widget.annotation.Widget;
import ru.tardyon.botframework.quarkus.widget.annotation.WidgetController;
import ru.tardyon.botframework.screen.WidgetView;

@WidgetController
public final class AutoDetectedWidgetController {
    @Widget(id = "autodetected.widget")
    public WidgetView render() {
        return WidgetView.of(List.of("autodetected widget"), List.of());
    }
}
