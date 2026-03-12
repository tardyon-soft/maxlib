# Project Roadmap

## Scope

High-level roadmap проекта MAX Java Bot Framework по спринтам.

## Current Status

- Sprint 2 (`Polling/Webhook ingestion`) завершён.
- Sprint 3 (`Dispatcher/Router runtime foundation`) завершён.
- Sprint 4 (`Filters/Middleware/context enrichment`) завершён.
- Sprint 5 (`DI / handler parameter resolution`) завершён.
- Следующий этап: Sprint 6 (`Messages/Keyboards/Callbacks ergonomics`).
- Sprint 6.1: contract freeze для high-level messaging API (`docs/messaging-api.md`).

## Sprint 0 — Spec/API Contract Freeze

Цель:
- Зафиксировать product spec, API contracts, ADR baseline и правила contribution.

Основные результаты:
- `product-spec`, `api-contract`, профильные contracts (filters/middleware/DI/message/callback/upload/FSM);
- ADR по модульности, router model, client/runtime separation, unified update pipeline;
- naming/package strategy и contributing workflow.

## Sprint 1 — Client/DTO/Errors

Цель:
- Реализовать базовый `max-client-core` и `max-model` для работы с MAX API.

Основные результаты:
- DTO/enums для MVP surface;
- HTTP client abstractions;
- типизированная модель ошибок (`validation`, `api`, `transport`);
- базовые client tests.

## Sprint 2 — Polling/Webhook

Цель:
- Реализовать transport adapters для polling и webhook с общим ingress контрактом.

Основные результаты:
- polling runtime foundation (`SdkPollingUpdateSource`, `DefaultLongPollingRunner`);
- webhook runtime entry foundation (`DefaultWebhookReceiver`, secret validation);
- normalize raw updates -> `Update` + typed transport results;
- unified ingestion pipeline для polling и webhook;
- lifecycle/shutdown semantics + lightweight overload control;
- integration-style fixtures/tests для regression safety.

## Sprint 3 — Dispatcher/Router

Цель:
- Реализовать core orchestration слой `Dispatcher/Router`.

Основные результаты:
- router graph + `includeRouter` semantics;
- observer registration (`message`, `callback`);
- deterministic handler resolution (first-match);
- runtime error boundary + `error` observer;
- ingestion integration through `UpdateConsumer` (`Dispatcher` as ingestion target);
- расширенные routing/integration tests.

## Sprint 4 — Filters/Middleware

Цель:
- Реализовать filter DSL и middleware pipeline.

Основные результаты:
- built-in MVP filters;
- `and/or/not` композиция;
- outer/inner middleware execution order;
- request-scoped context enrichment from filters and middleware;
- runtime error policy для `filter/middleware/enrichment/handler`;
- unit + integration-style regression safety net;
- usage examples for filter/middleware runtime API.

## Sprint 5 — DI

Цель:
- Ввести handler argument resolution model в runtime.

Основные результаты:
- invocation contract (`HandlerInvoker`, `HandlerParameterResolver`, `ResolverRegistry`);
- injection sources: context/update objects, filter data, middleware data, shared application services;
- shared registration APIs: `Dispatcher.registerApplicationData(...)`, `Dispatcher.registerService(...)`;
- диагностируемые resolution/invocation errors (`PARAMETER_RESOLUTION_FAILURE`, `INVOCATION_FAILURE`);
- usage examples + DI regression safety net (unit + integration-style tests).

## Sprint 6 — Messages/Keyboards/Callbacks

Цель:
- Реализовать high-level API для message/callback операций.

Основные результаты:
- builder-style send/edit/delete/reply;
- keyboard builders + buttons;
- callback handler API + answer abstraction;
- platform-aware validation для message/callback payload.

## Sprint 7 — Upload/Media

Цель:
- Реализовать upload/media abstractions с сокрытием MAX multi-step upload flow.

Основные результаты:
- `UploadInput` forms (`path`, `bytes`, `stream`, `resource`, `existing ref`);
- high-level media builders;
- hidden `prepare/transfer/finalize/send` orchestration;
- upload/media error model и тесты.

## Sprint 8 — FSM/Scenes

Цель:
- Реализовать stateful dialog subsystem.

Основные результаты:
- `FSMContext` + storage abstraction;
- state scopes + `StateFilter`;
- `Scene` + `SceneManager` lifecycle;
- minimal wizard-style API.

## Sprint 9 — Starter/Testkit/Docs Polishing

Цель:
- Довести framework до production-ready DX уровня.

Основные результаты:
- Spring Boot starter auto-configuration;
- testkit для unit/integration сценариев;
- examples и docs polishing;
- финальная проверка consistency между кодом, contracts и README.

## Notes

- Приоритет сохраняется: архитектура и стабильность API важнее расширения surface area.
- Каждый спринт может быть разбит на несколько атомарных задач и коммитов.
- Если MAX API ограничения блокируют фичу, это фиксируется в docs как explicit limitation.
