# MAX Java Bot Framework

Java framework для разработки ботов на платформе MAX с DX в стиле aiogram 3.

Важно: это не буквальная копия aiogram 3. Проект адаптирует лучшие архитектурные идеи под реальные возможности MAX API и Java ecosystem.

## Sprint status

Текущий этап: `Sprint 8 — FSM / scenes / storage`.

Завершённые этапы:
- Sprint 1 (`client/DTO/errors`);
- Sprint 2 (`polling + webhook ingestion layer`);
- Sprint 3 (`dispatcher/router/runtime foundation`);
- Sprint 4 (`filters/middleware/context enrichment`).
- Sprint 5 (`DI / parameter resolution / invocation`).
- Sprint 6 (`messages / keyboards / callbacks ergonomics`).
- Sprint 7 (`upload / media pipeline`).

Текущая цель Sprint 8:
- `FSMContext` и storage abstraction;
- state scopes + `StateFilter`;
- scenes/wizard minimal runtime APIs;
- интеграция state layer в текущий dispatcher/runtime pipeline.
- контракт Sprint 8 зафиксирован в `docs/fsm-and-scenes.md`.
- реализован core state model: `StateScope`, `StateKey`, `StateData`, `StateSnapshot`,
  `StateKeyStrategy` и built-in `StateKeyStrategies` (`USER`, `CHAT`, `USER_IN_CHAT`).
- реализован async storage contract `FSMStorage` (typed state/state-data operations + merge update),
  с явным разделением state и payload data.
- реализован `MemoryStorage` как thread-safe baseline implementation `FSMStorage`
  для unit/integration тестов и простых runtime сценариев.
- реализован runtime-facing `FSMContext` (`StorageBackedFSMContext`) для
  state/data операций в handler-ориентированном API.

Что уже реализовано:
- multi-module Gradle проект (Kotlin DSL) на Java 21;
- `max-client-core` foundation слой (transport, auth, serialization, errors, retry/rate-limit hooks, pagination);
- `max-model` с базовыми DTO, typed value objects и enum-контрактами;
- зафиксирован runtime contract Sprint 3 (`Dispatcher`, `Router`, `EventObserver`, `Handler`, `DispatchResult`);
- добавлена Java-friendly handler signature model Sprint 5: `ContextualEventHandler<TEvent>` (`event + RuntimeContext`);
- добавлен typed runtime data container foundation: `RuntimeDataContainer` + `RuntimeDataKey<T>` + `RuntimeDataScope`;
- добавлено ядро invocation engine Sprint 5:
  `HandlerInvoker` (`DefaultHandlerInvoker`), `HandlerParameterResolver`, `ResolverRegistry`,
  базовые resolvers для `RuntimeContext`/`Update`/`Message`/`Callback`/`User`/`Chat`/event
  и enrichment-derived parameters (filter data, middleware data);
- добавлен application-level injection bridge:
  `Dispatcher.registerService(...)` и `Dispatcher.registerApplicationData(...)`
  + built-in `ApplicationDataParameterResolver` в default invoker chain;
- dispatch pipeline теперь вызывает handler’ы через invocation engine (`HandlerInvoker`),
  включая reflective method handlers с parameter resolution из runtime sources;
- реализован базовый observer layer в `max-dispatcher`: `EventObserver`, `EventHandler`, `DefaultEventObserver`, MVP observer types (`update/message/callback/error`);
- реализован базовый filter contract в runtime: `Filter<TEvent>`, `FilterResult` (match/not-match/failed + enrichment), композиция `and/or/not`, filter-aware handler registration в `Router` и built-in filters MVP (`Command`, `TextEquals`, `TextStartsWith`, `ChatType`, `FromUser`, `HasAttachment`, `StateFilter` placeholder);
- реализованы middleware contracts foundation: `OuterMiddleware`, `InnerMiddleware`, `MiddlewareNext`, `RuntimeContext`/`ContextKey` и chain executor с short-circuit support;
- ingestion target contract в `max-dispatcher`: `UpdateConsumer` (async, preferred) + `UpdateSink` (compat alias) + `UpdateHandlingResult`;
- polling source abstraction в `max-dispatcher`: `PollingUpdateSource` + `SdkPollingUpdateSource` (SDK-backed `getUpdates` pull);
- long polling runtime foundation: `DefaultLongPollingRunner` с lifecycle API (`start/stop/shutdown/isRunning`);
- graceful lifecycle semantics для polling runner: `stop()` vs `shutdown()` + ownership-aware resource cleanup;
- marker progression contract: monotonic marker state с продвижением только после успешного batch handling;
- webhook secret validation foundation: `WebhookSecretValidator` + typed validation result/error contracts;
- webhook receiver foundation: `DefaultWebhookReceiver` (`WebhookRequest` -> `WebhookReceiveResult`);
- webhook overload control foundation: `WebhookReceiverConfig.maxInFlightRequests` + `OVERLOADED` result;
- unified ingestion pipeline foundation: `UpdatePipeline` + `DefaultUpdatePipeline` + `UpdatePipelineContext`;
- integration-style ingestion tests with JSON fixtures for polling/webhook regression safety;
- domain-level операции в client SDK: `getMe`, message operations, callback answer, `getUpdates`, webhook subscriptions;
- тестовая инфраструктура client SDK: JSON fixtures + reusable mocked HTTP context.
- Sprint 7 foundation: `InputFile` abstraction (`ru.max.botframework.upload`) with
  `fromPath(...)`, `fromBytes(...)`, `fromStream(...)` + file metadata (`fileName`, `contentType`, `knownSize`).
- Sprint 7 orchestration contract: `UploadService` (`prepare -> transfer -> finalize`) with separated gateways
  (`UploadPreparationGateway`, `UploadTransferGateway`, `UploadFinalizeGateway`) and result mapping (`UploadResultMapper`).
