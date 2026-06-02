# `@ScreenController`, `@WidgetController` и facade API

Этот слой есть только в `max-spring-boot-starter`. Он нужен для случая, когда экран описывается не отдельным классом `@Screen`, а набором методов в одном Spring bean.

## Что делает `@ScreenController`

`@ScreenController` помечает Spring bean, из которого starter собирает один или несколько `ScreenDefinition`.

Один controller может описывать несколько screen id. Группировка идет по строке `screen` в аннотациях методов:

- `@ScreenView(screen = "...")`
- `@OnScreenAction(screen = "...", action = "...")`
- `@OnScreenText(screen = "...")`

Для каждого screen id starter строит отдельный `ScreenDefinition`.

## Авто-регистрация

Если класс помечен `@ScreenController`, starter может сам зарегистрировать его как Spring bean даже без `@Component`, а затем автоматически подключить методы как screens. У `@ScreenController` есть флаг:

```java
@ScreenController(autoRegister = true)
```

По умолчанию он уже `true`.

## Минимальный пример

```java
import java.util.Map;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.screen.ScreenButton;
import ru.tardyon.botframework.screen.ScreenContext;
import ru.tardyon.botframework.screen.ScreenModel;
import ru.tardyon.botframework.screen.Widgets;
import ru.tardyon.botframework.spring.screen.annotation.OnScreenAction;
import ru.tardyon.botframework.spring.screen.annotation.OnScreenText;
import ru.tardyon.botframework.spring.screen.annotation.ScreenController;
import ru.tardyon.botframework.spring.screen.annotation.ScreenView;

@ScreenController
public final class ProfileController {

    @ScreenView(screen = "profile")
    public ScreenModel profile(ScreenContext ctx) {
        String name = String.valueOf(ctx.params().getOrDefault("name", "Гость"));
        return ScreenModel.builder()
                .title("Профиль")
                .widget(Widgets.text("Имя: " + name))
                .widget(Widgets.text("Отправьте новый текст, чтобы изменить имя"))
                .widget(Widgets.buttonRow(ScreenButton.of("Сбросить", "reset_name")))
                .showBackButton(true)
                .build();
    }

    @OnScreenAction(screen = "profile", action = "reset_name")
    public CompletionStage<Void> reset(ScreenContext ctx) {
        return ctx.nav().replace("profile", Map.of("name", "Гость"));
    }

    @OnScreenText(screen = "profile")
    public CompletionStage<Void> onText(ScreenContext ctx, String text) {
        String next = text == null || text.isBlank() ? "Гость" : text.trim();
        return ctx.nav().replace("profile", Map.of("name", next));
    }
}
```

## Обязательные правила

- Для каждого `screen` должен быть ровно один `@ScreenView`.
- `@OnScreenAction` для одной пары `(screen, action)` может быть только один.
- `@OnScreenText` для одного `screen` может быть только один.
- Если в controller вообще нет ни одного screen mapping, starter выбросит ошибку.
- Если у screen есть `@OnScreenAction` и пришло неизвестное `action`, runtime просто ничего не делает.
- Если у screen нет `@OnScreenText`, текстовый ввод для этого экрана игнорируется.

## Поддерживаемые параметры методов `@ScreenController`

Для `@ScreenView`, `@OnScreenAction`, `@OnScreenText` разрешены только такие параметры:

- `ScreenContext`
- `RuntimeContext`
- `Message`
- `Callback`
- `String`
- `Map`

Но есть важное ограничение:

- `@ScreenView` не может принимать `String` и `Map`

Как заполняются параметры:

- `ScreenContext` - текущий screen context
- `RuntimeContext` - runtime текущего update
- `Message` - `update.message()` или `callback.message()`, если экран открыт из callback
- `Callback` - текущий callback, если update пришел из callback
- `String` - payload строки
- `Map` - action args

### Что именно получает `String`

- в `@OnScreenText` - введенный пользователем текст
- в `@OnScreenAction` - имя action

### Что именно получает `Map`

- в `@OnScreenAction` - `Map<String, String>` с args кнопки
- в `@OnScreenText` обычно передается пустая map

## Возвращаемые типы методов `@ScreenController`

### `@ScreenView`

Разрешены только:

- `ScreenModel`
- `CompletionStage<ScreenModel>`

Любой другой return type вызовет ошибку регистрации.

### `@OnScreenAction`

Разрешены только:

- `void`
- `Void`
- `CompletionStage<?>`

Фактически этот обработчик должен выполнить побочный эффект, например:

- `ctx.nav().push(...)`
- `ctx.nav().replace(...)`
- `ctx.nav().rerender()`
- изменение данных через `ctx.fsm()`

### `@OnScreenText`

Разрешены те же типы:

- `void`
- `Void`
- `CompletionStage<?>`

## Несколько экранов в одном controller

Это основной сценарий facade API. Один bean может описывать экранную группу:

```java
@ScreenController
public final class AccountScreens {

    @ScreenView(screen = "account.home")
    public ScreenModel home(ScreenContext ctx) { /* ... */ }

    @OnScreenAction(screen = "account.home", action = "open_profile")
    public CompletionStage<Void> openProfile(ScreenContext ctx) { /* ... */ }

    @ScreenView(screen = "account.profile")
    public ScreenModel profile(ScreenContext ctx) { /* ... */ }

    @OnScreenText(screen = "account.profile")
    public CompletionStage<Void> profileText(ScreenContext ctx, String text) { /* ... */ }
}
```

## Как screen action попадает в `@OnScreenAction`

