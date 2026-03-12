# ADR-0002: Multi-Module Project Structure

- Status: Accepted
- Date: 2026-03-12

## Context

Framework должен развиваться как набор независимых, но согласованных слоёв: model, client, runtime orchestration, integration и test tooling.

Монолитная структура усложняет:
- эволюцию публичного API;
- контроль зависимостей между слоями;
- переиспользование частей framework в разных runtime профилях;
- тестирование и публикацию артефактов.

## Decision

Проект ведётся как Gradle multi-module с Kotlin DSL.

Базовая модульность (MVP baseline):
- `max-model`
- `max-client-core`
- `max-dispatcher`
- `max-fsm`
- `max-spring-boot-starter`
- `max-testkit`

Зависимости на baseline уровне:
- `max-client-core` -> `max-model`
- `max-dispatcher` -> `max-model`, `max-client-core`, `max-fsm`
- `max-spring-boot-starter` -> `max-dispatcher`
- `max-testkit` -> `max-dispatcher`, `max-model`

Планируемое расширение модулей (`max-filters`, `max-middleware` и т.п.) допускается, но только без нарушения текущих границ ответственности.

## Consequences

Плюсы:
- явные архитектурные границы;
- проще поддерживать стабильность public API;
- лучше тестируемость и скорость локальных изменений по модулям;
- удобнее публикация и версионирование.

Минусы:
- дополнительная сложность в build-configuration и управлении зависимостями;
- выше требования к дисциплине модульных границ.

## Alternatives considered

1. Single-module проект:
- отклонено из-за слабой изоляции слоёв и ухудшения долгосрочной масштабируемости.

2. Избыточная микромодульность с самого старта:
- отклонено для MVP как premature complexity.

## Follow-ups

- Выделить отдельные модули для filters/middleware после стабилизации runtime контрактов.
- Добавить архитектурные проверки зависимостей между модулями (build-time rules).

## References

- [../product-spec.md](../product-spec.md)
- [../api-contract.md](../api-contract.md)