- Sprint 7 multipart transfer implementation:
  - `MultipartUploadTransferGateway` for `UploadFlowType.MULTIPART`;
  - reusable raw HTTP boundary: `MultipartUploadHttpClient` + `JdkMultipartUploadHttpClient`;
  - typed transfer models: `MultipartUploadRequest`, `MultipartUploadResponse`;
  - dedicated transfer-stage error boundary: `UploadTransferException`.
- Sprint 7 resumable transfer implementation:
  - `ResumableUploadTransferGateway` for `UploadFlowType.RESUMABLE`;
  - chunk-level boundary: `ResumableChunkUploadClient` with typed request/response models;
  - configurable runtime strategy: `ResumableUploadOptions` (`chunkSizeBytes`, `maxRetriesPerChunk`);
  - in-operation resumable state: monotonically increasing chunk offset with regression guard.
- unified upload result semantics for attachment layer:
  - `UploadResult` carries `ref`, `flowType`, transferred bytes and `contentType`;
  - normalized `mediaKind` (`IMAGE/FILE/VIDEO/AUDIO/UNKNOWN`);
  - immutable `attachmentPayload` for media-specific metadata (for example dimensions/duration/preview refs).
- high-level media attachment abstractions:
  - `ImageAttachment`, `FileAttachment`, `VideoAttachment`, `AudioAttachment`;
  - unified contract `MediaAttachment` mapped to low-level `NewMessageAttachment`;
  - `MessageBuilder.attachment(MediaAttachment)` for seamless message composition.
- high-level media send/reply API:
  - `MediaMessagingFacade` with `sendImage/sendFile/sendVideo/sendAudio`;
  - `replyImage/replyFile/replyVideo/replyAudio` over existing reply flow;
  - internals reuse `InputFile + UploadService + MessagingFacade`.

## Modules

- `max-client-core` — Java SDK поверх MAX API.
- `max-model` — DTO, enum и value objects для MAX domain.
- `max-dispatcher` — runtime foundation: dispatcher, router tree, observers, dispatch/error model, ingestion integration.
- `max-fsm` — FSM contracts и core state model/scoping strategy foundation.
- `max-spring-boot-starter` — заготовка Spring Boot integration.
- `max-testkit` — заготовка framework test utilities.

## Quick start (client SDK foundation)

```java
import java.time.Duration;
import okhttp3.OkHttpClient;
import ru.max.botframework.client.DefaultMaxBotClient;
import ru.max.botframework.client.MaxApiClientConfig;
import ru.max.botframework.client.MaxBotClient;
import ru.max.botframework.client.http.MaxHttpClient;
import ru.max.botframework.client.http.okhttp.OkHttpMaxHttpClient;
import ru.max.botframework.client.serialization.JacksonJsonCodec;
import ru.max.botframework.model.BotInfo;

MaxApiClientConfig config = MaxApiClientConfig.builder()
    .token("YOUR_BOT_TOKEN")
    .connectTimeout(Duration.ofSeconds(5))
    .readTimeout(Duration.ofSeconds(30))
    .build();

OkHttpClient okHttp = new OkHttpClient.Builder()
    .connectTimeout(config.connectTimeout())
    .readTimeout(config.readTimeout())
    .build();

MaxHttpClient httpClient = new OkHttpMaxHttpClient(config.baseUri(), okHttp);
MaxBotClient botClient = new DefaultMaxBotClient(config, httpClient, new JacksonJsonCodec());

BotInfo me = botClient.getMe();
```

## FSM MemoryStorage baseline (Sprint 8)

```java
import ru.max.botframework.fsm.FSMStorage;
import ru.max.botframework.fsm.MemoryStorage;
import ru.max.botframework.fsm.StateData;
import ru.max.botframework.fsm.StateKey;
import ru.max.botframework.model.ChatId;
import ru.max.botframework.model.UserId;

FSMStorage storage = new MemoryStorage();
StateKey key = StateKey.userInChat(new UserId("u-1"), new ChatId("c-1"));

storage.setState(key, "checkout.email").toCompletableFuture().join();
storage.updateStateData(key, java.util.Map.of("email", "user@example.com")).toCompletableFuture().join();

String state = storage.getState(key).toCompletableFuture().join().orElse("none");
StateData data = storage.getStateData(key).toCompletableFuture().join();
```

## FSMContext usage (Sprint 8)

```java
import ru.max.botframework.fsm.FSMContext;
import ru.max.botframework.fsm.FSMStorage;
import ru.max.botframework.fsm.MemoryStorage;
import ru.max.botframework.fsm.StateKey;
import ru.max.botframework.model.ChatId;
import ru.max.botframework.model.UserId;

FSMStorage storage = new MemoryStorage();
FSMContext fsm = FSMContext.of(
    storage,
    StateKey.userInChat(new UserId("u-1"), new ChatId("c-1"))
);

fsm.setState("checkout.email").toCompletableFuture().join();
fsm.updateData(java.util.Map.of("email", "user@example.com")).toCompletableFuture().join();
```

## Client configuration

`MaxApiClientConfig` поддерживает builder-style конфигурацию:
- `baseUrl` / `baseUri` (по умолчанию `https://api.max.ru`);
- `token` (обязательный);
- `connectTimeout`, `readTimeout`;
- `userAgent`;
- `retryPolicy` (консервативный retry hook);
- `rateLimiter` (легковесный hook для client-side pacing).

Пример:

```java
import java.time.Duration;
import ru.max.botframework.client.MaxApiClientConfig;
import ru.max.botframework.client.RequestRateLimiter;
import ru.max.botframework.client.RetryPolicy;

MaxApiClientConfig config = MaxApiClientConfig.builder()
    .baseUrl("https://api.max.ru")
    .token("YOUR_BOT_TOKEN")
    .connectTimeout(Duration.ofSeconds(5))
    .readTimeout(Duration.ofSeconds(30))
    .userAgent("my-max-bot/1.0")
    .retryPolicy(RetryPolicy.fixed(2, Duration.ofMillis(200)))
    .rateLimiter(RequestRateLimiter.cooldown(Duration.ofMillis(300)))
    .build();
```

