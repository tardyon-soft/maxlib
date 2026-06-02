# Тестирование через max-testkit

## DispatcherTestKit

`DispatcherTestKit` поднимает реальный `Dispatcher` и позволяет прогонять через него `Update`.

Минимальный пример:

```java
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.dispatcher.BuiltInFilters;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.message.Messages;
import ru.tardyon.botframework.model.Update;
import ru.tardyon.botframework.testkit.DispatcherTestKit;

class BotTest {
    @Test
    void startCommandIsHandled() {
        Router router = new Router("test");
        router.message(BuiltInFilters.command("start"), (message, ctx) -> {
            ctx.reply(Messages.text("Привет"));
            return CompletableFuture.completedFuture(null);
        });

        DispatcherTestKit testKit = DispatcherTestKit.withRouter(router);

        Update update = /* создать Update с текстом /start */;
        DispatcherTestKit.DispatchProbe probe = testKit.feedAndCapture(update);

        assert probe.result().handled();
        assert !probe.sideEffects().isEmpty();
    }
}
```

## Что умеет testkit

- включать один или несколько `Router`
- подключать `FSMStorage`
- подключать `SceneRegistry` и `SceneStorage`
- подключать `UploadService`
- перехватывать вызовы через `RecordingMaxBotClient`

## Проверка побочных эффектов

После `feedAndCapture(...)` можно проверить:

- итог `DispatchResult`
- список `CapturedApiCall`
- наличие вызова по path через `hasCall(...)`

Пример:

```java
DispatcherTestKit.DispatchProbe probe = testKit.feedAndCapture(update);
assert probe.hasCall("/messages");
```

## Важно

`DispatcherTestKit` не создает фикстуры `Update` за вас. В тестах нужно использовать собственные builder-ы, готовые JSON fixtures или вспомогательные фабрики проекта.
