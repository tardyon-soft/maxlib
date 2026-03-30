# Screen Migration Notes

Пошаговая миграция со старого screen API на новый facade/widget/form слой.

## Важно: обязательной миграции нет

Проекты на старом API продолжают работать:

- `@Screen`
- `@Render`
- `@OnAction`
- `@OnText`

Новый API добавлен как расширение и может внедряться постепенно.

## Шаг 0. Зафиксировать baseline

Перед миграцией:

1. Зафиксируйте текущие e2e сценарии экранов.
2. Добавьте/обновите тесты на callback и text handlers.
3. Убедитесь, что текущий runtime стабилен в `LEGACY_STRING`.

## Шаг 1. Включить facade для новых экранов

- Не переписывайте все сразу.
- Для новых экранов используйте `@ScreenController` + `@ScreenView`.
- Старые аннотации оставьте как есть.

Результат: смешанный режим (legacy + facade) поддерживается.

## Шаг 2. Миграция action/text handler-ов

Для выбранного экрана:

1. Перенесите `@OnAction` -> `@OnScreenAction`.
2. Перенесите `@OnText` -> `@OnScreenText`.
3. Оставьте screen id/навигацию неизменными.

После каждого шага прогоняйте screen-flow тесты.

## Шаг 3. Вынести повторяемые части в widget layer

- Создайте `@WidgetController` и `@Widget(id = "...")`.
- Подключайте через `Widgets.ref("...")`.
- Не удаляйте старые `Widget` реализации, пока не закрыт regression scope.

## Шаг 4. Добавить form engine для wizard flow

- Используйте `FormEngine + FsmFormStateStorage` в `ctx.fsm("screen")`.
- Реализуйте переходы `next/back/cancel/submit`.
- Отдельно протестируйте валидацию шага.

## Шаг 5. Переключение callback codec mode

Рекомендуемый путь:

1. production: `LEGACY_STRING`.
2. staging: `TYPED_V1`.
3. проверить callback round-trip + backward fallback.
4. переключить production.

## Deprecation policy

На текущем этапе:

- legacy screen API не помечен как обязательный к удалению;
- принудительных сроков миграции нет;
- breaking removal допускается только после публикации migration plan и release notes.

Рекомендуемая политика на будущие релизы:

1. `N`: добавить новый API, legacy оставить активным.
2. `N+1`: при необходимости добавить `@Deprecated` и migration hints.
3. `N+2` и позже: рассматривать удаление только при подтвержденной готовности экосистемы.

## Чеклист готовности миграции

- Эквивалентный screen-flow работает на facade.
- Повторяемые блоки вынесены в widgets.
- Form-сценарии используют `screen` namespace.
- Callback flow проверен для `LEGACY_STRING` и `TYPED_V1`.
- Документация и тесты обновлены.

## См. также

- [Screen Facade Guide](screen-facade-guide.md)
- [Widget Layer Guide](widget-layer-guide.md)
- [Form Engine Guide](form-engine-guide.md)
- [Screen Callback Codec Modes](screen-callback-codec-modes.md)
- [Screen E2E Scenario](screen-e2e-scenario.md)
