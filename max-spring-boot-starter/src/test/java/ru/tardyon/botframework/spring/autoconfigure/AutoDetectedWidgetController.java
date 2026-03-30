package ru.tardyon.botframework.spring.autoconfigure;

import java.util.List;
import ru.tardyon.botframework.screen.WidgetView;
import ru.tardyon.botframework.spring.widget.annotation.Widget;
import ru.tardyon.botframework.spring.widget.annotation.WidgetController;

@WidgetController
public final class AutoDetectedWidgetController {
    @Widget(id = "autodetected.widget")
    public WidgetView render() {
        return WidgetView.of(List.of("autodetected widget"), List.of());
    }
}

