# Sprint 3 Runtime Examples

Минимальные примеры базового runtime слоя (`Dispatcher`/`Router`) без screen/FSM DSL.

## Файлы

- `DispatcherRouterExample.java` — регистрация handlers (`update/message/callback/error`), `includeRouter`, `feedUpdate`.
- `DispatcherIngestionIntegrationExample.java` — связка runtime с ingestion:
  - polling: `DefaultLongPollingRunner` + `SdkPollingUpdateSource` + `Dispatcher`;
  - webhook: `DefaultWebhookReceiver` + `Dispatcher`.

## Актуальность

- Примеры показывают фундаментальный API, который остается стабильным.
- Аннотационный API и screen API в этих примерах намеренно не используются.
