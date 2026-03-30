# Screen Callback Codec Modes

Руководство по режимам кодирования callback payload для screen действий.

## Property

`max.bot.screen.callback.codec.mode`

Поддерживаемые значения:

- `LEGACY_STRING` (по умолчанию)
- `TYPED_V1`

## LEGACY_STRING

Формат payload:

- `ui:act:<action>?k=v`

Плюсы:

- полная совместимость со старым поведением;
- простой человекочитаемый payload.

Когда выбирать:

- в проектах, где уже есть накопленные callback payload правила;
- при постепенной миграции без изменений бизнес-логики.

## TYPED_V1

Формат payload сериализуется typed codec (`TypedV1ScreenActionCodec`).

Особенности:

- payload кодируется/декодируется как DTO;
- в runtime есть backward-compatible fallback parsing legacy payload;
- старые экраны продолжают работать.

Когда выбирать:

- если нужен более строгий и расширяемый формат;
- если хотите унифицировать callback контракты между сервисами.

## Пример `application.yml`

```yaml
max:
  bot:
    screen:
      callback:
        codec:
          mode: LEGACY_STRING
```

```yaml
max:
  bot:
    screen:
      callback:
        codec:
          mode: TYPED_V1
```

## Совместимость и rollout

Рекомендуемый rollout:

1. Оставить `LEGACY_STRING` в production.
2. Поднять staging с `TYPED_V1`.
3. Прогнать screen e2e сценарии и callback regressions.
4. Переключить production.

Проекты на старом API продолжают работать в обоих режимах благодаря fallback.

## См. также

- [Screen Migration Notes](screen-migration-notes.md)
- [Screen E2E Scenario](screen-e2e-scenario.md)
