package ru.tardyon.botframework.screen;

import java.util.List;
import java.util.Objects;
import ru.tardyon.botframework.model.TextFormat;

/**
 * High-level screen view model.
 */
public record ScreenModel(
        String title,
        List<Widget> widgets,
        boolean showBackButton,
        TextFormat format
) {
    public ScreenModel {
        widgets = widgets == null ? List.of() : List.copyOf(widgets);
        format = format == null ? TextFormat.PLAIN : format;
    }

    public ScreenModel(String title, List<Widget> widgets, boolean showBackButton) {
        this(title, widgets, showBackButton, TextFormat.PLAIN);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String title;
        private final java.util.ArrayList<Widget> widgets = new java.util.ArrayList<>();
        private boolean showBackButton;
        private TextFormat format = TextFormat.PLAIN;

        public Builder title(String value) {
            this.title = value;
            return this;
        }

        public Builder widget(Widget widget) {
            widgets.add(Objects.requireNonNull(widget, "widget"));
            return this;
        }

        public Builder widgetView(WidgetView widgetView) {
            return widget(Objects.requireNonNull(widgetView, "widgetView").asWidget());
        }

        public Builder widgets(List<Widget> values) {
            Objects.requireNonNull(values, "values");
            values.forEach(this::widget);
            return this;
        }

        public Builder widgetViews(List<WidgetView> values) {
            Objects.requireNonNull(values, "values");
            values.forEach(this::widgetView);
            return this;
        }

        public Builder showBackButton(boolean value) {
            this.showBackButton = value;
            return this;
        }

        public Builder format(TextFormat value) {
            this.format = Objects.requireNonNull(value, "value");
            return this;
        }

        public Builder plain() {
            return format(TextFormat.PLAIN);
        }

        public Builder markdown() {
            return format(TextFormat.MARKDOWN);
        }

        public Builder html() {
            return format(TextFormat.HTML);
        }

        public ScreenModel build() {
            return new ScreenModel(title, widgets, showBackButton, format);
        }
    }
}