## Supported operations (Sprint 1)

`MaxBotClient` сейчас предоставляет:
- generic execution: `execute(MaxRequest<T>)` + `executeAsync(...)`;
- bot info: `getMe`, `getMeAsync`;
- messages:
  - `sendMessage`, `sendMessageAsync`;
  - `editMessage`, `editMessageAsync`;
  - `deleteMessage`, `deleteMessageAsync`;
  - `getMessage`, `getMessageAsync`;
  - `getMessages(ChatId)` и `getMessages(List<MessageId>)`;
- callbacks:
  - `answerCallback`, `answerCallbackAsync`;
- updates/polling foundation:
  - `getUpdates`, `getUpdatesAsync` (`marker`, `timeout`, `limit`, `types`);
- webhook subscriptions foundation:
  - `getSubscriptions`;
  - `createSubscription`, `createSubscriptionAsync`;
  - `deleteSubscription`, `deleteSubscriptionAsync`.

Сопутствующие foundation-возможности:
- HTTP transport: GET/POST/PUT/PATCH/DELETE;
- centralized JSON serialization (`JacksonJsonCodec` + shared mapper);
- auth interceptor (`Authorization` header);
- typed error hierarchy + structured MAX error payload;
- marker-based pagination abstractions;
- retry policy hook + rate-limit awareness (`429`, `Retry-After`).

## Current limitations

Ограничения текущего этапа:
- rich filter DSL ещё не реализован (доступен только базовый filter contract);
- middleware встроены в dispatcher pipeline (`outer -> filters -> inner -> handler`), но без advanced inheritance/scoping;
- DI/invocation есть, но resolution сейчас by-type (без annotation qualifiers);
- FSM/scenes runtime ещё не реализованы;
- Spring Boot starter и testkit пока на уровне скелетов модулей;
- upload/media layer реализован, но остаются ограничения:
  - нет helper слоя для `GET /videos/{videoToken}`;
  - нет auto-transcoding/media-processing subsystem;
  - нет persistent/distributed resume storage для resumable uploads;
- webhook source runtime loop пока не реализован (есть receiver + pipeline foundation);
- surface MAX API покрыт частично и будет расширяться в следующих спринтах.

## Sprint 4 Summary

Sprint 4 завершён:
- реализован filter layer (`Filter`, `FilterResult`, `BuiltInFilters`, `and/or/not`);
- filters встроены в handler matching с first-match semantics;
- реализован middleware layer (`OuterMiddleware`, `InnerMiddleware`) и runtime order
  `outer -> filters -> inner -> handler`;
- реализован request-scoped context enrichment для filters/middleware/dispatch result;
- зафиксирована и реализована runtime error model для filter/middleware/enrichment/handler фаз;
- добавлены usage examples и regression safety net (unit + integration-style tests).

## Sprint 5 Summary

Sprint 5 завершён:
- введён invocation contract: `HandlerInvoker`, `HandlerParameterResolver`, `ResolverRegistry`;
- dispatch pipeline интегрирован с invocation engine (`outer -> filters -> inner -> invocation`);
- реализованы built-in resolvers для runtime/update/event objects и enrichment/application data;
- добавлены APIs shared injection: `Dispatcher.registerApplicationData(...)`, `Dispatcher.registerService(...)`;
- зафиксирована и реализована error-модель DI/invocation:
  `PARAMETER_RESOLUTION_FAILURE` и `INVOCATION_FAILURE` + typed resolution exceptions;
- добавлены usage examples и regression safety net для resolver/invoker/full pipeline.

Sprint 6 завершён:
- `MessageTarget` abstraction (`ru.max.botframework.message.MessageTarget`) with `chat(...)` and `user(...)` targets.
- immutable `MessageBuilder` + `Messages` factory (`ru.max.botframework.message`) as high-level adapter над `NewMessageBody`/`SendMessageRequest`.
- `MessagingFacade` (`ru.max.botframework.message`) for high-level `send/edit/delete/reply` over existing `MaxBotClient`.
- inline keyboard model: `InlineKeyboard`, `KeyboardBuilder`, `Buttons`, `Keyboards.inline(...)`.
- `MessageBuilder.keyboard(...)` maps keyboard to low-level inline keyboard attachment.
- typed buttons API supports: `callback`, `link`, `requestContact`, `requestGeoLocation`, `openApp`, `message`.
- keyboard client-side validation (pre-SDK call):
  - max `30` rows;
  - max `7` buttons per row;
  - max `3` buttons per row for `link/openApp/requestGeoLocation/requestContact`;
  - max `210` buttons total;
  - max `2048` chars for `link` URL.
- callback high-level API: `CallbackFacade`, `CallbackContext`, `CallbackAnswers` for callback notification/update flows.
- chat actions high-level API: `ChatActionsFacade` with typed `ChatAction` dispatch and helpers (`typing`, `sendingPhoto`, ...).
- runtime integration: `Dispatcher.withBotClient(...)` injects messaging/callback/action APIs into handlers via
  `RuntimeContext` (`reply`, `answerCallback`, `chatAction`) and parameter resolution (`MessagingFacade`,
  `CallbackFacade`, `ChatActionsFacade`).
- Sprint 6 test coverage:
  - unit: message/keyboard/buttons/validation, callback answer builders/facade, chat actions facade/helpers;
  - integration-style: runtime handler -> `reply`, `answerCallback`, `chatAction`, plus reflective facade resolution.

Следующий этап (Sprint 8):
- FSMContext + storage abstraction;
- state scopes и `StateFilter`;
- Scene/SceneManager и minimal wizard API.

