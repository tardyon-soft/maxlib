# Screen Facade Guide

Руководство по facade-аннотациям для screen API в `max-spring-boot-starter`.

## Что это

Facade слой позволяет описывать экраны в стиле контроллеров:

- `@ScreenController`
- `@ScreenView(screen = "...")`
- `@OnScreenAction(screen = "...", action = "...")`
- `@OnScreenText(screen = "...")`

Этот слой совместим со старым API (`@Screen/@Render/@OnAction/@OnText`) и не требует обязательной миграции.

## Минимальный пример

```java
@ScreenController
public final class ProfileController {

    @ScreenView(screen = "profile.home")
    public ScreenModel home(ScreenContext ctx) {
        String name = String.valueOf(ctx.params().getOrDefault("name", "Гость"));
        return ScreenModel.builder()
                .title("Профиль")
                .widget(Widgets.text("Имя: " + name))
                .widget(Widgets.buttonRow(ScreenButton.of("Сбросить", "reset_name")))
                .showBackButton(true)
                .build();
    }

    @OnScreenAction(screen = "profile.home", action = "reset_name")
    public CompletionStage<Void> reset(ScreenContext ctx) {
        return ctx.nav().replace("profile.home", Map.of("name", "Гость"));
    }

    @OnScreenText(screen = "profile.home")
    public CompletionStage<Void> onText(ScreenContext ctx, String text) {
        String next = text == null || text.isBlank() ? "Гость" : text.trim();
        return ctx.nav().replace("profile.home", Map.of("name", next));
    }
}
```

## Поддерживаемые параметры методов

- `ScreenContext`
- `RuntimeContext`
- `Message`
- `Callback`
- `String` (text/action payload)
- `Map<String, String>` (action args)

## Когда использовать facade

- Когда в проекте уже используется Spring и удобнее controller-like структура.
- Когда один класс должен описывать несколько экранов.
- Когда хотите постепенно мигрировать с legacy аннотаций.

## Inline-кнопки в ScreenButton

В facade/screen API `ScreenButton` поддерживает не только callback-action, но и остальные inline-типы:

```java
ScreenModel.builder()
        .title("Buttons")
        .widget(Widgets.buttonRow(
                ScreenButton.of("Открыть профиль", "open_profile"),
                ScreenButton.clipboard("Скопировать код", "invoice-42")
        ))
        .widget(Widgets.buttonRow(
                ScreenButton.link("Документация", "https://example.com/docs"),
                ScreenButton.requestContact("Поделиться контактом")
        ))
        .widget(Widgets.buttonRow(
                ScreenButton.requestGeoLocation("Поделиться гео"),
                ScreenButton.openApp("Открыть mini app", "app:orders")
        ))
        .widget(Widgets.buttonRow(
                ScreenButton.message("Отправить шаблон", "hello-from-screen")
        ))
        .build();
```

`@OnScreenAction` и `@OnWidgetAction` обрабатывают только callback-кнопки (`ScreenButton.of(...)` / `ScreenButton.callback(...)`). Остальные типы рендерятся как inline-кнопки MAX API и не возвращаются в callback pipeline.

## Когда можно оставить старый API

- Если текущий `@Screen/@Render/@OnAction/@OnText` код стабилен и понятен.
- Если нет ценности в смене стиля на controller facade.

## См. также

- [Widget Layer Guide](widget-layer-guide.md)
- [Form Engine Guide](form-engine-guide.md)
- [Screen Callback Codec Modes](screen-callback-codec-modes.md)
- [Screen Migration Notes](screen-migration-notes.md)
- [Screen E2E Scenario](screen-e2e-scenario.md)
