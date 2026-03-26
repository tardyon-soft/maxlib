# Sprint 9 E2E Examples

End-to-end набор на актуальном публичном API.

## Что внутри

1. `SpringPollingBotApplication.java` — минимальный Spring Boot бот в polling mode.
2. `SpringWebhookBotApplication.java` — минимальный Spring Boot бот в webhook mode.
3. `RouterCompositionExample.java` — `includeRouter` и модульная композиция router tree.
4. `FiltersMiddlewareDiExample.java` — filters + middleware + DI invocation.
5. `MessagingCallbacksExample.java` — сообщения, клавиатуры, callbacks, chat actions.
6. `FsmScenesExample.java` — FSM + Scene/Wizard flow.
7. `MediaUploadExample.java` — upload/media facade.

## Конфиги

- `application-polling.yml`
- `application-webhook.yml`

## Актуальность

- Starter сам создает polling runner config (ручной bean обычно не требуется).
- Аннотационный API и screen API в этих e2e примерах не обязательны, чтобы сохранить минимальный baseline.
