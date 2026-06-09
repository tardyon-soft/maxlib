package ru.tardyon.botframework.micronaut.autoconfigure;

import java.util.List;
import ru.tardyon.botframework.micronaut.widget.annotation.Widget;
import ru.tardyon.botframework.micronaut.widget.annotation.WidgetController;
import ru.tardyon.botframework.screen.WidgetView;

@WidgetController
public final class AutoDetectedWidgetController {
    @Widget(id = "autodetected.widget")
    public WidgetView render() {
        return WidgetView.of(List.of("autodetected widget"), List.of());
    }
}
