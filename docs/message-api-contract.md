# Message API Contract Specification

## Status

Документ фиксирует публичный контракт Message API для framework.
Это спецификация API и поведения, а не реализация.

Актуальный Sprint 6.1 contract:
- [messaging-api.md](messaging-api.md)

## Goals

- Предоставить builder-style API для работы с сообщениями.
- Скрыть low-level request assembly за типобезопасными builders.
- Сохранить единый DX для send/edit/delete/reply сценариев.

## Scope

Контракт покрывает:
- отправку сообщений (`send`);
- редактирование сообщений (`edit`);
- удаление сообщений (`delete`);
- ответ на сообщение (`reply`);
- форматирование (`format`);
- управление notify;
- attachments;
- keyboard integration.

## Core operations

### Send

Назначение: отправка нового сообщения в чат.

Пример:

```java
bot.send(
    Messages.text("Привет")
        .chatId(chatId)
        .format(Format.MARKDOWN)
        .notify(true)
);
```

Контракт:
- `chatId` обязателен;
- body задаётся через typed builders (`text`, `media`, `template` post-MVP);
- platform validation выполняется до HTTP-вызова.

### Edit

Назначение: изменение ранее отправленного сообщения (text/markup/attachments в пределах возможностей MAX API).

Пример:

```java
bot.edit(
    Messages.edit(messageId)
        .chatId(chatId)
        .text("Статус: обновлено")
        .keyboard(k -> k.row(Buttons.callback("OK", "ok")))
);
```

Контракт:
- `messageId` и `chatId` обязательны;
- недопустимые изменения должны падать validation error до отправки;
- если MAX API ограничивает edit конкретного payload, framework возвращает typed exception.

### Delete

Назначение: удаление сообщения.

Пример:

```java
bot.delete(
    Messages.delete(messageId)
        .chatId(chatId)
);
```

Контракт:
- операция идемпотентна на уровне framework API (повторный delete допустим, поведение зависит от MAX API ответа);
- обязательные поля: `chatId`, `messageId`.

### Reply

Назначение: ответ на текущее входящее сообщение/контекст без ручного указания `chatId`.

Пример:

```java
router.message(F.message().commandStart(), ctx ->
    ctx.reply(
        Messages.text("Добро пожаловать")
            .keyboard(k -> k.row(Buttons.callback("Начать", "start")))
    )
);
```

Контракт:
- `Context.reply(...)` автоматически использует chat/thread данные текущего update;
- reply может переопределить target явно (для advanced сценариев).

## Builder model

### MessageBuilder (send/reply)

Базовый fluent API:

```java
MessageBuilder builder = Messages.text("Текст")
    .chatId(chatId)
    .format(Format.PLAIN)
    .notify(true)
    .attachment(Attachments.file(uploadRef))
    .keyboard(k -> k.row(Buttons.callback("Открыть", "open")));
```

Базовые поля MVP:
- `chatId`;
- `text` (для text builder);
- `format` (`PLAIN`, `MARKDOWN`, `HTML` только если поддерживается MAX);
- `notify` (true/false);
- `attachments` (0..N, в пределах platform limits);
- `keyboard` (inline/reply в зависимости от поддерживаемого типа).

### EditMessageBuilder

```java
EditMessageBuilder edit = Messages.edit(messageId)
    .chatId(chatId)
    .text("Новый текст")
    .format(Format.MARKDOWN)
    .keyboard(keyboard);
```

MVP правила:
- edit builder принимает только edit-совместимые поля;
- попытка изменить immutable поля -> validation error.

### DeleteMessageBuilder

```java
DeleteMessageBuilder delete = Messages.delete(messageId)
    .chatId(chatId);
```

## Formatting contract

- Формат задаётся enum-ом `Format`.
- По умолчанию используется безопасный plain text.
- Если формат не поддерживается целевым MAX endpoint/типом сообщения, операция завершается typed validation error.
- Экранирование спецсимволов может предоставляться helper-утилитами (`Escapers.markdown(...)`).

## Notify contract

- `notify(true)` — обычная доставка уведомления.
- `notify(false)` — тихая доставка (если поддержано MAX API).
- При отсутствии поддержки на стороне MAX API framework обязан явно документировать fallback behavior.

## Attachment contract

Attachment API строится на upload abstractions, а не raw multipart в handler-коде.

Пример:

```java
Attachment photo = Attachments.photo(uploadRef)
    .caption("Счёт")
    .format(Format.MARKDOWN);

bot.send(
    Messages.media(photo)
        .chatId(chatId)
        .notify(false)
);
```

Контракт:
- attachment builder принимает только валидные комбинации полей;
- upload lifecycle (pre-upload/token/reference) скрыт за `uploadRef` abstraction;
- attachment limits валидируются до отправки.

Детальный upload/media contract (input abstractions и multi-step flow):
- [upload-media-contract.md](upload-media-contract.md)

## Keyboard contract integration

Message API интегрируется с `KeyboardBuilder`:

```java
InlineKeyboard kb = Keyboards.inline(k -> k
    .row(
        Buttons.callback("Оплатить", "pay:1"),
        Buttons.link("Сайт", "https://example.com")
    )
);

ctx.reply(Messages.text("Выберите действие").keyboard(kb));
```

Контракт:
- keyboard тип валидируется относительно типа сообщения;
- invalid button payload -> validation error.

## Context-level convenience API

MVP convenience методы:
- `ctx.reply(MessageBuilder builder)`;
- `ctx.edit(EditMessageBuilder builder)` для текущего message context где применимо;
- `ctx.deleteCurrentMessage()` как shortcut (где доступно);
- `ctx.answerCallback(...)` остаётся отдельной callback-операцией.

## Error model

Типовые категории ошибок Message API:
- `ValidationException` — локальная проверка builder контракта;
- `ApiRequestException` — ошибка MAX API (4xx/5xx/transport);
- `UnsupportedOperationException` — операция не поддерживается для данного update/message type.

Требования:
- ошибки должны содержать operation kind (`send/edit/delete/reply`);
- должны включать diagnostic fields (`chatId`, `messageId` если есть).

## Compatibility guarantees

- Fluent builder методы считаются частью public API.
- Изменения имен/семантики методов — только с миграционной документацией.
- При конфликте между DX parity и реальными возможностями MAX API приоритет у MAX compatibility.

## Related docs

- [api-contract.md](api-contract.md)
- [filter-contract.md](filter-contract.md)
- [middleware-contract.md](middleware-contract.md)
- [event-model.md](event-model.md)
- [product-spec.md](product-spec.md)
