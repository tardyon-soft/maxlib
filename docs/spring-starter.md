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

`max.bot.screen.callback.codec.*`:

- `mode` (`LEGACY_STRING | TYPED_V1`, default `LEGACY_STRING`)
  - `LEGACY_STRING` — legacy payload format (`ui:act:<action>?k=v`)
  - `TYPED_V1` — typed v1 payload format with backward-compatible fallback parsing of legacy callbacks.

`max.bot.screen.*`:

- `namespace` (default `max.screen`) — FSM namespace для screen state.

## Auto-configured beans

- `MaxBotClient` stack
- `Dispatcher`
- `ScreenActionCodec` (selected by `max.bot.screen.callback.codec.mode`)
- polling/webhook bootstrap beans (по mode)
- `MessagingFacade`, `CallbackFacade`, `ChatActionsFacade`
- `MediaMessagingFacade` (если есть `UploadService` bean)

## Router registration

- Все `Router` beans автоматически включаются в dispatcher (ordered stream).
- `@Route(autoRegister = true)` beans автоматически конвертируются в routers через `AnnotatedRouteRegistrar`.
- Классы с `@Route` можно не аннотировать `@Component`: starter регистрирует их автоматически (если включен `max.bot.route-component-scan.enabled=true`).

## Screen Controller Facade

Starter поддерживает facade-аннотации для screen API:

- `@ScreenController` — класс-контроллер, который может описывать несколько экранов;
- `@ScreenView(screen = "...")` — render метода экрана;
- `@OnScreenAction(screen = "...", action = "...")` — action handler;
- `@OnScreenText(screen = "...")` — text handler.

Поддерживаемые параметры методов facade:

- `ScreenContext`
- `RuntimeContext`
- `Message`
- `Callback`
- `String` (для text/action payload)
- `Map<String, String>` (args action)

Поведение совместимо с существующим `ru.tardyon.botframework.screen.annotation.*` API:

- старые `@Screen/@Render/@OnAction/@OnText` продолжают работать;
- новый facade включается опционально и не ломает legacy flow.

## Widget Annotation Layer

Starter поддерживает widget facade-аннотации:

- `@WidgetController`
- `@Widget(id = "...")`
- `@OnWidgetAction(widget = "...", action = "...")`

Runtime-контракты для widget layer находятся в `max-dispatcher`:

- `WidgetContext`
- `WidgetView`
- `WidgetEffect`
- `WidgetViewResolver`
- `WidgetActionDispatcher`

Интеграция в screen pipeline:

- В screen render можно вставлять widget layer через `Widgets.ref("widget.id")`;
- Можно вставлять готовый `WidgetView` через `ScreenModel.builder().widgetView(...)`;
- Старый `Widget` API продолжает работать без изменений.

## Form/Wizard Engine (`max-dispatcher`)

Доступен базовый form runtime для многошаговых wizard-сценариев в screen namespace:

- `FormDefinition`
- `FormStep`
- `FormState`
- `FormValidator`
- `FormEngine`
- `FormStateStorage` + FSM-backed реализация `FsmFormStateStorage`

Поддерживаемые переходы:

- `next`
- `back`
- `cancel`
- `submit`

Особенности:

- Валидация шага блокирует переход и возвращает ошибку без потери текущего состояния.
- Состояние формы восстанавливается на следующем update через FSM payload (`ui.form.state.v1`).
- Текущий screen API (`@Screen/@Render/@OnAction/@OnText`) остается совместимым и не требует изменений.

## Related Guides

- [Screen Facade Guide](screen-facade-guide.md)
- [Widget Layer Guide](widget-layer-guide.md)
- [Form Engine Guide](form-engine-guide.md)
- [Screen Callback Codec Modes](screen-callback-codec-modes.md)
- [Screen E2E Scenario](screen-e2e-scenario.md)
- [Screen Migration Notes](screen-migration-notes.md)
