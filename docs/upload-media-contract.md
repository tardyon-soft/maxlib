# Upload/Media Contract Specification

## Status

Документ фиксирует публичный контракт upload/media API framework для MVP.
Это спецификация API и поведения, а не реализация.

## Goals

- Дать простой high-level API отправки медиа без ручной работы с многошаговым upload flow.
- Зафиксировать набор input abstractions для файлов и байтовых источников.
- Гарантировать типобезопасную интеграцию upload с Message API builders.

## Scope

Контракт покрывает:
- входные абстракции пользовательского файла;
- high-level API отправки медиа в сообщениях;
- модель сокрытия многошагового MAX upload flow;
- базовые правила валидации и ошибок.

## Core abstractions

### UploadInput

`UploadInput` — единая абстракция пользовательского входа для загрузки.

MVP формы:

1. `UploadInput.fromPath(Path path)`
- для локальных файлов.

2. `UploadInput.fromBytes(byte[] content, String fileName)`
- для in-memory содержимого.

3. `UploadInput.fromStream(StreamSupplier supplier, String fileName)`
- для больших/потоковых источников.

4. `UploadInput.fromResource(Resource resource)`
- для интеграции со Spring/Classpath-like ресурсами.

5. `UploadInput.existing(UploadRef ref)`
- для повторного использования уже загруженного media reference.

Контракт:
- `UploadInput` не раскрывает transport детали multi-step flow;
- content-type может быть передан явно или определён автоматически;
- имя файла и размер доступны для предварительной валидации.

### UploadRef

`UploadRef` — immutable ссылка на уже принятый MAX platform media object.

Использование:
- повторная отправка медиа без повторной загрузки;
- хранение в приложении/БД для последующих сообщений.

## High-level media API

### Media builders

High-level API для отправки медиа строится вокруг typed media builders.

Примеры:

```java
bot.send(
    Messages.media(
        Media.photo(UploadInput.fromPath(Path.of("./invoice.png")))
            .caption("Счёт")
            .format(Format.MARKDOWN)
    ).chatId(chatId)
);
```

```java
UploadRef ref = uploads.upload(UploadInput.fromBytes(bytes, "report.pdf"));

ctx.reply(
    Messages.media(
        Media.document(UploadInput.existing(ref))
            .caption("Отчёт")
    )
);
```

Контракт:
- медиа-тип задаётся явно (`photo`, `video`, `audio`, `document`, `file` в зависимости от поддерживаемого MAX surface);
- caption/format/notify/keyboard настраиваются через общий Message API;
- попытка использовать неподдерживаемую комбинацию полей даёт validation error.

### Upload facade (optional explicit API)

Для случаев предварительной загрузки доступен explicit facade:

```java
UploadRef ref = uploads.upload(
    UploadInput.fromPath(Path.of("./photo.jpg"))
);
```

Этот API optional для пользователя: `Messages.media(...)` может выполнять upload автоматически.

## Hidden multi-step MAX upload flow

Framework скрывает многошаговый upload процесс за единым вызовом.

Концептуальный flow:

1. `prepare`:
- framework запрашивает у MAX параметры загрузки и upload session metadata.

2. `transfer`:
- framework передаёт бинарные данные в upload endpoint (single-part или chunked, если требуется платформой).

3. `finalize`:
- framework подтверждает upload и получает platform media reference.

4. `send`:
- framework использует media reference в message request.

Пользовательский код не управляет этими шагами вручную.

## Validation and limits contract

Перед отправкой framework валидирует:
- обязательные поля media builder;
- допустимый размер/тип файла (если лимиты известны заранее);
- согласованность caption/format/media-type;
- совместимость keyboard/notify/attachment комбинации.

Если часть ограничений проверяется только сервером MAX, framework возвращает typed API exception с диагностикой operation stage (`prepare`/`transfer`/`finalize`/`send`).

## Error model

Категории ошибок:
- `UploadValidationException` — локальная ошибка входных данных;
- `UploadTransferException` — ошибка передачи содержимого;
- `UploadFinalizeException` — ошибка завершения upload;
- `ApiRequestException` — ошибка отправки сообщения/серверный отказ.

Требования:
- ошибка должна содержать operation stage и идентификатор запроса;
- retry policy для transfer/finalize задаётся runtime policy и не протекает в handler API.

## Lifecycle and observability

- Каждый upload/send flow имеет correlation metadata в `Context` attributes (если включено middleware enrichment).
- Framework публикует диагностические hooks для testkit/metrics на этапах `prepare`, `transfer`, `finalize`, `send`.
- Upload flow одинаково доступен из polling и webhook handler-ов.

## Non-goals

- Детали внутреннего HTTP протокола MAX upload endpoint.
- Полный список всех media типов и их ограничений для всех будущих версий MAX.
- Реализация storage/caching стратегии `UploadRef` вне контракта API.

## Related docs

- [message-api-contract.md](message-api-contract.md)
- [api-contract.md](api-contract.md)
- [callback-contract.md](callback-contract.md)
- [event-model.md](event-model.md)
- [product-spec.md](product-spec.md)
