# MAX API Compatibility Migration Plan

## Context

Проверка официальной документации MAX показала расхождения между текущими внутренними моделями framework и внешним API shape:

- [User](https://dev.max.ru/docs-api/objects/User)
- [Chat](https://dev.max.ru/docs-api/objects/Chat)
- [Message](https://dev.max.ru/docs-api/objects/Message)
- [Update](https://dev.max.ru/docs-api/objects/Update)
- [NewMessageBody](https://dev.max.ru/docs-api/objects/NewMessageBody)

Текущие runtime-контракты (`Dispatcher`, `Router`, `RuntimeContext`, существующие handlers/tests) считаются стабильными, поэтому миграция должна быть **additive**.

## Goal

Сделать внешний MAX contract-совместимый transport слой, не ломая существующий normalized runtime API.

## Target architecture

### 1) Transport DTO layer (external shape)

Добавить отдельные DTO с полями в формате MAX API (`snake_case`, официальные object shapes).

Пакет:

- `ru.tardyon.botframework.model.transport`

Примеры классов:

- `ApiUser`
- `ApiChat`
- `ApiMessage`
- `ApiUpdate`
- `ApiNewMessageBody`
- вложенные transport-типы (`ApiMessageBody`, `ApiRecipient`, `ApiLinkedMessage`, etc. по мере необходимости)

### 2) Normalized domain layer (current runtime shape)

Оставить текущие типы без breaking изменений:

- `ru.tardyon.botframework.model.User`
- `Chat`
- `Message`
- `Update`
- `request.NewMessageBody`

### 3) Mapping layer

Добавить явный mapper:

- `ru.tardyon.botframework.model.mapping.MaxApiModelMapper`

Направления:

- `Api* -> normalized` (для входящих payload)
- `normalized request -> Api*` (для исходящих request bodies)

## Migration phases

## Phase 1: Introduce transport DTOs + mapper (no runtime wiring changes)

### Scope

- Добавить transport DTO классы.
- Добавить mapper + unit tests на field-level mapping.
- Не менять сигнатуры существующих public методов.

### Codex tasks

1. Создать `model.transport` DTO для объектов `User`, `Chat`, `Message`, `Update`, `NewMessageBody`.
2. Ввести enum/typing для `update_type`, `chat type/status`, `format` в transport слое.
3. Добавить `MaxApiModelMapper` и тесты map roundtrip для key fields.
4. Добавить fixtures с payload-образцами из docs-format (`snake_case`) в `max-model/src/test/resources/fixtures/api-docs/`.

### Exit criteria

- Все новые mapper tests green.
- Текущие tests existing modules green.

## Phase 2: Polling/Webhook ingestion consume transport DTOs

### Scope

- Ingestion читает transport `ApiUpdate`, затем маппит в normalized `Update`.

### Integration points

- `SdkPollingUpdateSource` (polling)
- `DefaultWebhookReceiver` (webhook)

### Codex tasks

1. Добавить transport response envelope для getUpdates (`ApiGetUpdatesResponse`).
2. Переключить parsing ingestion на transport DTO.
3. Применять mapper в ingestion boundary перед `UpdatePipeline`.
4. Добавить integration tests с docs-shape payload fixtures для polling/webhook.

### Exit criteria

- Existing ingestion tests green.
- New docs-shape ingestion tests green.

## Phase 3: Outgoing request compatibility (`NewMessageBody`)

### Scope

- Формировать исходящее тело запроса в официальном MAX shape (`text`, `attachments`, `link`, `notify`, `format`).

### Integration points

- `SendMessageMethodRequest`
- `EditMessageMethodRequest`
- JSON serialization path in `DefaultMaxBotClient`

### Codex tasks

1. Ввести transport request payload for message body.
2. Добавить mapper `NewMessageBody -> ApiNewMessageBody`.
3. Подключить mapping в method requests/client execution.
4. Покрыть serialization tests на JSON field contract.

### Exit criteria

- Existing client serialization tests green.
- New transport body serialization tests green.

## Phase 4: Backward compatibility hardening

### Scope

- Сохранить старые normalized модели доступными и стабильными.
- Добавить compatibility adapters/aliases, где это безопасно.

### Codex tasks

1. Проверить публичные API surface на binary/source compatibility.
2. Добавить migration notes в docs (`README`, `docs/api-contract.md`, release notes).
3. Добавить deprecation strategy только для truly obsolete internal paths.

### Exit criteria

- No breaking changes для пользователей runtime API.
- Полный regression green.

## Risk register

- Разные event shapes внутри `Update` (наследники событий) могут требовать расширения transport DTO beyond initial five objects.
- Неполные doc samples могут не покрывать все реальные payload вариации; нужны live-contract fixtures.
- Ошибочный маппинг `chat type/status` может влиять на built-in filters (`chatType`, state flows через chat/user scope).

## Testing strategy

- Unit: mapper field-by-field.
- Integration: polling/webhook ingestion с docs-shape fixtures.
- Contract snapshots: JSON fixtures для outbound `NewMessageBody`.
- Regression: полный `./gradlew test` перед merge.

## Rollout strategy

1. Merge Phase 1 отдельно (low-risk).
2. Merge Phase 2 отдельно (ingestion behavior change).
3. Merge Phase 3 отдельно (outgoing payload change).
4. Final docs/release pass.

Это позволит быстро локализовать regressions между этапами.