Sprint 7.1.2 implemented:
- unified `InputFile` API:
  - `InputFile.fromPath(Path.of("./invoice.png"))`
  - `InputFile.fromBytes(bytes, "report.pdf")`
  - `InputFile.fromStream(() -> stream, "voice.ogg")`
- optional metadata overrides:
  - `.withFileName(...)`
  - `.withContentType(...)`
- `knownSize()` поддерживает known/unknown size semantics (например, stream source без размера).

Sprint 7.1.3 implemented:
- staged upload orchestration contract:
  - `UploadService.upload(InputFile, UploadRequest)`;
  - preparation command/result (`UploadPrepareCommand`, `UploadPreparation`);
  - transfer/finalize results (`UploadTransferReceipt`, `UploadFinalizeResult`);
- final attachment-ready result model (`UploadResult`, `UploadRef`).
- clear separation:
  - orchestration logic (`DefaultUploadService`);
  - raw transfer execution (`UploadTransferGateway`);
  - prepare/finalize API gateways (`UploadPreparationGateway`, `UploadFinalizeGateway`);
  - result mapping (`UploadResultMapper`).

Sprint 7.2.1 implemented:
- multipart upload flow over existing orchestration:
  - `prepare` stage via existing `UploadPreparationGateway` (`POST /uploads` contract);
  - multipart transfer by prepared URL via `MultipartUploadTransferGateway`;
  - `filename`, `contentType` (`application/octet-stream` by default) and file bytes
    propagated to low-level multipart request model (`MultipartUploadRequest`);
  - upload request failures and non-2xx responses mapped to `UploadTransferException`.

Sprint 7.2.2 implemented:
- resumable upload flow over existing orchestration:
  - existing `UploadPreparationGateway` still defines flow via `UploadFlowType.RESUMABLE`;
  - transfer stage chunks `InputFile` into sequential chunk uploads via `ResumableUploadTransferGateway`;
  - retry semantics are per-chunk and configurable (`ResumableUploadOptions.maxRetriesPerChunk`);
  - resumable state is local to operation (current committed offset), without global persistent resume storage;
  - failures in chunk transfer / non-retryable responses / offset regression are mapped to `UploadTransferException`.

Sprint 7.2.3 implemented:
- upload results from multipart/resumable flows are normalized into one attachment-ready model:
  - `UploadFinalizeResult` now supports `mediaKind` and `attachmentPayload`;
  - `DefaultUploadResultMapper` projects both flow types into unified `UploadResult`;
  - model intentionally hides raw transport response details and keeps only data needed by future media builders.

Sprint 7.3.1 implemented:
- high-level media attachments over upload result model:
  - `ImageAttachment.from(uploadResult)`, `FileAttachment.from(uploadResult)`,
    `VideoAttachment.from(uploadResult)`, `AudioAttachment.from(uploadResult)`;
  - each abstraction maps to existing low-level attachment DTO (`NewMessageAttachment`) with
    proper `MessageAttachmentType` and `AttachmentInput(uploadRef=...)`;
  - `MessageBuilder` now accepts `attachment(MediaAttachment)` without breaking existing low-level APIs.

Sprint 7.3.2 implemented:
- high-level media send/reply facade:
  - `MediaMessagingFacade` hides explicit upload orchestration for common scenarios;
  - upload flow: `UploadService.upload(...)` -> media attachment mapping -> `MessagingFacade.send/reply(...)`;
  - supported methods:
    - `sendImage/sendFile/sendVideo/sendAudio`;
    - `replyImage/replyFile/replyVideo/replyAudio`.

Sprint 7.3.3 implemented:
- token-aware media flow for `video/audio`:
  - `UploadResult` now exposes `mediaTokenOptional()` for token-aware media kinds;
  - known payload keys are centralized in `UploadPayloadKeys` (`videoToken`, `audioToken`, `token`);
  - `VideoAttachment`/`AudioAttachment` map token to outgoing attachment reference with fallback to `uploadRef`;
  - `ImageAttachment`/`FileAttachment` continue using regular `uploadRef` flow.
- `GET /videos/{videoToken}` helper layer is intentionally not added at this stage to keep Sprint 7 scope focused
  on send-path orchestration and attachment mapping.

Sprint 7.3.4 implemented:
- media layer integrated into runtime-facing APIs:
  - `Dispatcher.withUploadService(...)` registers upload service for runtime media ergonomics;
  - `RuntimeContext.media()` exposes `MediaMessagingFacade` when both bot client and upload service are configured;
  - convenience runtime shortcuts:
    - `context.replyImage(...)`, `context.replyFile(...)`;
    - `context.sendVideo(...)`, `context.sendAudio(...)`.
- parameter resolution integration:
  - reflective handlers can now resolve `MediaMessagingFacade` parameter directly.
- builder/media composition remains first-class:
  - `Messages.text(...).attachment(MediaAttachment.image(uploaded))` works naturally in runtime handlers.

Handler example:

```java
router.message((message, context) -> {
    context.replyImage(InputFile.fromPath(Path.of("./preview.jpg")));
    context.sendVideo(InputFile.fromPath(Path.of("./clip.mp4")));
    return CompletableFuture.completedFuture(null);
});
```

Sprint 7 test coverage:
- unit:
  - `InputFile` sources and metadata behavior;
  - upload orchestration (`UploadService`) wiring and failure boundaries;
  - multipart flow (`MultipartUploadTransferGateway`, `JdkMultipartUploadHttpClient`);
  - resumable flow (`ResumableUploadTransferGateway`, chunk retry/offset semantics);
  - upload result normalization (`UploadResultMapper`, `UploadResultTokenTest`);
  - media attachment mapping (`MediaAttachmentTest`);
  - runtime media resolver coverage (`RuntimeMessagingFacadeParameterResolverTest`).
