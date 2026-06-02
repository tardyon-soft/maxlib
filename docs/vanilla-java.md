# Vanilla Java

## Минимальный long polling бот

Ниже пример без Spring Boot: создается `MaxBotClient`, `Dispatcher`, `Router` и `DefaultLongPollingRunner`.

```java
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import okhttp3.OkHttpClient;
import ru.tardyon.botframework.client.DefaultMaxBotClient;
import ru.tardyon.botframework.client.MaxApiClientConfig;
import ru.tardyon.botframework.client.MaxBotClient;
import ru.tardyon.botframework.client.http.MaxHttpClient;
import ru.tardyon.botframework.client.http.okhttp.OkHttpMaxHttpClient;
import ru.tardyon.botframework.client.serialization.JacksonJsonCodec;
import ru.tardyon.botframework.client.serialization.JsonCodec;
import ru.tardyon.botframework.dispatcher.BuiltInFilters;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.fsm.MemoryStorage;
import ru.tardyon.botframework.ingestion.DefaultLongPollingRunner;
import ru.tardyon.botframework.ingestion.LongPollingRunner;
import ru.tardyon.botframework.ingestion.LongPollingRunnerConfig;
import ru.tardyon.botframework.ingestion.PollingFetchRequest;
import ru.tardyon.botframework.ingestion.PollingUpdateSource;
import ru.tardyon.botframework.ingestion.SdkPollingUpdateSource;
import ru.tardyon.botframework.message.Messages;

public final class VanillaBotApp {
    public static void main(String[] args) throws Exception {
        MaxApiClientConfig config = MaxApiClientConfig.builder()
                .token(System.getenv("MAX_BOT_TOKEN"))
                .build();

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(config.connectTimeout())
                .readTimeout(config.readTimeout())
                .callTimeout(Duration.ZERO)
                .build();

        MaxHttpClient transport = new OkHttpMaxHttpClient(config.baseUri(), okHttpClient);
        JsonCodec jsonCodec = new JacksonJsonCodec();
        MaxBotClient botClient = new DefaultMaxBotClient(config, transport, jsonCodec);

        Router router = new Router("main");
        router.message(BuiltInFilters.command("start"), (message, ctx) -> {
            ctx.reply(Messages.text("Привет из vanilla Java"));
            return CompletableFuture.completedFuture(null);
        });

        Dispatcher dispatcher = new Dispatcher()
                .withBotClient(botClient)
                .withFsmStorage(new MemoryStorage())
                .includeRouter(router);

        PollingUpdateSource source = new SdkPollingUpdateSource(botClient);
        LongPollingRunner runner = new DefaultLongPollingRunner(
                source,
                dispatcher,
                LongPollingRunnerConfig.builder()
                        .request(new PollingFetchRequest(null, 30, 100, java.util.List.of()))
                        .build()
        );

        Runtime.getRuntime().addShutdownHook(new Thread(runner::shutdown));
        runner.start();
        new CountDownLatch(1).await();
    }
}
```

## Прямой вызов MAX API без dispatcher

Если обработка update не нужна, можно работать напрямую через `MaxBotClient`.

```java
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.message.Messages;

MessagingFacade messaging = new MessagingFacade(botClient);
messaging.send(ChatId.of(123456L), Messages.text("Тестовое сообщение"));
```

Либо через request-модели:

```java
BotInfo me = botClient.getMe();
```

## Аннотационный роутинг без Spring

Аннотационный API не привязан к Spring. Класс с `@Route` можно зарегистрировать вручную:

```java
AnnotatedRouteRegistrar registrar = new AnnotatedRouteRegistrar();
dispatcher.includeRouter(registrar.register(new MenuRoute()));
```

## Webhook без Spring

В vanilla-режиме библиотека дает transport-agnostic интерфейс `WebhookReceiver`. Это граница между вашим HTTP-слоем и обработкой update:

```java
CompletionStage<WebhookReceiveResult> result = webhookReceiver.receive(
        new WebhookRequest(bodyBytes, headers)
);
```

Готовый HTTP-контроллер в репозитории есть только для Spring MVC.
