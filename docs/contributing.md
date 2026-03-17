# Contributing

## Workflow

1. Создайте branch с префиксом `codex/` или feature branch в вашем процессе.
2. Внесите изменения модульно (runtime/client/starter/testkit/docs).
3. Добавьте/обновите тесты рядом с изменениями.
4. Прогоните таргетные Gradle задачи.
5. Обновите docs, если меняется public API/behavior.

## Engineering rules

- Не ломать backward-compatible API без явной миграции.
- Не смешивать transport/client логику с runtime orchestration.
- Для поиска по проекту использовать `rg`.
- Предпочитать минимальные, проверяемые change sets.

## Testing expectations

Минимум:

- unit tests для нового поведения
- integration test для runtime/starter wiring (если меняется wiring)
- testkit-based scenario для user-facing runtime behavior

## Documentation expectations

При изменениях API обновлять релевантные документы в `docs/` и examples/demo.