- integration-style:
  - `upload + send` and `upload + reply` (`MediaMessagingFacadeTest`);
  - runtime handler -> media API (`DispatcherRuntimeMessagingIntegrationTest`);
  - builder + media attachment composition (`MessageBuilderTest` + dispatcher integration composition probe).

Example:

```java
UploadResult uploadedImage = ...;
UploadResult uploadedDoc = ...;

MessageBuilder message = Messages.text("Материалы готовы")
    .attachment(ImageAttachment.from(uploadedImage).caption("Превью"))
    .attachment(FileAttachment.from(uploadedDoc).caption("Документ"));

MediaMessagingFacade media = new MediaMessagingFacade(uploadService, messagingFacade);
media.sendImage(new ChatId("chat-1"), InputFile.fromPath(Path.of("./photo.jpg")));
media.replyVideo(sourceMessage, InputFile.fromPath(Path.of("./clip.mp4")), "Видео-ответ");
```

Пример:

```java
MessageTarget chatTarget = MessageTarget.chat(new ChatId("chat-1"));
MessageTarget userTarget = MessageTarget.user(new UserId("user-1"));

// low-level SDK send/edit path expects ChatId
ChatId chatId = userTarget.toChatId(userId -> resolveUserChat(userId));

SendMessageRequest request = Messages.text("Привет")
    .format(TextFormat.MARKDOWN)
    .notify(false)
    .link("https://example.com")
    .toSendRequest(chatId);

SendMessageRequest md = Messages.markdown("*Привет*")
    .toSendRequest(chatId);

SendMessageRequest html = Messages.html("<b>Привет</b>")
    .toSendRequest(chatId);

MessagingFacade messaging = new MessagingFacade(botClient, userId -> resolveUserChat(userId));
Message sent = messaging.send(chatTarget, Messages.text("Привет").markdown());
Message reply = messaging.reply(sent, Messages.html("<b>Принято</b>"));
boolean edited = messaging.edit(sent, Messages.text("Обновлено"));
boolean deleted = messaging.delete(sent);

InlineKeyboard keyboard = Keyboards.inline(k -> k
    .row(
        Buttons.callback("Оплатить", "pay:1"),
        Buttons.link("Сайт", "https://example.com"),
        Buttons.openApp("Открыть mini app", "app:orders")
    )
    .row(
        Buttons.requestContact("Отправить контакт"),
        Buttons.requestGeoLocation("Отправить геолокацию"),
        Buttons.message("Сообщение", "Привет из кнопки")
    )
);

Message withKeyboard = messaging.send(
    chatTarget,
    Messages.text("Выберите действие").keyboard(keyboard)
);

CallbackFacade callbacks = new CallbackFacade(botClient);
router.callback(cb -> {
    CallbackContext ctx = callbacks.context(cb);
    ctx.answer("Оплата принята");
    ctx.updateCurrentMessage(Messages.text("Статус: оплачено"));
    return java.util.concurrent.CompletableFuture.completedFuture(null);
});

ChatActionsFacade actions = new ChatActionsFacade(botClient);
actions.typing(new ChatId("chat-1"));
actions.sendingPhoto(new ChatId("chat-1"));

Dispatcher dispatcher = new Dispatcher().withBotClient(botClient);
Router router = new Router("runtime");
router.message((message, ctx) -> {
    ctx.reply(Messages.text("Pong"));
    ctx.chatAction(ChatAction.TYPING);
    return java.util.concurrent.CompletableFuture.completedFuture(null);
});
router.callback((callback, ctx) -> {
    ctx.answerCallback("OK");
    return java.util.concurrent.CompletableFuture.completedFuture(null);
});
dispatcher.includeRouter(router);
```

## Sprint 6 Messaging Examples

- `examples/sprint-6-messaging/README.md`
- `examples/sprint-6-messaging/MessagingFacadeExample.java`
- `examples/sprint-6-messaging/RuntimeMessagingHandlersExample.java`

Кейсы в примерах:
- send/edit/delete/reply;
- formatted text (`plain/markdown/html`);
- keyboard builder + typed buttons;
- callback answer;
- chat actions;
- runtime usage inside handlers (`RuntimeContext` shortcuts + facade parameter resolution).

## Sprint 7 Upload/Media Examples

- `examples/sprint-7-upload-media/README.md`
- `examples/sprint-7-upload-media/MediaFacadeExample.java`
- `examples/sprint-7-upload-media/RuntimeMediaHandlersExample.java`

Кейсы в примерах:
- `InputFile.fromPath(...)`;
- `InputFile.fromBytes(...)`;
- `sendImage(...)` и `sendFile(...)`;
- `replyVideo(...)` и `replyAudio(...)`;
- media API внутри handler-а (`RuntimeContext` shortcuts + `MediaMessagingFacade` resolver);
- builder + media attachment composition.

Минимальный фрагмент:

```java
InputFile image = InputFile.fromPath(Path.of("./assets/photo.jpg"));
InputFile doc = InputFile.fromBytes(loadPdfBytes(), "invoice.pdf");

MediaMessagingFacade media = new MediaMessagingFacade(uploadService, messagingFacade);
media.sendImage(new ChatId("chat-1"), image);
media.sendFile(new ChatId("chat-1"), doc, "Счёт");
media.replyVideo(sourceMessage, InputFile.fromPath(Path.of("./assets/clip.mp4")), "Видео");
media.replyAudio(sourceMessage, InputFile.fromBytes(loadAudioBytes(), "voice.mp3"), "Аудио");

UploadResult uploaded = uploadService.upload(image).toCompletableFuture().join();
messagingFacade.send(
    new ChatId("chat-1"),
    Messages.text("Материалы").attachment(MediaAttachment.image(uploaded).caption("Превью"))
);
```

## Shared Services Injection (Sprint 5.2.3)

