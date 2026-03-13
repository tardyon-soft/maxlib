# FSM And Scenes Contract (Sprint 8)

## Status

Документ фиксирует contract FSM/scenes layer для Sprint 8.
Это спецификация public API и runtime boundaries, а не реализация.

## Sprint 8 Goal

Добавить stateful dialog layer поверх текущего dispatcher/runtime:
- `FSMContext` для управления state/data;
- storage abstraction с явными scope strategy;
- `StateFilter` для routing по текущему state;
- scene lifecycle + wizard-style flows.

## Sprint 8 Boundaries

В scope Sprint 8:
- contracts и базовая runtime integration FSM/scenes;
- storage abstraction (`FSMStorage`) + in-memory implementation baseline;
- scene registry/manager APIs;
- minimal wizard API поверх scene contracts.

Вне scope Sprint 8:
- Spring starter integration;
- distributed workflow engine;
- visual scene designer;
- сложный persistent orchestration layer (saga/BPM/etc).

## Core Contracts

### `FSMContext`

Назначение:
- request-scoped API для работы со state и state-data текущего dialog scope.

Ожидаемый контракт:

```java
public interface FSMContext {
    StateKey scope();

    CompletionStage<Optional<String>> currentState();
    CompletionStage<Void> setState(String state);
    CompletionStage<Void> clearState();

    CompletionStage<StateData> data();
    CompletionStage<Void> setData(StateData data);
    CompletionStage<Void> updateData(Map<String, Object> patch);
    CompletionStage<Void> clearData();
}
```

Границы:
- не знает про transport (polling/webhook);
- не делает routing самостоятельно;
- не управляет scene lifecycle напрямую (это `SceneManager`).
- базовая реализация `StorageBackedFSMContext` делегирует все операции в `FSMStorage`.

### State Representation

Базовые типы:
- state id: `String` (например, `"checkout.email"`);
- optional typed wrappers допустимы позже (`StateId`), но Sprint 8 использует string-first contract;
- data: `Map<String, Object>` как минимальный interoperable payload.

Требования:
- state id должен быть стабильным и предсказуемым для фильтров/scene routing;
- state-data обновляется patch-style, без implicit merge magic.

### Storage Abstraction

Назначение:
- изолировать FSM/scenes runtime от конкретного persistence backend.

Ожидаемый контракт:

```java
public interface FSMStorage {
    CompletionStage<Optional<String>> getState(StateKey key);
    CompletionStage<Void> setState(StateKey key, String state);
    CompletionStage<Void> clearState(StateKey key);

    CompletionStage<StateData> getStateData(StateKey key);
    CompletionStage<Void> setStateData(StateKey key, StateData data);
    CompletionStage<StateData> updateStateData(StateKey key, Map<String, Object> patch);
    CompletionStage<Void> clearStateData(StateKey key);
}
```

Требования:
- clear semantics для отсутствующего state/data;
- deterministic behavior в рамках одного process/runtime;
- backend-specific ошибки маппятся в typed framework exceptions.
- state и state-data хранятся раздельно; scene metadata будет отдельным контрактом (`SceneStorage`)
  и не смешивается с `FSMStorage`.

Baseline implementation Sprint 8:
- `MemoryStorage` (thread-safe in-memory `FSMStorage`) для разработки, тестов и простых runtime сценариев.

### State Scope / Strategy

Назначение:
- определить границы изоляции state.

Базовые стратегии Sprint 8:
- `USER` (state на пользователя независимо от чата);
- `CHAT` (один state на чат);
- `USER_IN_CHAT` (state на пару chat+user, дефолт).

Ожидаемый key contract:

```java
public record StateKey(
    StateScope scope,
    UserId userId,
    ChatId chatId
) {}
```

Границы:
- scope strategy выбирается runtime-конфигурацией;
- strategy не должна протекать в business handlers как low-level storage detail.
- key generation выполняется из incoming `Update` через `StateKeyStrategy`, чтобы
  storage layer и `StateFilter` использовали единый способ вычисления scope key.

### `StateFilter`

Назначение:
- matching update/event по текущему state.

Ожидаемый контракт:

```java
public interface StateFilter extends Filter<Message> {
    static StateFilter equalsTo(String state) { ... }
    static StateFilter in(Set<String> states) { ... }
}
```

Семантика:
- фильтр читает state через `FSMContext` текущего scope;
- `NOT_MATCHED` не считается ошибкой;
- storage/runtime ошибки фильтра идут в runtime error boundary.

### `Scene`

Назначение:
- именованный модуль сценария поверх FSM state-machine contract.

Ожидаемый контракт:

```java
public interface Scene {
    String id();

    CompletionStage<Void> onEnter(SceneContext ctx);
    CompletionStage<Void> onExit(SceneContext ctx);
}
```

Границы:
- scene описывает lifecycle hooks и metadata-bound dialog lifecycle;
- scene не делает transport startup;
- scene не управляет глобальным dispatcher lifecycle.

