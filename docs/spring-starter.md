# Spring Starter

## Module

`max-spring-boot-starter` подключает runtime, polling/webhook bootstrap и router registration.

## Properties (`max.bot.*`)

- `token` (required)
- `base-url` (default `https://api.max.ru`)
- `mode`: `POLLING | WEBHOOK`

`max.bot.polling.*`:

- `enabled` (default `true`)
- `limit` (default `100`)
- `timeout` (default `30s`)
- `types` (`UpdateEventType` list)

`max.bot.webhook.*`:

- `enabled`
- `path` (default `/webhook/max`)
- `secret`
- `max-in-flight`

`max.bot.storage.*`:

- `type` (`MEMORY | REDIS`)
- `state-scope` (`USER | CHAT | USER_IN_CHAT`)

`max.bot.storage.redis.*` (when `type=REDIS`):

- `key-prefix` (default `max:bot:fsm`)
- `ttl` (optional, example `120s`)

`max.bot.route-component-scan.*`:

- `enabled` (default `true`) — авто-детект классов с `@Route` как Spring beans.

## Auto-configured beans

- `MaxBotClient` stack
- `Dispatcher`
- polling/webhook bootstrap beans (по mode)
- `MessagingFacade`, `CallbackFacade`, `ChatActionsFacade`
- `MediaMessagingFacade` (если есть `UploadService` bean)

## Router registration

- Все `Router` beans автоматически включаются в dispatcher (ordered stream).
- `@Route(autoRegister = true)` beans автоматически конвертируются в routers через `AnnotatedRouteRegistrar`.
- Классы с `@Route` можно не аннотировать `@Component`: starter регистрирует их автоматически (если включен `max.bot.route-component-scan.enabled=true`).