```java
Dispatcher dispatcher = new Dispatcher();
dispatcher.registerService(OrderService.class, new OrderService());

Router router = new Router("orders");
Method method = OrderHandlers.class.getDeclaredMethod(
    "onMessage",
    Message.class,
    OrderService.class
);
router.message(new OrderHandlers(), method);
dispatcher.includeRouter(router);
```

Правила:
- shared objects регистрируются явно на уровне `Dispatcher`;
- resolution идёт по типу параметра (`ApplicationDataParameterResolver`);
- framework не управляет lifecycle сервисов и не делает IoC/scopes.

Пример mixed handler signature через invocation engine:

```java
public CompletionStage<Void> onMessage(
    Message message,
    Update update,
    User user,
    Chat chat,
    RuntimeContext context,
    String textSuffix,   // из BuiltInFilters.textStartsWith(...)
    Integer attempt,     // из middleware enrichment
    OrderService service // из Dispatcher.registerService(...)
) { ... }
```

## Sprint 5 DI Examples

- `examples/sprint-5-di-invocation/README.md`
- `examples/sprint-5-di-invocation/HandlerDiExample.java`

Ключевые сценарии в примере:
- core runtime/update параметры в handler signature (`Message`, `Update`, `User`, `Chat`);
- filter-derived parameter (`String` suffix из `BuiltInFilters.textStartsWith(...)`);
- middleware-derived parameter (`Integer` enrichment);
- custom shared service (`Dispatcher.registerService(...)`);
- combined pipeline (`outer -> filters -> invocation`) на одном update.

## Sprint 2 Summary

- реализован transport-level ingestion для polling и webhook;
- polling и webhook сведены к единому `UpdatePipeline`;
- зафиксированы marker strategy, lifecycle/shutdown semantics и overload control;
- добавлены integration-style fixtures/tests как regression safety net перед Sprint 3.

## Sprint 3 Summary

- реализован runtime orchestration слой: `Dispatcher` + `Router` + observer model;
- поддержан router tree (`includeRouter`) с deterministic DFS propagation и first-match semantics;
- добавлен dispatch result/error model (`HANDLED`/`IGNORED`/`FAILED`, typed runtime error categories);
- реализован `feedUpdate()` pipeline и event mapping layer (`UpdateEventResolver`);
- `Dispatcher` интегрирован с ingestion layer как `UpdateConsumer` (и `asUpdateSink` adapter для legacy path);
- добавлены unit и integration-style тесты runtime + ingestion linkage как Sprint 3 regression safety net.

## Low-level Long Polling Example

```java
import java.time.Duration;
import ru.max.botframework.ingestion.DefaultLongPollingRunner;
import ru.max.botframework.ingestion.LongPollingRunner;
import ru.max.botframework.ingestion.LongPollingRunnerConfig;
import ru.max.botframework.ingestion.PollingFetchRequest;
import ru.max.botframework.ingestion.SdkPollingUpdateSource;
import ru.max.botframework.ingestion.UpdateConsumer;
import ru.max.botframework.model.UpdateEventType;

SdkPollingUpdateSource source = new SdkPollingUpdateSource(botClient);
UpdateConsumer sink = update -> {
    System.out.println("Update: " + update.updateId().value());
    return java.util.concurrent.CompletableFuture.completedFuture(
        ru.max.botframework.ingestion.UpdateHandlingResult.success()
    );
};

LongPollingRunner runner = new DefaultLongPollingRunner(
    source,
    sink,
    LongPollingRunnerConfig.builder()
        .request(new PollingFetchRequest(
            null,
            30,
            100,
            java.util.List.of(UpdateEventType.MESSAGE_CREATED, UpdateEventType.MESSAGE_CALLBACK)
        ))
        .idleDelay(Duration.ofMillis(100))
        .sourceErrorDelay(Duration.ofSeconds(1))
        .sinkErrorDelay(Duration.ofMillis(200))
        .build()
);

runner.start();
// ...
runner.stop();
```

## Marker Strategy (Long Polling)

- marker хранится внутри runner через `PollingMarkerState` (по умолчанию in-memory);
- marker продвигается только после успешной обработки всего полученного batch;
- при sink/source ошибках marker не двигается, чтобы сохранить at-least-once доставку;
- marker не регрессирует даже если source вернул более старое значение.

## Webhook Secret Validation

- заголовок секрета: `X-Max-Bot-Api-Secret`;
- контракт: `WebhookSecretValidator.validate(String secretHeader)`;
- outcomes:
- `ACCEPTED`
- `SKIPPED_NO_SECRET_CONFIGURED`
- `REJECTED` (`SECRET_HEADER_MISSING` или `SECRET_MISMATCH`).

## Unified Ingestion Pipeline

- polling runner и webhook receiver используют один transport-level contract: `UpdatePipeline`;
- стандартная реализация: `DefaultUpdatePipeline`, которая делегирует в `UpdateConsumer` (`UpdateSink` alias);
- контекст источника фиксируется через `UpdatePipelineContext` (`POLLING` / `WEBHOOK`);
- расширение для observability: `UpdatePipelineHook` (`onBefore` / `onAfter`).

## Lifecycle and Shutdown

- `DefaultLongPollingRunner.stop()` — graceful stop цикла polling без финального закрытия компонентов;
- `DefaultLongPollingRunner.shutdown()` — финальная остановка и cleanup ресурсов по `LongPollingRunnerConfig`;
- ownership lifecycle управляется конфигом:
- `closeExecutorOnShutdown`
- `closeSourceOnShutdown`
- `shutdownTimeout`

## Webhook Overload Control

- receiver использует `WebhookReceiverConfig(maxInFlightRequests)`;
- при достижении лимита возвращается `WebhookReceiveStatus.OVERLOADED`;
- это lightweight backpressure на transport-уровне без reactive engine.

## Ingestion Integration Tests

