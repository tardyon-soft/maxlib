# Sprint 2 Low-level Examples

Эти примеры показывают transport-level использование Sprint 2 до появления Dispatcher.

## Files

- `LongPollingExample.java` — `MaxBotClient` + `SdkPollingUpdateSource` + `DefaultLongPollingRunner`
- `WebhookHandlingExample.java` — `DefaultWebhookReceiver` + secret validation + result -> HTTP status mapping

## Notes

- Это иллюстративные low-level examples, они не подключены как отдельный Gradle module.
- Перед запуском polling примера задайте `MAX_BOT_TOKEN`.
