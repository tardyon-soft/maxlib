# MAX Java Bot Framework

Java framework для разработки ботов на платформе MAX с DX, вдохновлённым aiogram 3, но адаптированным под Java и реальные возможности MAX API.

## Цель проекта

Сделать production-ready framework, где разработчик пишет бизнес-логику бота, а не низкоуровневый JSON/HTTP код.

Ключевые принципы:
- модульная и расширяемая архитектура;
- разделение client SDK и runtime framework;
- типобезопасный и тестируемый API;
- единый update pipeline для polling и webhook;
- честная адаптация под MAX API без имитации несуществующих платформенных возможностей.

## Источники истины

- MAX API docs: [dev.max.ru/docs-api](https://dev.max.ru/docs-api)
- MAX methods: [dev.max.ru/docs-api/methods](https://dev.max.ru/docs-api/methods)
- MAX objects: [dev.max.ru/docs-api/objects](https://dev.max.ru/docs-api/objects)
- aiogram 3 reference: [docs.aiogram.dev](https://docs.aiogram.dev/)

## Текущий статус

Статус: `bootstrap` (инициализация репозитория и стартовой multi-module структуры).

Реализовано:
- Gradle multi-module проект (Kotlin DSL) с 6 стартовыми модулями;
- Java toolchain 21 на уровне всех модулей;
- базовые модульные границы и минимальные public contracts;
- межмодульные зависимости на базовом уровне, проект собирается;
- JUnit 5 конфигурация для тестов;
- Gradle Wrapper, `.gitignore`, стартовая документация.

Пока не реализовано (следующие этапы):
- полноценный MAX HTTP client и DTO surface;
- dispatcher/router observers, filters DSL, middleware chain;
- context DI, FSM/scenes;
- webhook/polling runtime;
- Spring Boot starter auto-configuration;
- testkit для end-to-end сценариев.

## План модульности

Текущая стартовая модульность:
- `max-model` — DTO/enums и унифицированные model-контракты;
- `max-client-core` — HTTP/client abstractions и API вызовы MAX;
- `max-dispatcher` — dispatcher, router и orchestration update pipeline;
- `max-fsm` — FSM storage abstractions и scenes/wizard primitives;
- `max-spring-boot-starter` — Spring Boot интеграция и автоконфигурация;
- `max-testkit` — инструменты тестирования framework-level сценариев.

План следующего расширения модульности:
- выделить `max-filters` и `max-middleware` в отдельные модули после стабилизации dispatcher pipeline.

## Технологический стек

- Java 21
- Gradle (Kotlin DSL)
- JUnit 5

## Быстрый старт

Требования:
- JDK 21+

Команды:

```bash
./gradlew clean build
```

Для запуска конкретного модуля:

```bash
./gradlew :max-dispatcher:test
```

## Roadmap (укрупнённо)

1. HTTP client / DTO / errors
2. polling + webhook transport
3. dispatcher / router / observers
4. filters + middleware
5. context DI
6. messages / keyboards / callbacks
7. upload/media pipeline
8. FSM / scenes
9. Spring Boot starter
10. testkit / examples / docs polishing

## Ограничения и TODO

- Все возможности должны валидироваться по официальной MAX документации перед реализацией.
- Если по MAX API есть неясности, они будут фиксироваться как `TODO/NOTE` в коде и документации.
