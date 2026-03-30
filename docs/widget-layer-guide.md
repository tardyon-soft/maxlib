# Widget Layer Guide

Руководство по аннотационному widget layer.

## Что это

Widget layer позволяет выносить части интерфейса экрана в отдельные контроллеры:

- `@WidgetController`
- `@Widget(id = "...")`
- `@OnWidgetAction(widget = "...", action = "...")`

В screen render виджет подключается через `Widgets.ref("widget.id")`.

## Минимальный пример

```java
@WidgetController
public final class CounterWidgetController {

    @Widget(id = "demo.counter")
    public WidgetView view(WidgetContext context) {
        int value = Integer.parseInt(String.valueOf(context.params().getOrDefault("counter", 0)));
        return WidgetView.of(
                List.of("Счетчик: " + value),
                List.of(List.of(ScreenButton.of("+1", "widget:increment")))
        );
    }

    @OnWidgetAction(widget = "demo.counter", action = "increment")
    public WidgetEffect increment(WidgetContext context) {
        return WidgetEffect.RERENDER;
    }
}
```

## Использование в экране

```java
ScreenModel.builder()
        .title("Dashboard")
        .widget(Widgets.text("Сводка"))
        .widget(Widgets.ref("demo.counter", Map.of("counter", 10)))
        .build();
```

## Валидация конфликтов

При старте приложения валидируются:

- дубли `widgetId`;
- дубли `(widgetId, action)`;
- некорректные сигнатуры методов.

## Совместимость

- Старый `Widget` API продолжает работать.
- Новый layer можно внедрять точечно, не переписывая все экраны.

## См. также

- [Screen Facade Guide](screen-facade-guide.md)
- [Screen E2E Scenario](screen-e2e-scenario.md)
- [Screen Migration Notes](screen-migration-notes.md)
