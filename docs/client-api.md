# Работа с MaxBotClient и upload API

## MaxBotClient

`MaxBotClient` - typed entrypoint для вызова MAX API.

Из интерфейса доступны группы операций:

- информация о боте: `getMe()`, `getMeApi()`
- сообщения: `sendMessage(...)`, `editMessage(...)`, `deleteMessage(...)`, `getMessage(...)`, `getMessages(...)`
- callback: `answerCallback(...)`
- chat actions: `sendChatAction(...)`
- updates: `getUpdates(...)`, `getUpdatesApi(...)`
- чаты: `getChatsApi(...)`, `getChatApi(...)`, `patchChatApi(...)`, `deleteChatApi(...)`, `leaveChatApi(...)`
- участники и админы чата: `getChatMembersApi(...)`, `addChatMembersApi(...)`, `removeChatMemberApi(...)`, `getChatAdminsApi(...)`, `addChatAdminsApi(...)`, `removeChatAdminApi(...)`
- pin: `getChatPinApi(...)`, `pinChatMessageApi(...)`, `unpinChatMessageApi(...)`
- uploads: `prepareUploadApi(...)`, `getVideoApi(...)`
- подписки: `getSubscriptions()`, `createSubscription(...)`, `deleteSubscription(...)`

У большинства методов есть и sync, и async варианты.

## Создание клиента

```java
MaxApiClientConfig config = MaxApiClientConfig.builder()
        .token(System.getenv("MAX_BOT_TOKEN"))
        .baseUrl("https://platform-api.max.ru")
        .build();

OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .connectTimeout(config.connectTimeout())
        .readTimeout(config.readTimeout())
        .callTimeout(Duration.ZERO)
        .build();

MaxHttpClient transport = new OkHttpMaxHttpClient(config.baseUri(), okHttpClient);
JsonCodec jsonCodec = new JacksonJsonCodec();
MaxBotClient client = new DefaultMaxBotClient(config, transport, jsonCodec);
```

## Высокоуровневые фасады runtime-слоя

Если клиент уже подключен к `Dispatcher`, в обработчиках обычно удобнее использовать:

- `MessagingFacade`
- `CallbackFacade`
- `ChatActionsFacade`
- `MediaMessagingFacade`

Пример:

```java
ctx.reply(Messages.markdown("*Привет*"));
ctx.answerCallback("Готово");
ctx.chatAction(ChatAction.TYPING);
```

## Upload API

Upload-слой построен вокруг:

- `InputFile`
- `UploadService`
- `UploadRequest`
- `UploadResult`

Поддерживаемые источники файла:

- `InputFile.fromPath(...)`
- `InputFile.fromBytes(...)`
- `InputFile.fromStream(...)`

Пример создания `InputFile`:

```java
InputFile file = InputFile.fromPath(Path.of("/tmp/report.pdf"));
```

`UploadService` - это orchestration contract. Он собирается из трех частей:

- `UploadPreparationGateway`
- `UploadTransferGateway`
- `UploadFinalizeGateway`

Для transfer-слоя в коде есть реализации:

- `MultipartUploadTransferGateway`
- `ResumableUploadTransferGateway`

Для multipart HTTP есть `JdkMultipartUploadHttpClient`.

## Что важно про media runtime

`MediaMessagingFacade` и методы `ctx.media()`, `ctx.replyImage(...)`, `ctx.replyFile(...)`, `ctx.sendVideo(...)`, `ctx.sendAudio(...)` работают только если `Dispatcher` получил `UploadService`.

В Spring starter `MediaMessagingFacade` создается только при наличии bean `UploadService`. Готового upload bean по умолчанию starter не добавляет.
