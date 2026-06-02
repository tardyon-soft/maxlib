# FSM, scenes, wizards, screens и forms

## FSM

`FSMContext` - основной API для работы с состоянием:

- `currentState()`
- `setState(...)`
- `clearState()`
- `data()`
- `setData(...)`
- `updateData(...)`
- `clearData()`
- `clear()`

Пример:

```java
router.message(BuiltInFilters.command("form"), (message, ctx) ->
        ctx.fsm().setState("demo.form.name")
                .thenAccept(ignored -> ctx.reply(Messages.text("Введите имя")))
);

router.message(BuiltInFilters.state("demo.form.name"), (message, ctx) ->
        ctx.fsm().updateData(Map.of("name", message.text()))
                .thenCompose(ignored -> ctx.fsm().setState("demo.form.done"))
);
```

## Scopes

В starter по умолчанию используется `StateScope.USER_IN_CHAT`.

Без Spring его можно задать явно:

```java
dispatcher.withStateScope(StateScope.USER_IN_CHAT);
```

## Scenes и wizards

В `max-fsm` есть два дополнительных runtime API:

- `SceneManager`
- `WizardManager`

`SceneManager` поддерживает:

- `currentScene()`
- `enter(...)`
- `exit()`
- `transition(...)`

`WizardManager` поддерживает:

- `enter(...)`
- `currentStep()`
- `next()`
- `back()`
- `exit()`

## Screen API

Screen runtime построен вокруг:

- `ScreenRegistry`
- `ScreenDefinition`
- `ScreenNavigator`
- `ScreenRouter`
- `Screens.navigator(...)`

Для классического варианта экран регистрируется вручную, а `ScreenRouter.attach(...)` подключает обработку callback и text input в обычный `Router`.

```java
ScreenRegistry screenRegistry = new InMemoryScreenRegistry();

screenRegistry.register(new ScreenDefinition() {
    @Override
    public String id() {
        return "home";
    }

    @Override
    public CompletionStage<ScreenModel> render(ScreenContext context) {
        return CompletableFuture.completedFuture(
                ScreenModel.builder()
                        .title("Главная")
                        .widget(Widgets.text("Ручная регистрация screen"))
                        .showBackButton(false)
                        .build()
        );
    }
});

ScreenRouter.attach(router, screenRegistry);

router.message(BuiltInFilters.command("screen"), (message, ctx) ->
        Screens.navigator(ctx, screenRegistry).start("home", Map.of())
);
```

## Аннотационные screens

Поддерживаются:

- `@Screen`
- `@Render`
- `@OnAction`
- `@OnText`

Минимальный пример:

```java
@Screen("profile")
public final class ProfileScreen {
    @Render
    public ScreenModel render(ScreenContext ctx) {
        String name = String.valueOf(ctx.params().getOrDefault("name", "Гость"));
        return ScreenModel.builder()
                .title("Профиль")
                .widget(Widgets.text("Имя: " + name))
                .showBackButton(true)
                .build();
    }
}
```

## Screen namespace и callback codec

У screens есть отдельный FSM namespace. В starter он настраивается через:

```yaml
max:
  bot:
    screen:
      namespace: max.screen
      callback:
        codec:
          mode: LEGACY_STRING
```

Поддерживаемые режимы codec:

- `LEGACY_STRING`
- `TYPED_V1`

## Widgets

Фабрика `Widgets` поддерживает:

- `text(...)`
- `spacer()`
- `buttonRow(...)`
- `media(...)`
- `image(...)`
- `sticker(...)`
- `location(...)`
- `share(...)`
- `attachment(...)`
- `attachments(...)`
- `view(...)`
- `ref(...)`
- `section(...)`

В Spring starter можно использовать `@WidgetController`, `@Widget` и `@OnWidgetAction`.

Подробный разбор facade API вынесен отдельно:

- [docs/screen-controllers.md](screen-controllers.md)

## Forms

В screen-слое есть form API:

- `FormDefinition`
- `FormStep`
- `FormValidator`
- `FormEngine`
- `FormStateStorage`
- `FsmFormStateStorage`

Типичный сценарий:

1. Создать `FormDefinition`.
2. Запустить форму через `FormEngine.start(...)`.
3. На каждом сообщении передавать текст в `FormEngine`.
4. Хранить прогресс в `FsmFormStateStorage`.
