# Product Spec (Current)

## Vision

Сделать практичный Java framework для MAX ботов с явным typed API, предсказуемым runtime и удобной интеграцией в Spring/vanilla проекты.

## Non-goals

- не копировать 1:1 aiogram DSL;
- не скрывать transport ограничения MAX под «магией»;
- не делать compile-time codegen mandatory path.

## Core user stories

1. Я хочу быстро поднять polling/webhook bot в Spring.
2. Я хочу писать handlers в явном `Router` API.
3. Я хочу optional annotation sugar без поломки старого кода.
4. Я хочу stateful flows (FSM/scenes/wizard).
5. Я хочу тестировать handlers без реального MAX API.

## Functional scope

- typed client + model
- unified ingestion
- dispatcher/router
- filters + middleware
- reflective DI invocation
- messaging/callback/actions/media
- FSM/scenes/wizard
- spring starter + testkit

## Compatibility policy

- additive evolution: новый sugar добавляется поверх существующего API;
- базовые runtime contracts (`Dispatcher`, `Router`, `Filter`, `RuntimeContext`) остаются стабильными;
- breaking changes требуют migration notes.
