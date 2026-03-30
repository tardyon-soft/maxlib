package ru.tardyon.botframework.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ScreenModelWidgetLayerTest {

    @Test
    void builderAcceptsWidgetViewAndKeepsLegacyWidgets() {
        Widget legacy = Widgets.text("legacy");
        WidgetView view = WidgetView.of(List.of("view"), List.of());

        ScreenModel model = ScreenModel.builder()
                .title("screen")
                .widget(legacy)
                .widgetView(view)
                .showBackButton(true)
                .build();

        assertEquals(2, model.widgets().size());
        assertEquals(true, model.showBackButton());
    }
}

