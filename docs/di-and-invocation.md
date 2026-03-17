# DI and Invocation

## Components

- `ReflectiveEventHandler`
- `HandlerInvoker` (`DefaultHandlerInvoker`)
- `ResolverRegistry`
- `HandlerParameterResolver`

## Default resolvers

По умолчанию подключаются resolvers для:

- runtime context (`RuntimeContext`)
- FSM/scenes/wizard (`FSMContext`, `SceneManager`, `WizardManager`)
- messaging abstractions
- update/message/callback/user/chat
- filter/middleware enrichment
- application data/services

## Invocation flow

1. `ReflectiveEventHandler` получает `event + RuntimeContext`
2. `DefaultHandlerInvoker` кеширует metadata метода
3. Каждый параметр резолвится через `ResolverRegistry`
4. `Method.invoke(...)`
5. Return value нормализуется в `CompletionStage<Void>`

## Failure taxonomy

- parameter missing/unsupported
- ambiguous resolution
- resolver execution failure
- reflective invocation failure
- invalid return type

Все ошибки маршрутизируются в dispatcher error flow.
