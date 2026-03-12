# Event Model Specification

## Status

Этот документ фиксирует event model framework для MVP: поддерживаемые update types, mapping на router observers и единый pipeline обработки.

Это спецификация контракта, а не реализация.

## Цели event model

- Нормализовать входящие события MAX в единый `Update` формат.
- Обеспечить одинаковое поведение runtime для polling и webhook.
- Зафиксировать предсказуемую маршрутизацию `Update -> observer -> handler`.

## Normalized Update (concept)

```java
public interface Update {
    UpdateType type();
    Object payload();
    UpdateMeta meta();
}
```

`UpdateMeta` содержит транспортные и технические данные (update id, timestamp, source type, retry hint и т.д.).

## Supported Update Types (MVP)

Для MVP intentionally поддерживается узкий набор update типов:

1. `MESSAGE`
- Входящее сообщение пользователя/чата.
- Включает text и поддерживаемые для MVP message payload варианты.

2. `CALLBACK`
- Событие callback от нажатия кнопки с `callbackData`.

3. `UNKNOWN`
- Raw update, который не сопоставлен с поддерживаемым типом.
- Не приводит к handler execution, но должен логироваться для последующего расширения поддержки.

## Update -> Router Observers mapping (MVP)

| UpdateType | Router observer | Handler context |
|---|---|---|
| `MESSAGE` | `router.message(...)` | `MessageContext` |
| `CALLBACK` | `router.callback(...)` | `CallbackContext` |
| `UNKNOWN` | не маршрутизируется в business handlers | `UpdateMeta` only (for logging/metrics) |

Требования к mapping:
- Один `Update` матчится только в observer своего типа.
- Дальнейший отбор делает filter chain внутри observer.
- Если filter не матчится, handler не вызывается.

## Unified Pipeline for Polling and Webhook

### Ingress paths

1. Polling path:
- Polling adapter получает batch raw updates из MAX API.
- Каждый raw update нормализуется в `Update`.
- Далее передаётся в общий dispatcher pipeline.

2. Webhook path:
- Webhook adapter получает raw update из HTTP request.
- Raw update нормализуется в `Update`.
- Далее передаётся в общий dispatcher pipeline.

### Common processing pipeline

1. `Transport Adapter` (polling/webhook) получает raw update.
2. `Update Decoder/Normalizer` строит normalized `Update`.
3. `Dispatcher` запускает outer middleware chain.
4. `Router resolver` выбирает observer по `UpdateType`.
5. `Filters` observer-а выполняются в registration order.
6. `Inner middleware` выполняется вокруг matched handler.
7. `Handler` исполняется с context injection.
8. `Error boundary` обрабатывает исключения через dispatcher policy.
9. `Post-processing` пишет metrics/logging и завершает transport acknowledgment.

## Transport equivalence contract

- Polling и webhook обязаны использовать один и тот же dispatcher pipeline.
- Различия transport не должны менять semantics filter/middleware/handler execution.
- Бизнес-код handler-ов не должен зависеть от источника события (polling/webhook).

## Acknowledgment rules (MVP)

- `MESSAGE`/`CALLBACK` считаются обработанными после завершения handler pipeline (включая middleware).
- `UNKNOWN` подтверждается без handler execution после logging/metrics.
- Точная стратегия retry/backoff — ответственность transport adapter и будет детализирована в runtime spec.

## Ordering and idempotency expectations (MVP)

- Порядок updates сохраняется в рамках одного transport stream насколько это гарантирует MAX API.
- Framework не должен дублировать handler execution для одного и того же normalized update id в пределах одной delivery attempt.
- Более строгие idempotency guarantees (cross-restart/store-backed) относятся к post-MVP runtime policy.

## Out of scope

- Полный каталог всех MAX update объектов.
- Внутренние transport-specific классы и threading model.
- Конкретная реализация dedup storage.

## Related docs

- Product spec: [product-spec.md](product-spec.md)
- API contract: [api-contract.md](api-contract.md)
