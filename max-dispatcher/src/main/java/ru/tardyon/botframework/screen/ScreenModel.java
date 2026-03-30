package ru.tardyon.botframework.screen;

import java.util.List;
import java.util.Objects;

/**
 * High-level screen view model.
 */
public record ScreenModel(
        String title,
        List<Widget> widgets,
        boolean showBackButton
) {
    public ScreenModel {
        widgets = widgets == null ? List.of() : List.copyOf(widgets);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String title;
        private final java.util.ArrayList<Widget> widgets = new java.util.ArrayList<>();
        private boolean showBackButton;

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

        public ScreenModel build() {
            return new ScreenModel(title, widgets, showBackButton);
        }
    }
}