- polling chain coverage: `polling -> SdkPollingUpdateSource -> DefaultLongPollingRunner -> UpdateConsumer`;
- webhook chain coverage: `webhook request -> DefaultWebhookReceiver -> secret validation -> UpdateConsumer`;
- error coverage:
- webhook payload deserialization errors (`BAD_PAYLOAD`);
- sink failures (`INTERNAL_ERROR`);
- fixtures path: `max-dispatcher/src/test/resources/fixtures/ingestion`.

## Sprint 2 Examples

- low-level examples directory: `examples/sprint-2-low-level`;
- files:
- `LongPollingExample.java`
- `WebhookHandlingExample.java`

## Sprint 3 Examples

- runtime examples directory: `examples/sprint-3-runtime`;
- files:
- `DispatcherRouterExample.java` (dispatcher/router handlers + includeRouter + feedUpdate);
- `DispatcherIngestionIntegrationExample.java` (dispatcher as ingestion target for polling/webhook).

## Sprint 4 Examples

- filters/middleware examples directory: `examples/sprint-4-filters-middleware`;
- files:
- `FiltersMiddlewareExample.java`:
  - built-in filters на handler registration;
  - filter composition (`and`);
  - `outerMiddleware` + `innerMiddleware`;
  - context enrichment (`putEnrichment` / `enrichmentValue`);
  - router tree (`includeRouter`).

## Router Registration (Sprint 3 foundation)

```java
import java.util.concurrent.CompletableFuture;
import ru.max.botframework.dispatcher.Router;

Router router = new Router("main")
    .update(update -> {
        System.out.println("Any update: " + update.updateId().value());
        return CompletableFuture.completedFuture(null);
    })
    .message(message -> {
        System.out.println("Message text: " + message.text());
        return CompletableFuture.completedFuture(null);
    })
    .callback(callback -> CompletableFuture.completedFuture(null))
    .error(error -> {
        error.error().printStackTrace();
        return CompletableFuture.completedFuture(null);
    });

Router admin = new Router("admin")
    .message(message -> CompletableFuture.completedFuture(null));

router.includeRouter(admin);

router
    .message(BuiltInFilters.command("start"), message -> CompletableFuture.completedFuture(null))
    .message(BuiltInFilters.textStartsWith("pay:"), message -> CompletableFuture.completedFuture(null))
    .callback(BuiltInFilters.fromCallbackUser(new UserId("u-1")), callback -> CompletableFuture.completedFuture(null));
```

## Dispatcher Role (Sprint 3 foundation)

`Dispatcher` — корневой runtime orchestrator над root routers.

- хранит root routing graph (`includeRouter`, `includeRouters`);
- даёт единую dispatch entrypoint: `feedUpdate(Update) -> DispatchResult`;
- реализует ingestion boundary `UpdateConsumer` через `handle(Update)`.

```java
Dispatcher dispatcher = new Dispatcher()
    .outerMiddleware((ctx, next) -> next.proceed())
    .includeRouter(router);

router.innerMiddleware((ctx, next) -> next.proceed());

DispatchResult result = dispatcher.feedUpdate(update).toCompletableFuture().join();
```

Minimal filter + middleware usage:

```java
ContextKey<String> traceId = ContextKey.of("traceId", String.class);

Router root = new Router("root");
Router payments = new Router("payments");

payments.message(
    BuiltInFilters.chatType(ChatType.PRIVATE).and(BuiltInFilters.textStartsWith("pay:")),
    message -> CompletableFuture.completedFuture(null)
);
payments.innerMiddleware((ctx, next) -> {
    String trace = ctx.enrichmentValue(traceId).orElse("trace-missing");
    String suffix = ctx.enrichmentValue(BuiltInFilters.TEXT_SUFFIX_KEY, String.class).orElse("n/a");
    System.out.println(trace + " -> " + suffix);
    return next.proceed();
});
root.includeRouter(payments);

Dispatcher dispatcher = new Dispatcher()
    .outerMiddleware((ctx, next) -> {
        ctx.putEnrichment(traceId, "trace-" + ctx.update().updateId().value());
        return next.proceed();
    })
    .includeRouter(root);
```

Sprint 5 handler signature model (current):

```java
router.message((Message message, RuntimeContext ctx) -> {
    String suffix = ctx.enrichmentValue(BuiltInFilters.TEXT_SUFFIX_KEY, String.class).orElse("n/a");
    return CompletableFuture.completedFuture(null);
});

router.callback((Callback callback, User actor, Chat chat, RuntimeContext ctx) -> {
    return CompletableFuture.completedFuture(null);
});

// Reflective handler method can receive filter-derived String (e.g. text suffix from pay:123)
// when resolver chain can unambiguously resolve by type.
```

Event mapping strategy:
- всегда вызывается generic `update` observer;
- затем `UpdateEventResolver` маппит update в `message`/`callback`/`unsupported`;
- fallback: если type не распознан, но payload содержит `message` или `callback`, используется соответствующий observer;
- `unsupported` update без подходящего payload даёт `DispatchResult.IGNORED`.

First-match and propagation rules:
- `HANDLED` возникает на первом успешно выполненном handler;
- при `HANDLED` дальнейший поиск по текущему observer/router tree/root routers прекращается;
- обход идёт deterministic: root routers в порядке include, внутри root — DFS pre-order по router tree;
- если handler не найден во всей цепочке, результат `DispatchResult.IGNORED`;
- если handler падает, результат `DispatchResult.FAILED`, и вызывается `error` observer текущего router.
- при регистрации handler с filter учитываются только handler-ы с `FilterResult.MATCHED`;
- filter `NOT_MATCHED` оставляет dispatch в поиске следующего handler-а по first-match правилам;
- context enrichment объединяет данные filters и middleware в одном request-scoped runtime context;
- middleware может писать enrichment через `RuntimeContext.putEnrichment(...)`;
- конфликт enrichment key с разными значениями приводит к `EnrichmentConflictException` и runtime `FAILED`;
- итоговый `DispatchResult.enrichment()` возвращает merged enrichment текущего handled path.

