# MAX API Coverage Matrix

Актуально для текущего `MaxBotClient` после добавления docs-совместимого `*Api` слоя.

## Legend

- `EXACT` — endpoint покрыт docs-совместимым методом (path/query/body/shape) в `*Api`.
- `LEGACY` — endpoint покрыт старым нормализованным методом (может отличаться shape ответа/запроса).
- `PARTIAL` — endpoint есть, но только частично по docs-контракту.

## Methods from dev.max.ru docs

| HTTP | Endpoint | Library method | Status | Notes |
|---|---|---|---|---|
| GET | `/me` | `getMeApi()` | EXACT | `getMe()` остается LEGACY sugar |
| GET | `/chats` | `getChatsApi(GetChatsApiRequest)` | EXACT | `count`, `marker` |
| POST | `/answers` | `answerCallback(AnswerCallbackRequest)` | EXACT | маппинг на `callback_id` query + `message/notification` body |
| GET | `/videos/{videoToken}` | `getVideoApi(String)` | EXACT | transport response |
| GET | `/messages/{messageId}` | `getMessageApi(MessageId)` | EXACT | `getMessage()` остается LEGACY sugar |
| DELETE | `/messages` | `deleteMessage(MessageId)` | LEGACY | контракт совпадает по query `message_id` |
| PUT | `/messages` | `editMessage(EditMessageRequest)` | LEGACY | работает через нормализованный DTO |
| POST | `/messages` | `sendMessageApi(SendMessageApiRequest)` | EXACT | поддержаны `user_id/chat_id`, `disable_link_preview` |
| GET | `/messages` | `getMessagesApi(GetMessagesApiRequest)` | EXACT | `chat_id`, `from`, `to`, `count` |
| POST | `/uploads` | `prepareUploadApi(PrepareUploadApiRequest)` | EXACT | |
| GET | `/updates` | `getUpdatesApi(GetUpdatesRequest)` | EXACT | `getUpdates()` остается LEGACY sugar |
| DELETE | `/subscriptions` | `deleteSubscription(DeleteSubscriptionRequest)` | LEGACY | соответствует по `url` |
| POST | `/subscriptions` | `createSubscription(CreateSubscriptionRequest)` | LEGACY | |
| GET | `/subscriptions` | `getSubscriptions()` | LEGACY | |
| DELETE | `/chats/{chatId}/members` | `removeChatMemberApi(chatId, RemoveChatMemberApiRequest)` | EXACT | `user_id`, `block` |
| POST | `/chats/{chatId}/members` | `addChatMembersApi(chatId, AddChatMembersApiRequest)` | EXACT | `user_ids` |
| GET | `/chats/{chatId}/members` | `getChatMembersApi(chatId, GetChatMembersApiRequest)` | EXACT | `user_ids`, `marker`, `count` |
| DELETE | `/chats/{chatId}/members/admins/{userId}` | `removeChatAdminApi(chatId, userId)` | EXACT | |
| POST | `/chats/{chatId}/members/admins` | `addChatAdminsApi(chatId, AddChatAdminsApiRequest)` | EXACT | |
| GET | `/chats/{chatId}/members/admins` | `getChatAdminsApi(chatId)` | EXACT | |
| DELETE | `/chats/{chatId}/members/me` | `leaveChatApi(chatId)` | EXACT | |
| GET | `/chats/{chatId}/members/me` | `getMyChatMembershipApi(chatId)` | EXACT | |
| DELETE | `/chats/{chatId}/pin` | `unpinChatMessageApi(chatId)` | EXACT | |
| PUT | `/chats/{chatId}/pin` | `pinChatMessageApi(chatId, PinChatMessageApiRequest)` | EXACT | `message_id`, `notify` |
| GET | `/chats/{chatId}/pin` | `getChatPinApi(chatId)` | EXACT | |
| POST | `/chats/{chatId}/actions` | `sendChatAction(ChatId, SendChatActionRequest)` | LEGACY | endpoint совпадает |
| DELETE | `/chats/{chatId}` | `deleteChatApi(chatId)` | EXACT | |
| PATCH | `/chats/{chatId}` | `patchChatApi(chatId, UpdateChatApiRequest)` | EXACT | `icon`, `title`, `pin`, `notify` |
| GET | `/chats/{chatId}` | `getChatApi(chatId)` | EXACT | |

## Remaining caveats

- Для старых `LEGACY` методов shape DTO остается нормализованным (это намеренно, для обратной совместимости).
- Для новых `*Api` методов рекомендуется использовать transport DTO из `max-model/transport`.
