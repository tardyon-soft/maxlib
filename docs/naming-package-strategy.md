# Naming and Package Strategy

## Base packages

- `ru.tardyon.botframework.model`
- `ru.tardyon.botframework.client`
- `ru.tardyon.botframework.dispatcher`
- `ru.tardyon.botframework.fsm`
- `ru.tardyon.botframework.message`
- `ru.tardyon.botframework.callback`
- `ru.tardyon.botframework.upload`
- `ru.tardyon.botframework.spring.*`

## Naming principles

- Public contracts: короткие, предметные имена (`Dispatcher`, `Router`, `Filter`, `RuntimeContext`).
- Runtime internals: префикс `Default*` для базовых реализаций (`DefaultHandlerInvoker`, `DefaultEventObserver`).
- Facades для user-facing operations (`MessagingFacade`, `CallbackFacade`, `ChatActionsFacade`).
- Suffix `*Context` для request-scoped/runtime-scoped abstractions (`RuntimeContext`, `FSMContext`).

## Annotation package

Annotation sugar расположен отдельно:

- `ru.tardyon.botframework.dispatcher.annotation.*`

Это подчёркивает, что аннотации — additive layer поверх core runtime.
