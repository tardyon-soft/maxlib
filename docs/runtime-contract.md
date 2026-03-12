# Runtime Contract (Sprint 3)

## Status

Этот документ фиксирует runtime contracts для Sprint 3:
`Dispatcher`, `Router`, `EventObserver`, `Handler` и базовый dispatch pipeline
поверх ingestion layer из Sprint 2.

Это спецификация контракта, а не полная реализация runtime.

## Sprint 3 boundaries

В scope Sprint 3:
- рабочий dispatch pipeline, принимающий `Update` из unified ingestion pipeline;
- router tree и registration API для handler-ов;
- observer-модель для event-type handlers;
- deterministic first-match handler selection.

Вне scope Sprint 3:
- полноценный filter DSL (будет в Sprint 4);
- middleware runtime chain (будет в Sprint 4);
- FSM/scenes runtime (будет в Sprint 8);
- Spring Boot starter интеграция runtime (будет в Sprint 9).

## Core contracts

### `Dispatcher`

Назначение:
- корневой runtime orchestrator для dispatch процесса;
- integration point между ingestion layer (`UpdatePipeline`/`UpdateConsumer`) и router graph.

Ответственность:
- хранить root-router set;
- принимать normalized `Update` из ingestion;
- делегировать обработку в router tree;
- возвращать `DispatchResult`.

Не отвечает за:
- polling/webhook transport loop (это Sprint 2 ingestion layer);
- HTTP/MAX API transport;
- middleware/filter/FSM runtime на этом этапе.

### `Router`

Назначение:
- модульная единица регистрации handlers и дочерних router-ов.

Ответственность:
- содержать observer-ы по поддержанным event types;
- регистрировать handlers в observer-ах;
- поддерживать `includeRouter(child)` для построения router tree.

Не отвечает за:
- lifecycle polling/webhook;
- transport deserialization;
- выбор источника update.

### `EventObserver`

Назначение:
- registry + resolution слой для handlers одного event-type.

Ответственность:
- хранить handlers в порядке регистрации;
- выполнять matching policy observer-а;
- возвращать выбранный handler (или отсутствие match).

MVP observer types в Sprint 3:
- `message` observer;
- `callback` observer.

MVP event mapping:
- update с message payload -> `message` observer;
- update с callback payload -> `callback` observer;
- остальные update types в MVP Sprint 3 считаются unsupported и дают `DispatchResult.IGNORED`.

### `Handler`

Назначение:
- единая функция обработки события observer-уровня.

Контракт:
- handler принимает typed runtime context/event envelope;
- handler может быть async (`CompletionStage`) или sync (оборачивается runtime-слоем);
- handler не должен знать transport детали polling/webhook.

### `HandlerExecution`

Назначение:
- internal execution contract одного выбранного handler-а.

Ответственность:
- выполнить handler;
- нормализовать sync/async выполнение;
- отдать typed execution outcome (`handled`/`failed`).

Примечание:
- `HandlerExecution` может быть отдельным типом или internal helper, но семантика должна быть явно закреплена.

### `DispatchResult`

Назначение:
- результат dispatch-итерации для одного `Update`.

MVP outcomes:
- `HANDLED` — найден handler и выполнен успешно;
- `IGNORED` — update type/observer не поддержан или handler не найден;
- `FAILED` — ошибка во время handler execution.

## Dispatcher ↔ Router relation

- `Dispatcher` владеет списком root-router-ов.
- Каждый root-router может включать child-router-ы (`includeRouter`) и формирует дерево.
- На каждый update dispatcher проходит router tree в deterministic порядке:
1. root routers в порядке подключения;
2. внутри router — observer для mapped event type;
3. handlers observer-а в порядке регистрации;
4. child routers в порядке include.
- Dispatch останавливается на первом успешно matched handler-е (first-match behavior).

## Router tree model

- Router tree направлен от root к child через `includeRouter`.
- Один и тот же router instance не должен включаться рекурсивно в собственную ветку.
- Порядок include влияет на итоговый resolution order и считается частью публичной семантики.

## First-match behavior

- Matching policy: первый handler, удовлетворяющий observer matching rules, выигрывает.
- После `HANDLED` дальнейший поиск по текущему observer и downstream router-ам прекращается.
- Если handler завершился `FAILED`, результат dispatch = `FAILED`; fallback к следующему handler в Sprint 3 не делается.
- Если ни один handler не matched, результат = `IGNORED`.

Практическая propagation-семантика Sprint 3:
- обход начинается с root router-ов в порядке `Dispatcher.includeRouter(...)`;
- внутри root используется DFS pre-order router tree (`root -> children in include order`);
- для каждого router сначала пробуется generic `update` observer, затем resolved typed observer (`message`/`callback`);
- первый `HANDLED` останавливает весь dispatch pipeline;
- первый `FAILED` завершает pipeline как `FAILED` (после уведомления `error` observer текущего router).

## Runtime pipeline over Sprint 2 ingestion

Целевой flow Sprint 3:
1. polling/webhook ingestion доставляет normalized `Update` в unified `UpdatePipeline`;
2. pipeline делегирует update в runtime consumer (dispatcher adapter);
3. dispatcher выполняет routing и handler execution;
4. runtime outcome маппится обратно в ingestion result boundary.

Требование:
- polling и webhook обязаны приходить в один и тот же dispatch pipeline без расхождения внутренней логики.

## Runtime error boundary

Dispatch-layer runtime ошибки в Sprint 3:
- `HANDLER_FAILURE` — исключение/failed completion в `update`/`message`/`callback` handler;
- `EVENT_MAPPING_FAILURE` — ошибка при `Update -> observer/event` resolution;
- `OBSERVER_EXECUTION_FAILURE` — ошибка во время вызова observer execution contract.

Propagation model:
- runtime error пробрасывается в `error` observer текущего router как `ErrorEvent(update, error, type)`;
- dispatch result остаётся `FAILED` даже если `error` handler выполнился успешно;
- если `error` handler отсутствует, ошибка остаётся `FAILED` без дополнительной обработки;
- если `error` handler сам падает, его ошибка добавляется в `suppressed` к исходной runtime ошибке.

Boundary с ingestion:
- ingestion-level transport ошибки (polling/webhook parse/IO/secret validation) остаются в ingestion layer;
- runtime dispatch ошибки (routing/handler/event resolution) остаются в dispatcher layer;
- `Dispatcher` адаптируется к ingestion через `UpdateConsumer.handle(Update)` без смешивания transport-ошибок и runtime-ошибок.
