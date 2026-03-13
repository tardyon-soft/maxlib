# Demo Spring Boot Polling App

Отдельное тестовое Spring Boot приложение внутри репозитория для ручной проверки MAX bot framework.

## Назначение

Этот demo показывает фактический текущий API framework без вымышленных слоёв:
- Spring Boot starter в `POLLING` режиме;
- `Dispatcher/Router` handlers;
- built-in filters;
- inline keyboard + callback handling;
- chat action (`typing`);
- FSM (`/form`) через `BuiltInFilters.state(...)` и `FSMContext`.

## Структура

- `src/main/java/.../DemoSpringPollingApplication.java` — main app + router handlers.
- `src/main/resources/application.yml` — конфигурация через properties/env.
- `src/test/java/.../DemoSpringPollingApplicationSmokeTest.java` — smoke test поднятия контекста.

## Конфигурация

Токен не захардкожен и берётся из env:

```bash
export MAX_BOT_TOKEN=<your-max-bot-token>
```

`application.yml` использует:

- `max.bot.token: ${MAX_BOT_TOKEN:}`
- `max.bot.mode: POLLING`
- polling `limit/timeout/types`.

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

## Что осознанно не покрыто в этом demo

- webhook mode (в этом приложении проверяется именно polling как основной сценарий);
- upload/media flow;
- scenes/wizard UI.

Эти части уже есть в framework, но здесь намеренно оставлен минимальный ручной стенд для быстрой проверки базового runtime path.
