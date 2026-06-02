# Spring Boot

## Зависимости

Минимально:

```kotlin
dependencies {
    implementation("ru.tardyon.botframework:max-spring-boot-starter:<version>")
}
```

Для webhook:

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
}
```

Для Redis storage:

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
}
```

## Polling режим

`application.yml`:

```yaml
max:
  bot:
    token: ${MAX_BOT_TOKEN}
    mode: POLLING
    polling:
      enabled: true
      limit: 100
      timeout: 30s
```

Минимальный `Router` bean:

```java
import java.util.concurrent.CompletableFuture;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.tardyon.botframework.dispatcher.BuiltInFilters;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.message.Messages;

@Configuration
public class BotConfig {
    @Bean
    Router botRouter() {
        Router router = new Router("main");
        router.message(BuiltInFilters.command("start"), (message, ctx) -> {
            ctx.reply(Messages.text("Привет из Spring Boot"));
            return CompletableFuture.completedFuture(null);
        });
        return router;
    }
}
```

Starter сам создает `MaxBotClient`, `Dispatcher` и lifecycle для polling.

## Webhook режим

`application.yml`:

```yaml
max:
  bot:
    token: ${MAX_BOT_TOKEN}
    mode: WEBHOOK
    webhook:
      path: /webhook/max
      secret: ${MAX_WEBHOOK_SECRET}
      max-in-flight: 64
```

В этом режиме starter поднимает Spring MVC endpoint по пути `max.bot.webhook.path` и передает запросы в `WebhookReceiver`.

## Аннотационный роутинг

Классы с `@Route` можно регистрировать без `@Component`. Starter сканирует пакеты приложения и сам добавляет такие классы в Spring context. Если у `@Route(autoRegister = true)`, route автоматически включается в `Dispatcher`.

```java
import ru.tardyon.botframework.dispatcher.RuntimeContext;
import ru.tardyon.botframework.dispatcher.annotation.Callback;
import ru.tardyon.botframework.dispatcher.annotation.Command;
import ru.tardyon.botframework.dispatcher.annotation.Route;
import ru.tardyon.botframework.message.Messages;

@Route(value = "menu", autoRegister = true)
public final class MenuRoute {

    @Command("start")
    public void start(RuntimeContext ctx) {
        ctx.reply(Messages.text("Команды: /menu"));
    }

    @Command("menu")
    public void menu(RuntimeContext ctx) {
        ctx.reply(Messages.text("Меню открыто"));
    }

    @Callback("menu:ok")
    public void ok(RuntimeContext ctx) {
        ctx.answerCallback("OK");
    }
}
```

## Storage

По умолчанию starter использует:

- `max.bot.storage.type=MEMORY`
- `max.bot.storage.state-scope=USER_IN_CHAT`

Переключение на Redis:

```yaml
max:
  bot:
    storage:
      type: REDIS
      redis:
        key-prefix: max:bot:fsm
        ttl: 30m
```

## Screen API в Spring

Starter автоматически регистрирует:

- классы с `@Screen`
- фасадные контроллеры с `@ScreenController`
- widget-контроллеры с `@WidgetController`

Пример `@Screen`:

```java
import java.util.Map;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.screen.ScreenContext;
import ru.tardyon.botframework.screen.ScreenModel;
import ru.tardyon.botframework.screen.Widgets;
import ru.tardyon.botframework.screen.annotation.OnAction;
import ru.tardyon.botframework.screen.annotation.Render;
import ru.tardyon.botframework.screen.annotation.Screen;

@Screen("home")
public final class HomeScreen {
    @Render
    public ScreenModel render(ScreenContext ctx) {
        return ScreenModel.builder()
                .title("Главная")
                .widget(Widgets.text("Экран зарегистрирован через starter"))
                .showBackButton(false)
                .build();
    }

    @OnAction("open_profile")
    public CompletionStage<Void> openProfile(ScreenContext ctx) {
        return ctx.nav().push("profile", Map.of("name", "Гость"));
    }
}
```
