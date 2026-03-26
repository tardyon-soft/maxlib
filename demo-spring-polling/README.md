# Demo Spring Boot Polling App

Ручной стенд для проверки актуального API библиотеки в режиме `POLLING`.

## Что покрывает demo

- `Dispatcher/Router` и annotation routes.
- `@Route` без `@Component` (авто-регистрация в Spring starter).
- `@Screen`/`@Render`/`@OnAction`/`@OnText`.
- FSM формы (`/form`, `/aform`) и screen stack (`/screen`, `/ascreen`).
- App-mode UX (`/app`) с удалением пользовательских сообщений и обновлением интерфейса через callback.
- Smoke-команды для проверки MAX API (`/qa*`).

## Основные файлы

- `src/main/java/.../DemoSpringPollingApplication.java` — классический router API.
- `src/main/java/.../AnnotatedMenuRoute.java` — команды/callback на аннотациях.
- `src/main/java/.../AnnotatedFormRoute.java` — FSM на аннотациях.
- `src/main/java/.../AppModeRoute.java` и `AppModeMiddleware.java` — app-like UX.
- `src/main/java/.../AnnotatedScreenRoute.java` + `Annotated*Screen.java` — screen API через аннотации.
- `src/main/java/.../ApiSmokeRoute.java` + `ApiSmokeService.java` — smoke API.

## Конфигурация

```bash
export MAX_BOT_TOKEN=<token>
export DEMO_SMOKE_DESTRUCTIVE=false
export DEMO_SMOKE_WEBHOOK_URL=
```

`application.yml`:

- `max.bot.mode=POLLING`
- polling: `types`, `timeout`, `limit`
- `demo.smoke.destructive`
- `demo.smoke.webhook-url`

`LongPollingRunnerConfig` вручную в demo не создается: его поднимает starter.

## Запуск

```bash
./gradlew :demo-spring-polling:run
```

### Redis профиль (FSM storage)

```bash
docker compose -f demo-spring-polling/docker-compose.redis.yml up -d
SPRING_PROFILES_ACTIVE=redis ./gradlew :demo-spring-polling:run
```

Профиль `redis` включает `max.bot.storage.type=REDIS` и `spring.data.redis.*`.

## Команды

Классический API:

- `/start`
- `/menu` + callbacks `menu:pay`, `menu:help`
- `/typing`
- `/form`
- `/screen`

Аннотационный API:

- `/astart`
- `/amenu` + callbacks `amenu:pay`, `amenu:*`
- `/aecho <text>`
- `/aform`
- `/ascreen`

App mode:

- `/app`
- callbacks `app:*`

Smoke API:

- `/qa`
- `/qa_run_all`
- `/qa_callback`
- `/qa_set_video <token>`

## Важные замечания

- Командные фильтры имеют повышенный приоритет, поэтому `/start`, `/screen`, `/form` и другие команды обрабатываются раньше generic текстовых/state обработчиков.
- Screen state хранится в отдельном FSM namespace (`screen`) и не конфликтует с пользовательскими FSM стейтами.
- Для media в экранах используйте только валидные MAX upload/file reference; произвольные URL могут давать `400`.

## Мини-примеры screen API

Старт ручного screen-flow:

```java
router.message(BuiltInFilters.command("screen"), (message, ctx) ->
        Screens.navigator(ctx, screenRegistry).start("home", Map.of())
);
```

Старт аннотационного screen-flow:

```java
@Route(value = "annotated-screen-route", autoRegister = true)
public final class AnnotatedScreenRoute {
    @Command("ascreen")
    public CompletionStage<Void> start(RuntimeContext context, ScreenRegistry screenRegistry) {
        return Screens.navigator(context, screenRegistry).start("annotated.home", Map.of());
    }
}
```
