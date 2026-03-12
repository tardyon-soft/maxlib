# Sprint 3 Runtime Examples

Минимальные примеры runtime-слоя без filters/middleware/FSM.

Файлы:
- `DispatcherRouterExample.java` — создание `Dispatcher`/`Router`, регистрация `update/message/callback/error` handler-ов, `includeRouter`, `feedUpdate`.
- `DispatcherIngestionIntegrationExample.java` — интеграция `Dispatcher` с ingestion layer:
  - polling path (`DefaultLongPollingRunner` + `SdkPollingUpdateSource` + `Dispatcher`);
  - webhook path (`DefaultWebhookReceiver` + `Dispatcher`).

Примеры отражают текущий low-level API Sprint 3 и не используют не реализованные runtime features.