Обычно action задается через `ScreenButton`:

```java
Widgets.buttonRow(
        ScreenButton.of("Открыть профиль", "open_profile")
)
```

Если нужны аргументы:

```java
Widgets.buttonRow(
        ScreenButton.callback("Открыть", "open_profile", Map.of("id", "42"))
)
```

Тогда метод может принять `Map`:

```java
@OnScreenAction(screen = "home", action = "open_profile")
public CompletionStage<Void> openProfile(ScreenContext ctx, Map<String, String> args) {
    String id = args.get("id");
    return ctx.nav().push("profile", Map.of("id", id));
}
```

## Когда лучше использовать `@Screen`, а когда `@ScreenController`

`@Screen` удобнее, когда:

- один класс = один экран
- логика экрана локальна
- не нужен controller-style grouping

`@ScreenController` удобнее, когда:

- несколько экранов образуют один flow
- хочется держать navigation и handlers в одном bean
- нужен привычный controller-стиль для Spring

## Что делает `@WidgetController`

`@WidgetController` - это аналог facade API для widgets.

Из такого bean starter строит две runtime-роли:

- `WidgetViewResolver`
- `WidgetActionDispatcher`

Как и `@ScreenController`, класс с `@WidgetController` starter умеет:

- зарегистрировать как Spring bean без `@Component`
- автоматически подключить в runtime, если `autoRegister=true`

Виджет потом можно подключить в screen через:

```java
Widgets.ref("demo.counter")
```

## Минимальный пример widget controller

```java
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.screen.ScreenButton;
import ru.tardyon.botframework.screen.WidgetContext;
import ru.tardyon.botframework.screen.WidgetEffect;
import ru.tardyon.botframework.screen.WidgetView;
import ru.tardyon.botframework.spring.widget.annotation.OnWidgetAction;
import ru.tardyon.botframework.spring.widget.annotation.Widget;
import ru.tardyon.botframework.spring.widget.annotation.WidgetController;

@WidgetController
public final class CounterWidgetController {
    private static final String COUNTER_KEY = "demo.counter.value";

    @Widget(id = "demo.counter")
    public CompletionStage<WidgetView> render(WidgetContext context) {
        return context.screen().fsm().data().thenApply(data -> {
            int value = data.get(COUNTER_KEY).map(v -> Integer.parseInt(String.valueOf(v))).orElse(0);
            return WidgetView.of(
                    List.of("Counter: " + value),
                    List.of(List.of(ScreenButton.of("Increment", "increment")))
            );
        });
    }

    @OnWidgetAction(widget = "demo.counter", action = "increment")
    public CompletionStage<WidgetEffect> increment(WidgetContext context) {
        return context.screen().fsm().data()
                .thenCompose(data -> {
                    int current = data.get(COUNTER_KEY).map(v -> Integer.parseInt(String.valueOf(v))).orElse(0);
                    return context.screen().fsm().updateData(Map.of(COUNTER_KEY, current + 1));
                })
                .thenApply(ignored -> WidgetEffect.RERENDER);
    }
}
```

## Правила регистрации widget controller

- В controller должен быть хотя бы один `@Widget`.
- Для каждого `widget id` может быть только один render-метод.
- Для пары `(widget, action)` может быть только один action handler.
- Если widget с таким id уже зарегистрирован, будет ошибка.

## Поддерживаемые параметры методов `@WidgetController`

Разрешены:

- `WidgetContext`
- `ScreenContext`
- `RuntimeContext`
- `Message`
- `Callback`
- `String`
- `Map`

Как заполняются:

- `WidgetContext` - текущий widget context
- `ScreenContext` - screen, внутри которого рендерится widget
- `RuntimeContext` - runtime текущего update
- `Message` - текущее message-событие
- `Callback` - callback-событие
- `String` - имя widget action
- `Map` - payload/args

## Возвращаемые типы методов `@WidgetController`

### `@Widget`

Разрешены только:

- `WidgetView`
- `CompletionStage<WidgetView>`

### `@OnWidgetAction`

Разрешены:

- `void`
- `Void`
- `WidgetEffect`
- `CompletionStage<Void>`
- `CompletionStage<WidgetEffect>`

Если метод возвращает `null` или `void`, runtime трактует это как `WidgetEffect.RERENDER`.

Поддерживаемые эффекты:

- `WidgetEffect.NONE`
- `WidgetEffect.RERENDER`

## Как работают кнопки widget

Если в `WidgetView` кнопка имеет тип callback, registry автоматически перекодирует action во внутренний widget callback. То есть внутри render-метода можно писать обычное:

```java
ScreenButton.of("Increment", "increment")
```

А потом ловить это в:

```java
@OnWidgetAction(widget = "demo.counter", action = "increment")
```

Ручное кодирование callback для widget не требуется.

## Что доступно в `WidgetContext`

`WidgetContext` содержит:

- `screen()` - текущий `ScreenContext`
- `widgetId()` - id виджета
- `viewParams()` - параметры, переданные через `Widgets.ref("id", params)`
- `message()` - текущее сообщение
- `callback()` - текущий callback
- `runtime()` - shortcut к `screen().runtime()`

## Когда использовать widget controller

Он полезен, когда:

- один и тот же кусок UI нужно переиспользовать в нескольких screens
- у блока есть собственные callback actions
- хочется изолировать маленький интерактивный компонент от большого screen controller

Если логика маленькая и переиспользование не нужно, проще остаться на обычных `Widgets.text(...)`, `Widgets.buttonRow(...)` и других factory-методах.
