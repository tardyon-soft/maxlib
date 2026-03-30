# Screen E2E Scenario

Полный end-to-end сценарий: facade + widget layer + form engine + callback codec mode.

## Цель

Собрать flow "Планирование поста" в одном боте:

1. Пользователь открывает home screen.
2. Видит widget-счетчик черновиков.
3. Нажимает кнопку "Создать отложенный пост".
4. Проходит форму выбора канала/дня/времени.
5. Получает финальное подтверждение.

## Конфигурация

```yaml
max:
  bot:
    screen:
      callback:
        codec:
          mode: TYPED_V1
```

## Шаг 1. Экран через facade

```java
@ScreenController
public final class ScheduleController {

    @ScreenView(screen = "schedule.home")
    public ScreenModel home(ScreenContext ctx) {
        return ScreenModel.builder()
                .title("Планировщик")
                .widget(Widgets.ref("draft.counter"))
                .widget(Widgets.buttonRow(ScreenButton.of("Создать отложенный пост", "open_form")))
                .showBackButton(false)
                .build();
    }

    @OnScreenAction(screen = "schedule.home", action = "open_form")
    public CompletionStage<Void> openForm(ScreenContext ctx) {
        return ctx.nav().push("schedule.form", Map.of());
    }
}
```

## Шаг 2. Виджет

```java
@WidgetController
public final class DraftCounterWidget {

    @Widget(id = "draft.counter")
    public WidgetView view(WidgetContext ctx) {
        return WidgetView.of(
                List.of("Черновики: 3"),
                List.of()
        );
    }
}
```

## Шаг 3. Form engine

```java
FormDefinition scheduleForm = FormDefinition.of(
        "schedule.form.v1",
        FormStep.of("channel", "Введите ссылку канала", FormValidator.required("Ссылка обязательна")),
        FormStep.of("day", "СЕГОДНЯ / ЗАВТРА / ПОСЛЕЗАВТРА", FormValidator.required("День обязателен")),
        FormStep.of("time", "HH:mm", FormValidator.required("Время обязательно"))
);

FormEngine formEngine = new FormEngine(new FsmFormStateStorage());
```

Роуты/handlers работают с `ctx.fsm("screen")` и переходами `next/back/cancel/submit`.

## Шаг 4. Поведение callback codec

- При `LEGACY_STRING` действия кодируются как `ui:act:...`.
- При `TYPED_V1` используется typed payload.
- В обоих режимах legacy callbacks остаются читаемыми через fallback.

## Критерии готовности сценария

- Открытие home экрана работает.
- Кнопка действия переводит на form screen.
- Форма проходит все шаги и валидирует ввод.
- `submit` завершает flow и очищает form state.
- Поведение корректно в `LEGACY_STRING` и `TYPED_V1`.

## Связанные гайды

- [Screen Facade Guide](screen-facade-guide.md)
- [Widget Layer Guide](widget-layer-guide.md)
- [Form Engine Guide](form-engine-guide.md)
- [Screen Callback Codec Modes](screen-callback-codec-modes.md)
- [Screen Migration Notes](screen-migration-notes.md)
