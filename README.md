# MAX Java Bot Framework

Java framework для разработки ботов на платформе MAX с DX в стиле aiogram 3.

Важно: проект вдохновлён архитектурными паттернами aiogram 3, но не является его буквальной копией. Мы адаптируем идеи под реальные возможности MAX API и Java ecosystem.

## Product vision

Сделать production-ready framework, в котором разработчик пишет бизнес-логику бота, а не низкоуровневый JSON/HTTP код.

## Goals

- Дать удобный framework-level API для построения ботов на MAX.
- Объединить polling и webhook в единый update pipeline.
- Предоставить расширяемую архитектуру: dispatcher, router, filters, middleware, DI, FSM/scenes.
- Сохранить типобезопасность и тестируемость как базовые свойства API.
- Разделить client SDK и runtime framework по модулям.

## Non-goals

- Не делать 1:1 порт aiogram 3 на Java.
- Не выдумывать возможности, которых нет в MAX API.
- Не ограничиваться ролью "тонкого HTTP-клиента" без runtime-слоя.
- Не жертвовать архитектурой ради быстрого увеличения surface area.

## MVP

- Базовый MAX client-core (транспорт, ошибки, базовые методы).
- Unified update ingestion: polling + webhook в общий dispatcher pipeline.
- Dispatcher + Router + базовые observer hooks.
- Минимальный filter/middleware слой для маршрутизации и cross-cutting логики.
- Базовые message/callback handling сценарии.
- Рабочий testkit для unit/integration сценариев handler-логики.

## V1

- Зрелый filters DSL и middleware chain (outer/inner).
- Context-based dependency injection для handler-параметров.
- FSM storage abstraction + scenes/wizard.
- Message/attachment builders и keyboard builders.
- Upload abstraction для многошаговых MAX upload flow.
- Spring Boot starter с автоконфигурацией и runtime wiring.
- Документация и примеры production-oriented сценариев.

## Later

- Расширение покрываемых MAX API surface областей.
- Дополнительные storage/adapters реализации для FSM и runtime.
- Набор observability и reliability extensions (metrics, tracing, retries).
- Больше готовых интеграций и эталонных sample apps.

## Design principles

- API-first DX: приоритет удобства публичного API framework.
- Honest platform mapping: только реальные MAX возможности.
- Layered architecture: client SDK отделён от framework runtime.
- Type safety by default: явные и предсказуемые контракты.
- Extensibility: новые слои строятся поверх существующих абстракций.
- Testability: нетривиальная логика покрывается тестами.
- Documentation as contract: README/docs синхронизированы с текущим состоянием.

## Source of truth

- MAX API docs: [dev.max.ru/docs-api](https://dev.max.ru/docs-api)
- MAX methods: [dev.max.ru/docs-api/methods](https://dev.max.ru/docs-api/methods)
- MAX objects: [dev.max.ru/docs-api/objects](https://dev.max.ru/docs-api/objects)
- aiogram 3 reference (DX patterns): [docs.aiogram.dev](https://docs.aiogram.dev/)

## Documentation

- Product API specification: [docs/product-spec.md](docs/product-spec.md)
- Core API contract: [docs/api-contract.md](docs/api-contract.md)
- Filters contract specification: [docs/filter-contract.md](docs/filter-contract.md)
- Middleware contract specification: [docs/middleware-contract.md](docs/middleware-contract.md)
- Dependency injection model: [docs/di-model.md](docs/di-model.md)
- Message API contract: [docs/message-api-contract.md](docs/message-api-contract.md)
- Upload/media contract: [docs/upload-media-contract.md](docs/upload-media-contract.md)
- FSM/scenes contract: [docs/fsm-scenes-contract.md](docs/fsm-scenes-contract.md)
- Callback contract: [docs/callback-contract.md](docs/callback-contract.md)
- Event model specification: [docs/event-model.md](docs/event-model.md)
- Naming and package strategy: [docs/naming-package-strategy.md](docs/naming-package-strategy.md)
- Contributing workflow: [docs/contributing.md](docs/contributing.md)
- Sprint roadmap: [docs/roadmap.md](docs/roadmap.md)
- Architecture ADR index: [docs/adr/README.md](docs/adr/README.md)
- ADR-0001 Dispatcher/Router model: [docs/adr/0001-router-model.md](docs/adr/0001-router-model.md)
- ADR-0002 Multi-module structure: [docs/adr/0002-multi-module-structure.md](docs/adr/0002-multi-module-structure.md)
- ADR-0003 Client/runtime separation: [docs/adr/0003-client-runtime-separation.md](docs/adr/0003-client-runtime-separation.md)
- ADR-0004 Unified polling/webhook pipeline: [docs/adr/0004-unified-update-pipeline.md](docs/adr/0004-unified-update-pipeline.md)

## Current status

Статус: `bootstrap`.

Сейчас в репозитории:
- стартовая multi-module структура Gradle (Kotlin DSL);
- Java toolchain 21;
- базовые межмодульные зависимости;
- первичные скелеты контрактов;
- `max-client-core` foundation layer: `MaxBotClient`, `MaxHttpClient`, `MaxRequest<T>`, `MaxApiClientConfig`;
- builder-style конфигурация client SDK: `baseUrl`, `token`, `timeouts`, `user-agent`, `retry policy` (placeholder);
- базовый HTTP transport layer в `max-client-core` (GET/POST/PUT/PATCH/DELETE + JSON request/response pipeline), отделённый от domain-level `MaxBotClient`/`MaxRequest<T>` API;
- auth layer в `max-client-core`: автоматический `Authorization` header через отдельный interceptor component;
- Gradle Wrapper и базовая тестовая конфигурация.

## Module plan (current)

- `max-model` — базовые DTO/enums контракты.
- `max-client-core` — Java SDK слой поверх MAX API (HTTP transport, auth, serialization, errors, pagination).
- `max-dispatcher` — dispatcher/router orchestration runtime.
- `max-fsm` — state management abstractions для диалогов.
- `max-spring-boot-starter` — Spring Boot integration слой.
- `max-testkit` — тестовые утилиты для framework-level сценариев.

## Client Config Example

```java
MaxApiClientConfig config = MaxApiClientConfig.builder()
    .baseUrl("https://api.max.ru")
    .token("YOUR_BOT_TOKEN")
    .connectTimeout(Duration.ofSeconds(5))
    .readTimeout(Duration.ofSeconds(30))
    .userAgent("my-max-bot/1.0")
    .retryPolicy(RetryPolicy.none()) // placeholder for future retry behavior
    .build();
```

## Build

Требования:
- JDK 21+

Команда:

```bash
./gradlew clean build
```
