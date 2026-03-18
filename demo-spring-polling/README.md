# Demo Spring Boot Polling App

Отдельное тестовое Spring Boot приложение внутри репозитория для ручной проверки MAX bot framework.

## Назначение

Этот demo показывает фактический текущий API framework без вымышленных слоёв:
- Spring Boot starter в `POLLING` режиме;
- `Dispatcher/Router` handlers;
- annotation routes (`@Route`, `@Command`, `@Callback`, `@State`, `@UseFilters`, `@UseMiddleware`);
- `@Route` auto-detect как Spring bean (без обязательного `@Component`);
- built-in filters;
- inline keyboard + callback handling;
- chat action (`typing`);
- FSM (`/form`) через `BuiltInFilters.state(...)` и `FSMContext`.
- smoke bot команды для ручной проверки MAX API (`/qa_*`).

## Структура

- `src/main/java/.../DemoSpringPollingApplication.java` — main app + router handlers.
- `src/main/java/.../AnnotatedMenuRoute.java` — demo route на annotation API для команд/callback/filter/middleware.
- `src/main/java/.../AnnotatedFormRoute.java` — demo route на annotation API для FSM state flow.
- `src/main/java/.../ApiSmokeRoute.java` — чат-команды smoke проверки API.
- `src/main/java/.../ApiSmokeService.java` — шаги smoke run и сводный отчёт.
- `src/main/resources/application.yml` — конфигурация через properties/env.
- `src/test/java/.../DemoSpringPollingApplicationSmokeTest.java` — smoke test поднятия контекста.

## Конфигурация

Токен не захардкожен и берётся из env:

```bash
export MAX_BOT_TOKEN=<your-max-bot-token>
export DEMO_SMOKE_DESTRUCTIVE=false
export DEMO_SMOKE_WEBHOOK_URL=https://example.com/max-webhook
```

`application.yml` использует:

- `max.bot.token: ${MAX_BOT_TOKEN:}`
- `max.bot.mode: POLLING`
- polling `limit/timeout/types`.
- `demo.smoke.destructive` — включение потенциально разрушительных шагов (`DELETE /messages`, chat mutation methods).
- `demo.smoke.webhook-url` — URL для тестов `POST/DELETE /subscriptions`.

## Запуск

Из корня репозитория:

```bash
./gradlew :demo-spring-polling:run
```

## Команды в demo

- `/start` — приветствие и список команд.
- `/menu` — сообщение с inline keyboard.
- callback `menu:pay`, `menu:help` — ответ на callback и update текущего сообщения.
- `/typing` — отправка chat action `typing`.
- `/form` — старт FSM flow (ожидание имени), затем сохранение имени и завершение.
- любое другое сообщение — echo reply.

Аннотационный API (новый sugar):
- `/astart` — приветствие для annotation routes.
- `/amenu` — inline keyboard (callback `amenu:pay`, `amenu:help`).
- callback `amenu:*` — обработка через `@Callback/@CallbackPrefix`.
- `/aform` — FSM flow через `@State(...)`.
- `/aecho <text>` — обработка через `@Message(text=..., startsWith=true)`.

Smoke API:
- `/qa` — список smoke команд.
- `/qa_run_all` — запуск последовательной проверки API с итоговым отчётом `pass/fail/skip`.
- `/qa_callback` — отправляет кнопку, по нажатию проверяется callback flow + `POST /answers`.
- `/qa_set_video <token>` — сохраняет `videoToken` для шага `GET /videos/{token}`.

`/qa_run_all` покрывает:
- `GET /me`, `GET /chats`, `GET /chats/{chatId}`
- `POST/PUT/GET(/id)/GET(list)/DELETE /messages` (delete при `demo.smoke.destructive=true`)
- `POST /chats/{chatId}/actions`
- `GET /chats/{chatId}/members`, `GET /admins`, `GET /members/me`
- `GET/PUT/DELETE /chats/{chatId}/pin`
- `GET /updates`
- `GET/POST/DELETE /subscriptions` (POST/DELETE при заполненном `demo.smoke.webhook-url`)
- `POST /uploads`
- `GET /videos/{videoToken}` (если заранее задан token)

## Что осознанно не покрыто в этом demo

- webhook mode (в этом приложении проверяется именно polling как основной сценарий);
- scenes/wizard UI.
- destructive chat mutations по умолчанию отключены (`demo.smoke.destructive=false`).

Эти части уже есть в framework, но здесь намеренно оставлен минимальный ручной стенд для быстрой проверки базового runtime path.