`SceneContext` содержит:
- `FSMContext` текущего scope;
- `SceneSession` (scene id + entered timestamp).

### `SceneRegistry`

Назначение:
- реестр доступных сцен и lookup by id.

Ожидаемый контракт:

```java
public interface SceneRegistry {
    SceneRegistry register(Scene scene);
    Optional<Scene> find(String sceneId);
    Collection<Scene> all();
}
```

Требования:
- уникальность `scene.id()`;
- deterministic registration order.
- registry не смешивается с router tree и не зависит от Spring scanning.

### `SceneStorage`

Назначение:
- хранить scene metadata отдельно от `FSMStorage`.

Ожидаемый контракт:

```java
public interface SceneStorage {
    CompletionStage<Optional<SceneSession>> get(StateKey key);
    CompletionStage<Void> set(StateKey key, SceneSession session);
    CompletionStage<Void> clear(StateKey key);
}
```

Baseline implementation Sprint 8:
- `MemorySceneStorage` (thread-safe in-memory storage).

### `SceneManager`

Назначение:
- runtime lifecycle сцен в текущем state scope.

Ожидаемый контракт:

```java
public interface SceneManager {
    CompletionStage<Optional<SceneSession>> currentScene();

    CompletionStage<Void> enter(String sceneId);
    CompletionStage<Void> exit();
    CompletionStage<Void> transition(String sceneId);
}
```

Семантика:
- `enter` валидирует scene в registry, создаёт `SceneSession`, сохраняет metadata в `SceneStorage`,
  bind-ит underlying FSM state через `SceneStateBinding`, затем вызывает `onEnter`;
- `transition` вызывает `onExit` current + `onEnter` next;
- `exit` вызывает `onExit` current и очищает scene metadata + bound FSM state.

Baseline implementation Sprint 8:
- `DefaultSceneManager` (`SceneRegistry` + `SceneStorage` + `FSMContext` + `SceneStateBinding`).

### Wizard-style Flow

Назначение:
- минимальный линейный step-by-step API поверх `SceneManager` + `FSMContext`.

Контракт (baseline):

```java
public interface Wizard extends Scene {
    List<WizardStep> steps();
}

public interface WizardManager {
    CompletionStage<Void> enter(String wizardId);
    CompletionStage<Optional<WizardStep>> currentStep();
    CompletionStage<Void> next();
    CompletionStage<Void> back();
    CompletionStage<Void> exit();
}
```

Пример:

```java
Wizard checkout = Wizard.named("checkout")
    .step("email")
    .step("confirm")
    .step("done")
    .build();
```

Семантика:
- step identity задаётся `WizardStep.id`;
- текущий шаг хранится в `FSMContext.data` (`wizard.stepIndex` + `wizard.stepId`);
- `next`/`back` ограничены границами списка шагов (clamp behavior);
- `exit` очищает scene metadata и wizard step metadata.

Границы:
- wizard не является BPM engine;
- без distributed compensation/orchestration;
- без branching graph editor в Sprint 8 baseline.

## Desired Developer Experience

### 1) Установить state

```java
router.message(Command.start(), (Message msg, FSMContext fsm) ->
    fsm.setState("checkout.email")
);
```

### 2) Прочитать state

```java
router.message((Message msg, FSMContext fsm) ->
    fsm.currentState().thenAccept(state -> {
        // use state
    })
);
```

### 3) Обновить state data

```java
fsm.updateData(Map.of("email", "user@example.com"));
```

### 4) Сбросить state

```java
fsm.clearState().thenCompose(v -> fsm.clearData());
```

### 5) Войти в scene

```java
sceneManager.enter("checkout");
```

### 6) Переход к следующему шагу

```java
sceneManager.transition("checkout.confirm");
```

### 7) Выйти из scene

```java
sceneManager.exit();
```

## Integration Requirements

- FSM/scenes layer должен переиспользовать существующие:
  - `Dispatcher`/`Router` runtime pipeline;
  - filter contracts (`StateFilter` как часть filter layer);
  - handler DI/invocation (`FSMContext`, `SceneManager` resolvers).
- Runtime wiring for `FSMContext`:
  - `Dispatcher.withFsmStorage(FSMStorage)` задаёт storage backend;
  - `Dispatcher.withStateScope(StateScope)` или `withStateKeyStrategy(...)` задаёт key strategy;
  - `FSMContext` резолвится в handler parameters через existing Sprint 5 resolver pipeline.
- `StateFilter` работает как обычный `Filter` в существующей filter architecture:
  - участвует в first-match semantics без специальных веток роутинга;
  - композиция `StateFilter.and(...)`/`or(...)` работает на общих правилах filter composition;
  - совместим с outer/inner middleware pipeline.
- Storage abstraction не должен смешиваться с messaging layer.
- FSM/scenes runtime errors идут в существующий runtime error boundary.

## Notes

- State/data model intentionally conservative для предсказуемого API.
- Persistent/distributed storage и сложные orchestration сценарии — отдельные фазы после Sprint 8.
