# Sprint 2 Low-level Examples

Transport-level примеры без runtime dispatcher слоя.

## Файлы

- `LongPollingExample.java` — `MaxBotClient` + `SdkPollingUpdateSource` + `DefaultLongPollingRunner`.
- `WebhookHandlingExample.java` — `DefaultWebhookReceiver` + secret validation + mapping в HTTP status.

## Актуальность

- Примеры остаются валидными как низкоуровневый путь интеграции.
- Это не отдельный Gradle модуль, а reference snippets.
- Для polling нужен `MAX_BOT_TOKEN`.
