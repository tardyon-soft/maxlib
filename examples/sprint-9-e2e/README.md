# Sprint 9 E2E Examples

Набор небольших end-to-end примеров на текущем публичном API framework.

## Что внутри

1. `SpringPollingBotApplication.java`
   - минимальный Spring Boot bot в polling mode.
2. `SpringWebhookBotApplication.java`
   - минимальный Spring Boot bot в webhook mode.
3. `RouterCompositionExample.java`
   - includeRouter и модульная композиция router tree.
4. `FiltersMiddlewareDiExample.java`
   - filters + outer/inner middleware + DI/parameter resolution.
5. `MessagingCallbacksExample.java`
   - high-level messages/keyboards/callbacks/actions.
6. `FsmScenesExample.java`
   - FSMContext + StateFilter + Scene/Wizard flow.
7. `MediaUploadExample.java`
   - upload/media facade (`sendImage/replyVideo/sendAudio`).

## Spring properties examples

- `application-polling.yml`
- `application-webhook.yml`

## Notes

- Примеры intentionally минимальны и ориентированы на quick start.
- Плейсхолдеры `UnsupportedOperationException` показывают integration points, где нужно подставить реальную инфраструктуру приложения.
