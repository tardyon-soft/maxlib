# Form Engine Guide

Руководство по многошаговым формам (`max-dispatcher/screen.form`).

## Основные типы

- `FormDefinition`
- `FormStep`
- `FormState`
- `FormValidator`
- `FormEngine`
- `FormStateStorage`
- `FsmFormStateStorage`

## Переходы

Поддерживаются переходы:

- `next`
- `back`
- `cancel`
- `submit`

Валидация шага выполняется на `next/submit`. Если валидация не прошла, переход блокируется.

## Минимальный пример

```java
FormDefinition form = FormDefinition.of(
        "channel.form",
        FormStep.of("channel", "Введите ссылку канала", FormValidator.required("Ссылка обязательна")),
        FormStep.of("timezone", "Введите таймзону (+03:00)", FormValidator.required("Таймзона обязательна"))
);

FormEngine engine = new FormEngine(new FsmFormStateStorage());

engine.start(ctx.fsm("screen"), form);
// next/back/cancel/submit вызываются в message/callback handlers
```

## Хранение состояния

Рекомендуемая модель:

- хранить форму в `screen` namespace (`ctx.fsm("screen")`);
- использовать `FsmFormStateStorage`.

Ключ storage payload: `ui.form.state.v1`.

## Паттерн обработки ввода

1. Получить текущее состояние формы: `engine.current(fsm)`.
2. Определить переход (`back/cancel/next/submit`) по тексту/кнопке.
3. Выполнить переход.
4. Если `blocked()`, отправить `result.error()` и текущий prompt.
5. Если `finished()`, отправить summary и очистить flow.

## Совместимость

- Form engine не ломает существующий FSM/API.
- Можно использовать параллельно с legacy `BuiltInFilters.state("...")` формами.

## См. также

- [Screen E2E Scenario](screen-e2e-scenario.md)
- [Screen Migration Notes](screen-migration-notes.md)
