# ADR-0003: Separation of Client SDK and Framework Runtime

- Status: Accepted
- Date: 2026-03-12

## Context

Проект должен одновременно предоставлять:
- client SDK для работы с MAX API;
- framework runtime для routing/middleware/DI/FSM.

Смешивание этих слоёв в одном API приводит к:
- утечке transport деталей в handler-код;
- усложнению тестирования runtime без реального API;
- слабой переиспользуемости client SDK отдельно от framework.

## Decision

Разделяем слои на контрактном уровне:

1. Client SDK layer (`max-client-core`)
- отвечает за MAX API requests/responses/errors/upload transport;
- не зависит от Dispatcher/Router/FSM runtime.

2. Framework runtime layer (`max-dispatcher`, `max-fsm`)
- отвечает за update pipeline, routing, filters/middleware (по контрактам), DI и сценные модели;
- использует client layer через абстракции (`MaxBotClient`, `Bot`).

3. Integration layer (`max-spring-boot-starter`)
- wiring runtime + client + app services;
- не меняет контрактные границы между client и runtime.

Правило зависимости:
- runtime может зависеть от client contracts;
- client не зависит от runtime.

## Consequences

Плюсы:
- чистая архитектура слоёв;
- тестирование runtime возможно через client stubs/testkit;
- проще поддерживать стабильный и предсказуемый public API.

Минусы:
- требуется поддержка нескольких наборов абстракций;
- интеграционный слой становится критичным для DX.

## Alternatives considered

1. Единый API без чёткой границы client/runtime:
- отклонено из-за риска архитектурной эрозии и роста связанности.

2. Полная изоляция runtime от client с transport plugins only:
- отклонено как избыточно сложно для MVP.

## Follow-ups

- Поддерживать строгие dependency rules между модулями.
- Добавить архитектурные тесты, проверяющие отсутствие runtime-зависимостей в client modules.

## References

- [../api-contract.md](../api-contract.md)
- [../di-model.md](../di-model.md)
- [../message-api-contract.md](../message-api-contract.md)
