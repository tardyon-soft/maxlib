package ru.tardyon.botframework.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.model.TextFormat;

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
        assertEquals(TextFormat.PLAIN, model.format());
    }

    @Test
    void builderSupportsTextFormat() {
        ScreenModel markdown = ScreenModel.builder()
                .title("*screen*")
                .markdown()
                .build();
        ScreenModel html = ScreenModel.builder()
                .title("<b>screen</b>")
                .html()
                .build();

        assertEquals(TextFormat.MARKDOWN, markdown.format());
        assertEquals(TextFormat.HTML, html.format());
    }
}
