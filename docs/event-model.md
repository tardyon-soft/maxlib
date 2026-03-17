# Event Model

## Normalized update

Framework использует единый DTO `Update` (`max-model`) как transport-agnostic event.

`Update` включает:

- `updateId`
- `type` (`UpdateType`)
- optional payloads: `message`, `callback`
- `timestamp`

## Resolved runtime events

`Dispatcher` через `UpdateEventResolver` маппит `Update` в runtime event type:

- `ResolvedUpdateEventType.MESSAGE`
- `ResolvedUpdateEventType.CALLBACK`
- `UNKNOWN/unsupported` -> ignored

## Observer mapping

- `router.update(...)` всегда получает полный `Update`
- `router.message(...)` получает `Message`
- `router.callback(...)` получает `Callback`
- `router.error(...)` получает `ErrorEvent`

## Ingestion compatibility

Оба источника (polling/webhook) должны приводить данные к одинаковому `Update`, чтобы runtime path был единым.
