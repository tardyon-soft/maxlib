# FSM/Scenes Contract Specification

## Status

Документ фиксирует публичный контракт FSM/scenes subsystem для framework MVP.
Это спецификация API и поведения, а не реализация.

## Goals

- Зафиксировать контракт `FSMContext` для работы с состоянием диалога.
- Определить storage abstraction и state scope model.
- Определить `StateFilter` для маршрутизации по состояниям.
- Зафиксировать роли `Scene` и `SceneManager`.
- Описать минимальный wizard-style API для пошаговых сценариев.

## Core concepts

- `State`: текущее логическое состояние диалога (`String`/typed state id).
- `StateData`: key-value данные, связанные с текущим state scope.
- `StateScope`: ключ области, где хранится state (`chat`, `user`, `chat+user`, `thread` и т.д.).
- `Scene`: именованный сценарий поверх базового FSM.

## FSMContext contract

`FSMContext` — request-scoped gateway к текущему FSM scope.

Concept API:

```java
public interface FSMContext {
    Optional<String> state();
    void setState(String state);
    void clearState();

    StateData data();
    void clearData();

    StateScope scope();
}
```

Контракт:
- `setState` меняет только state текущего scope;
- `clearState` не обязан автоматически очищать `data` (явно через `clearData`);
- операции должны быть атомарными в рамках одного update processing шага;
- `FSMContext` доступен в handler DI и scene handlers.

## Storage abstraction contract

Framework хранит состояние через `FSMStorage` abstraction.

Concept API:

```java
public interface FSMStorage {
    Optional<FsmSnapshot> load(StateScope scope);
    void save(StateScope scope, FsmSnapshot snapshot);
    void clear(StateScope scope);
}
```

Где `FsmSnapshot` включает:
- `state` (`Optional<String>`);
- `data` (`Map<String, Object>`);
- version metadata (для optimistic concurrency, post-MVP).

MVP storage expectations:
- memory storage (in-process);
- расширяемость под Redis/JDBC/custom adapters;
- ошибки storage маппятся в typed runtime exceptions.

## State scopes

Scope определяет изоляцию FSM state/data между диалогами.

MVP поддерживаемые стратегии:

1. `CHAT`
- один state на чат.

2. `USER`
- один state на пользователя глобально для бота.

3. `CHAT_USER`
- один state на пару чат+пользователь.

4. `THREAD` (если поддерживается MAX update model)
- state на thread/subconversation.

Конфигурация scope:
- default scope задаётся на уровне dispatcher/runtime;
- route/scene может переопределять scope policy (post-MVP extension).

## StateFilter contract

`StateFilter` — filter для матчинга update по текущему FSM state.

Concept API:

```java
Filter<Context> stateIs(String state);
Filter<Context> stateIn(Set<String> states);
Filter<Context> stateAbsent();
```

DSL entrypoint:

```java
F.stateIs("checkout.waiting_email")
```

Поведение:
- filter читает state текущего `FSMContext.scope()`;
- при storage/read error filter не матчится и передаёт ошибку в dispatcher error policy;
- `stateAbsent()` матчится, если состояние не установлено.

## Scene contract

`Scene` — named composition слой поверх FSM, группирующий handlers и transitions.

Concept API:

```java
public interface Scene {
    String id();
    void onEnter(SceneContext ctx);
    void onExit(SceneContext ctx);
}
```

Scene responsibilities:
- описывает шаги сценария;
- определяет allowed transitions;
- может регистрировать scene-local handlers/filters/middleware.

Scene does not:
- напрямую управлять transport lifecycle;
- заменять dispatcher/router.

## SceneManager contract

`SceneManager` управляет жизненным циклом сцен для текущего scope.

Concept API:

```java
public interface SceneManager {
    Optional<String> currentScene();
    void enter(String sceneId);
    void exit();
    void transition(String nextSceneId);
}
```

Контракт:
- `enter` устанавливает scene marker + стартовый state;
- `exit` очищает scene marker и завершает scene lifecycle;
- `transition` выполняет `onExit(current)` -> `onEnter(next)`;
- scene operations должны быть согласованы с `FSMStorage`.

## Minimal Wizard-style API (MVP)

Wizard — минимальная пошаговая DSL-обёртка поверх Scene + FSM state.

Пример:

```java
Wizard checkout = Wizards.create("checkout")
    .step("email", (ctx, fsm) -> {
        fsm.setState("checkout.email");
        ctx.reply("Введите email");
    })
    .step("confirm", (ctx, fsm) -> {
        fsm.setState("checkout.confirm");
        ctx.reply("Подтвердите заказ");
    })
    .onFinish((ctx, fsm) -> {
        fsm.clearState();
        fsm.clearData();
        ctx.reply("Готово");
    });

router.scene(checkout.scene());
```

State-driven handling пример:

```java
router.message(
    F.stateIs("checkout.email"),
    (MessageContext ctx, FSMContext fsm, SceneManager scenes) -> {
        fsm.data().put("email", ctx.message().text());
        scenes.transition("checkout.confirm");
        ctx.reply("Email сохранён");
    }
);
```

## Lifecycle summary

1. Handler/scene получает `FSMContext` через DI.
2. Reads/writes идут в storage по текущему scope.
3. `StateFilter` использует текущее состояние для маршрутизации.
4. `SceneManager` управляет enter/exit/transition и scene marker.
5. Wizard использует scene + state transitions как high-level API.

## Error handling contract

- Storage ошибки не должны silently игнорироваться.
- Если `setState/save` неуспешен, handler завершается исключением и идёт в общий dispatcher error flow.
- Неконсистентный transition (scene not found, invalid transition) -> typed scene exception.

## Non-goals

- Полный каталог post-MVP scene patterns.
- Internal locking/concurrency details конкретных storage адаптеров.
- Готовая реализация distributed saga/workflow поверх scenes.

## Related docs

- [api-contract.md](api-contract.md)
- [di-model.md](di-model.md)
- [filter-contract.md](filter-contract.md)
- [event-model.md](event-model.md)
- [adr/0001-router-model.md](adr/0001-router-model.md)
