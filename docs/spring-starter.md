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

- `type` (`MEMORY`)
- `state-scope` (`USER | CHAT | USER_IN_CHAT`)

## Auto-configured beans

- `MaxBotClient` stack
- `Dispatcher`
- polling/webhook bootstrap beans (по mode)
- `MessagingFacade`, `CallbackFacade`, `ChatActionsFacade`
- `MediaMessagingFacade` (если есть `UploadService` bean)

## Router registration

- Все `Router` beans автоматически включаются в dispatcher (ordered stream).
- `@Route(autoRegister = true)` beans автоматически конвертируются в routers через `AnnotatedRouteRegistrar`.
