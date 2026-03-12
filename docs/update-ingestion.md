# Update Ingestion Contract (Sprint 2)

## Status

Этот документ фиксирует contract ingestion layer для Sprint 2.
Это спецификация транспорта получения `Update` и единой точки входа в дальнейший runtime pipeline.

## Sprint 2 scope boundaries

В scope Sprint 2 входит:
- polling transport layer (`getUpdates`-based ingestion);
- webhook transport layer (HTTP ingress для update payload);
- unified ingestion flow для обоих источников.

Вне scope Sprint 2:
- полноценный `Dispatcher`/`Router` runtime;
- middleware runtime chain;
- FSM/scenes runtime.

## Core contracts

### `UpdateSource`

Назначение:
- абстракция источника `Update` событий;
- lifecycle управления источником (`start`/`stop`);
- доставка normalized `Update` в единый sink.

Граница:
- не решает routing/handler выбор;
- не реализует бизнес-логику.

### `PollingUpdateSource`

Назначение:
- источник `Update`, читающий события через MAX long polling (`getUpdates`);
- инкапсулирует transport-level polling параметры (`marker`, `timeout`, `limit`, `types`);
- выполняет один pull-вызов и возвращает batch normalized update.

Граница:
- не содержит lifecycle loop (это ответственность `LongPollingRunner`);
- не содержит dispatcher/runtime orchestration.

Контракт (минимальный):
- `PollingBatch poll(PollingFetchRequest request)`.
- `PollingBatch` содержит `updates` и `nextMarker` для следующего polling шага.

### `WebhookUpdateSource`

Назначение:
- источник `Update`, получающий события из webhook ingress;
- приводит входящий webhook payload к normalized `Update` и отправляет его в sink.

Граница:
- не определяет HTTP server stack (Spring, Netty и т.п.);
- не отвечает за handler execution.

### `WebhookRequest`

Назначение:
- framework-agnostic модель raw webhook request;
- переносит `body` и `headers` без привязки к servlet/spring API.

### `WebhookSecretValidator`

Назначение:
- проверка секрета из заголовка `X-Max-Bot-Api-Secret` до передачи update в sink.

Контракт (минимальный):
- `WebhookSecretValidationResult validate(String secretHeader)`.

`WebhookSecretValidationResult`:
- `ACCEPTED` — секрет валиден;
- `SKIPPED_NO_SECRET_CONFIGURED` — секрет в рантайме не настроен, проверка пропущена;
- `REJECTED` — валидация не пройдена (`WebhookValidationError` с кодом причины).

Поведение валидации:
- если секрет не настроен: `SKIPPED_NO_SECRET_CONFIGURED`;
- если секрет настроен, но header отсутствует: `REJECTED` + `SECRET_HEADER_MISSING`;
- если секрет настроен, но не совпадает: `REJECTED` + `SECRET_MISMATCH`;
- если секрет настроен и совпадает: `ACCEPTED`.

### `UpdateSink` / `UpdateConsumer`

Назначение:
- единый контракт потребителя normalized `Update`;
- целевая точка, куда `UpdateSource` передаёт событие после нормализации.
- асинхронная граница обработки update с явным результатом.

Граница:
- ingestion-слой не требует знания про router/filters/fsm.
- transport-детали не протекают наружу: sink работает только с `Update` и result-моделью.

Контракт (минимальный):
- `CompletionStage<UpdateHandlingResult> handle(Update update)`.

`UpdateHandlingResult`:
- `SUCCESS` — update принят downstream-слоем;
- `FAILURE` — update завершился ошибкой в ingestion boundary.

### `LongPollingRunner`

Назначение:
- runtime loop для `PollingUpdateSource`;
- управление циклом polling, паузами и завершением;
- делегирование каждого normalized `Update` в `UpdateSink`.

Граница:
- не принимает решения маршрутизации;
- не реализует middleware/FSM.

Контракт (минимальный):
- `start()`
- `stop()`
- `isRunning()`

Базовая execution модель:
- runner запускается в отдельном single-thread executor;
- один цикл выполняет `poll -> deliver to sink -> next poll`;
- пустой batch обрабатывается как idle-итерация;
- transient ошибки source/sink не останавливают runner автоматически.

Marker progression strategy (Sprint 2.2.3):
- marker хранится в `PollingMarkerState` (по умолчанию in-memory);
- marker продвигается только после успешно обработанного batch;
- пустой batch может продвинуть marker, если source вернул более новый `nextMarker`;
- при любой ошибке source marker не меняется;
- при частичной/полной ошибке sink marker не меняется (at-least-once replay семантика);
- регресс marker запрещён: применяется только монотонное продвижение вперёд.

### `WebhookReceiver`

Назначение:
- adapter boundary между HTTP endpoint и `WebhookUpdateSource`;
- принимает `WebhookRequest`, валидирует secret, десериализует `Update`
  и передаёт событие в `UpdateSink`.

Граница:
- не содержит бизнес-обработку update;
- не зависит от конкретного routing слоя.

Контракт (минимальный):
- `CompletionStage<WebhookReceiveResult> receive(WebhookRequest request)`.

`WebhookReceiveResult`:
- `ACCEPTED` — update успешно принят sink;
- `INVALID_SECRET` — webhook secret не прошёл проверку;
- `BAD_PAYLOAD` — body не удалось десериализовать в `Update`;
- `INTERNAL_ERROR` — ошибка sink/внутренней обработки receiver.

### `UpdatePipeline` (unified ingestion contract)

Назначение:
- единый вход normalized update flow для polling и webhook;
- единая точка передачи в следующий runtime слой.

Граница:
- в Sprint 2 может быть минимальным `UpdateSink`-совместимым контрактом;
- не обязан содержать routing/middleware semantics на этом этапе.

## Update lifecycle (ingestion layer)

1. Получение update:
- polling: `PollingUpdateSource.poll(...)` вызывает SDK `getUpdates` и возвращает `PollingBatch`;
- webhook: получение raw payload из HTTP ingress.

2. Нормализация:
- payload приводится к `ru.max.botframework.model.Update`;
- обязательные поля/тип update проверяются на уровне ingestion validation.

3. Передача в sink:
- normalized `Update` передаётся в единый `UpdateSink`/`UpdatePipeline`;
- polling и webhook используют один и тот же sink contract.

4. Ошибки ingestion-слоя:
- transport/decoding/normalization ошибки обрабатываются в ingestion boundary;
- ошибка одного update не должна приводить к падению всего ingestion runtime без явной policy;
- sink-ошибки выражаются через `UpdateHandlingResult.FAILURE` (или exceptional completion на boundary уровне);
- policy retry/backoff/skip фиксируется ingestion runtime и не смешивается с business routing.

## Unified flow requirement

Ключевое требование Sprint 2:
- polling и webhook обязаны приводиться к одному и тому же internal update flow;
- downstream слой получает одинаковый normalized `Update` независимо от источника;
- источник (`polling`/`webhook`) не должен менять семантику дальнейшей обработки.

## Related docs

- [event-model.md](event-model.md)
- [api-contract.md](api-contract.md)
- [adr/0004-unified-update-pipeline.md](adr/0004-unified-update-pipeline.md)
