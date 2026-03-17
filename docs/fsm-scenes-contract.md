# FSM and Scenes Contract

## FSMContext

`FSMContext` — scope-bound runtime API для состояния и payload-данных.

Ключевые методы:

- `scope()`
- `currentState()`
- `setState(...)`, `clearState()`
- `data()`, `setData(...)`, `updateData(...)`, `clearData()`
- `snapshot()`
- `clear()`

## FSMStorage

Storage contract (`FSMStorage`) задаёт persistence boundary:

- read/write/clear state
- read/write/merge/clear state data

Базовая реализация: `MemoryStorage`.

## Scope model

Scope определяется `StateKey`/`StateScope` и стратегией в dispatcher:

- `USER`
- `CHAT`
- `USER_IN_CHAT`

## SceneManager

Scene lifecycle API:

- `currentScene()`
- `enter(sceneId)`
- `exit()`
- `transition(sceneId)`

## WizardManager

Wizard API поверх scene/fsm:

- `enter(wizardId)`
- `currentStep()`
- `next()`
- `back()`
- `exit()`

## Runtime availability

`RuntimeContext.fsm()/scenes()/wizard()` доступны только если dispatcher настроен соответствующими `with*` методами.
