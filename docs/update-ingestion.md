# Update Ingestion

## Unified model

Ingestion слой приводит транспортные данные к `Update` и отправляет в единый runtime pipeline.

## Key contracts

- `UpdateConsumer` / `UpdateSink`
- `UpdateSource`
- `PollingUpdateSource`
- `WebhookReceiver`
- `UpdatePipeline`

## Polling flow

1. `PollingUpdateSource.poll(...)`
2. batch updates
3. `Dispatcher.handle(update)` для каждого update

## Webhook flow

1. `WebhookReceiver.receive(request)`
2. optional secret validation (`X-Max-Bot-Api-Secret`)
3. decode payload -> `Update`
4. `Dispatcher.handle(update)`

## Result model

- ingestion APIs возвращают typed status/result objects (`UpdateHandlingResult`, `WebhookReceiveResult`, etc.).