Runtime error boundary:
- runtime dispatch ошибки классифицируются как:
  `FILTER_FAILURE`, `OUTER_MIDDLEWARE_FAILURE`, `INNER_MIDDLEWARE_FAILURE`,
  `ENRICHMENT_FAILURE`, `PARAMETER_RESOLUTION_FAILURE`, `INVOCATION_FAILURE`,
  `HANDLER_FAILURE`, `EVENT_MAPPING_FAILURE`, `OBSERVER_EXECUTION_FAILURE`;
- все они передаются в `error` observer текущего router как `ErrorEvent`;
- исключение: outer middleware failure на уровне dispatcher отправляется в `error` observer первого root router
  (предпочтительно первого root router с зарегистрированным error handler);
- даже при успешном `error` handler итог dispatch остаётся `FAILED`;
- если `error` handler сам падает, его ошибка добавляется в `suppressed` исходной runtime ошибки.

DI/invocation-specific behavior:
- unresolved core/runtime parameter -> `UnsupportedHandlerParameterException`;
- missing required service/data -> `MissingHandlerDependencyException`;
- ambiguous typed resolution -> `ParameterResolutionException` (`AMBIGUOUS_RESOLUTION`);
- resolver thrown exception -> `ParameterResolutionException` (`RESOLVER_FAILURE`);
- invalid reflective method contract/access issues -> `ReflectiveInvocationException`.

Dispatcher <-> ingestion integration:
- `Dispatcher` реализует `UpdateConsumer` и может быть передан напрямую в:
- `DefaultLongPollingRunner(source, dispatcher, config)`
- `DefaultWebhookReceiver(secretValidator, jsonCodec, dispatcher)`
- для backward-compatible ingestion API доступен адаптер `dispatcher.asUpdateSink()`.

Sprint 3 runtime test coverage:
- unit: observer registration, router composition, dispatch results, event mapping, first-match semantics, error observer/error boundary;
- integration-style: router tree dispatch propagation и ingestion linkage (`polling -> runner -> dispatcher`, `webhook -> receiver -> dispatcher`);
- reused fixtures: `max-dispatcher/src/test/resources/fixtures/ingestion`.

Sprint 5 DI/invocation test coverage:
- unit: `ResolverRegistry`, built-in parameter resolvers, enrichment resolvers, application-data resolver,
  shared service registration/injection, `DefaultHandlerInvoker`, resolution/invocation failure scenarios;
- integration-style: `dispatcher + router + filters + middleware + invocation`,
  mixed multi-source parameter signatures, error observer propagation for resolution failures.

## Low-level Webhook Handling Example

```java
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import ru.max.botframework.client.serialization.JacksonJsonCodec;
import ru.max.botframework.ingestion.DefaultWebhookReceiver;
import ru.max.botframework.ingestion.DefaultWebhookSecretValidator;
import ru.max.botframework.ingestion.UpdateHandlingResult;
import ru.max.botframework.ingestion.UpdateConsumer;
import ru.max.botframework.ingestion.WebhookReceiveResult;
import ru.max.botframework.ingestion.WebhookReceiveStatus;
import ru.max.botframework.ingestion.WebhookRequest;

UpdateConsumer sink = update -> CompletableFuture.completedFuture(UpdateHandlingResult.success());

DefaultWebhookReceiver receiver = new DefaultWebhookReceiver(
    new DefaultWebhookSecretValidator("my-secret"),
    new JacksonJsonCodec(),
    sink
);

WebhookRequest request = new WebhookRequest(
    rawBodyBytes,
    Map.of("X-Max-Bot-Api-Secret", List.of(secretFromHttpHeader))
);

WebhookReceiveResult result = receiver.receive(request).toCompletableFuture().join();
if (result.status() == WebhookReceiveStatus.ACCEPTED) {
    // return HTTP 200
} else if (result.status() == WebhookReceiveStatus.INVALID_SECRET) {
    // return HTTP 401/403
} else if (result.status() == WebhookReceiveStatus.BAD_PAYLOAD) {
    // return HTTP 400
} else if (result.status() == WebhookReceiveStatus.OVERLOADED) {
    // return HTTP 429/503
} else {
    // return HTTP 500
}
```

## Build and test

Требование: JDK 21+

```bash
./gradlew clean test
```

## Documentation

- Product vision and target DX: [docs/product-spec.md](docs/product-spec.md)
- Core API contract: [docs/api-contract.md](docs/api-contract.md)
- Runtime contract (Sprint 3): [docs/runtime-contract.md](docs/runtime-contract.md)
- Filters/Middleware contract (Sprint 4): [docs/filters-and-middleware.md](docs/filters-and-middleware.md)
- DI and invocation contract (Sprint 5): [docs/di-and-invocation.md](docs/di-and-invocation.md)
- Event model: [docs/event-model.md](docs/event-model.md)
- Update ingestion contract (Sprint 2): [docs/update-ingestion.md](docs/update-ingestion.md)
- Roadmap: [docs/roadmap.md](docs/roadmap.md)
- Contributing workflow: [docs/contributing.md](docs/contributing.md)
- ADR index: [docs/adr/README.md](docs/adr/README.md)

## Source of truth

- MAX API docs: [https://dev.max.ru/docs-api](https://dev.max.ru/docs-api)
- MAX methods: [https://dev.max.ru/docs-api/methods](https://dev.max.ru/docs-api/methods)
- MAX objects: [https://dev.max.ru/docs-api/objects](https://dev.max.ru/docs-api/objects)
- aiogram 3 reference: [https://docs.aiogram.dev/](https://docs.aiogram.dev/)
