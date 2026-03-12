# ADR-0004: Unified Update Pipeline for Polling and Webhook

- Status: Accepted
- Date: 2026-03-12

## Context

Framework поддерживает два ingress канала: polling и webhook.

Если каждый transport будет иметь отдельную логику routing/middleware/handlers, возникнут:
- расхождения поведения;
- дублирование кода;
- нестабильность DX и тестов.

## Decision

Вводим единый update pipeline:

1. Transport adapter (polling/webhook) получает raw update.
2. Normalizer преобразует raw data в normalized `Update`.
3. Dispatcher запускает общий pipeline (outer middleware -> router resolution -> filters -> inner middleware -> handler -> error policy).
4. Результат обработки возвращается transport adapter-у для acknowledgment/retry semantics.

Обязательное правило:
- polling и webhook обязаны использовать один и тот же dispatcher entrypoint и одну и ту же routing semantics.

## Consequences

Плюсы:
- одинаковое поведение бизнес-логики независимо от transport;
- снижение дублирования и упрощение поддержки;
- единая точка observability/error handling.

Минусы:
- нужен слой нормализации update форматов;
- специфические transport оптимизации должны делаться без ломки общего pipeline.

## Alternatives considered

1. Раздельные pipeline для polling и webhook:
- отклонено из-за расхождения semantics и роста сложности.

2. Общий pipeline только на уровне handlers без общей middleware/router логики:
- отклонено, так как не гарантирует одинаковое поведение всего runtime.

## Follow-ups

- Зафиксировать детальные acknowledgment/retry политики по transport в runtime spec.
- Добавить testkit сценарии эквивалентности polling vs webhook.

## References

- [../event-model.md](../event-model.md)
- [0001-router-model.md](0001-router-model.md)
- [../middleware-contract.md](../middleware-contract.md)
