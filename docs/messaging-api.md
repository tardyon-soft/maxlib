# Messaging API Contract (Sprint 6)

## Status

Документ фиксирует контракт high-level messaging layer для Sprint 6.
Это спецификация public API и поведения, а не полная реализация.

## Goal

Дать разработчику ergonomic API для типичных bot-сценариев:
- отправка текста/ответа;
- редактирование/удаление;
- inline keyboard builder;
- callback answer;
- chat actions.

При этом messaging layer должен строиться поверх уже существующих слоёв:
- runtime (`Dispatcher` / `Router` / `RuntimeContext`);
- typed SDK (`MaxBotClient` и request/response модели);
- без дублирования transport logic.

## Sprint 6 boundaries

В scope Sprint 6:
- high-level message/callback/chat-action facade;
- builders/factories для message/keyboard/buttons;
- runtime-level convenience API для handler-кода.

Вне scope Sprint 6:
- полноценный upload/media subsystem (будет отдельным этапом);
- FSM/scenes;
- Spring starter integration.

## Core contracts

### `MessageTarget`

Назначение:
- typed target для message operations без ручной сборки transport request.

Ответственность:
- инкапсулировать минимальный routing context (`chatId`, optional `messageId`, optional thread/reply context);
- использоваться как единая точка для `send/reply/edit/delete`.

Пример (концептуально):

```java
MessageTarget target = MessageTarget.chat(chatId);
MessageTarget replyTarget = MessageTarget.replyTo(chatId, messageId);
```

### `MessageBuilder`

Назначение:
- builder-style сборка send/reply payload.

Ответственность:
- выразить message payload в framework-уровневых типах;
- валидировать обязательные поля до SDK вызова;
- хранить keyboard/format/notify параметры.

MVP поля:
- `text`;
- `format` (`TextFormat`-совместимая модель);
- `notify`;
- `keyboard` (inline keyboard contract Sprint 6);
- reply targeting через `MessageTarget`/runtime context.

### `Messages` factory

Назначение:
- entrypoint для создания builders.

Пример (концептуально):

```java
Messages.text("Привет");
Messages.reply("Готово");
Messages.edit("Новый текст");
Messages.delete();
```

Контракт:
- factory не выполняет HTTP-запросы;
- factory возвращает immutable/controlled builders.

### `KeyboardBuilder`

Назначение:
- декларативная сборка inline keyboard в Java-friendly DSL стиле.

Пример (концептуально):

```java
Messages.text("Выберите действие")
    .keyboard(k -> k.row(
        Buttons.callback("Оплатить", "pay:1"),
        Buttons.link("Сайт", "https://example.com")
    ));
```

Контракт:
- deterministic button order;
- валидация payload/button constraints до SDK вызова.

### `Buttons`

Назначение:
- factory typed button-конструкторов (`callback`, `link`, и т.д. в пределах MAX возможностей).

Контракт:
- no magic strings where typed value possible;
- validation-friendly input model.

### High-level messaging facade/service

Назначение:
- тонкий orchestration слой для выполнения high-level operations через `MaxBotClient`.

Рабочее имя контракта: `MessagingFacade` (финальное имя может быть скорректировано при реализации).

Ответственность:
- маппинг high-level builders -> SDK requests;
- вызовы `MaxBotClient` (`sendMessage`, `editMessage`, `deleteMessage`, `answerCallback`, ...);
- единый error boundary messaging-слоя (validation + API failure propagation).

Не отвечает за:
- routing/dispatch (это runtime layer);
- raw HTTP/serialization (это SDK layer).

### Callback answer abstraction

Назначение:
- ergonomic API для ответа на callback внутри handler-кода.

Пример (концептуально):

```java
ctx.callback().answer("OK");
// или
ctx.answerCallback("OK");
```

Контракт:
- используется существующий SDK callback method;
- runtime convenience API не дублирует transport client.

### Chat actions abstraction

Назначение:
- удобная отправка chat action (typing/upload/etc.) как high-level operation.

Пример (концептуально):

```java
ctx.chatAction(ChatAction.TYPING);
```

Контракт:
- основан на typed `ChatAction` model;
- выполняется через существующий SDK client слой.

## Desired DX examples

### 1) Send text

```java
ctx.send(
    Messages.text("Привет!")
);
```

### 2) Reply

```java
ctx.reply(
    Messages.text("Принято")
);
```

### 3) Edit/Delete

```java
ctx.edit(
    Messages.edit("Статус обновлён")
);

ctx.delete(
    Messages.delete()
);
```

### 4) Keyboard builder

```java
ctx.reply(
    Messages.text("Выберите действие")
        .keyboard(k -> k.row(
            Buttons.callback("Оплатить", "pay:1"),
            Buttons.link("Сайт", "https://example.com")
        ))
);
```

### 5) Callback answer + chat action

```java
router.callback(callback -> {
    return ctx.answerCallback("OK")
        .thenCompose(ignored -> ctx.chatAction(ChatAction.TYPING));
});
```

## Architecture constraints

- Messaging layer обязан использовать существующий `MaxBotClient` и runtime context.
- Нельзя дублировать transport/client abstractions новым параллельным client API.
- Upload/media details не подтягиваются в Sprint 6.1 (только совместимые extension points).
- Контракт должен оставаться совместимым с текущим dispatcher/filter/middleware/DI pipeline.

## Related docs

- [api-contract.md](api-contract.md)
- [runtime-contract.md](runtime-contract.md)
- [message-api-contract.md](message-api-contract.md)
- [callback-contract.md](callback-contract.md)
- [roadmap.md](roadmap.md)
