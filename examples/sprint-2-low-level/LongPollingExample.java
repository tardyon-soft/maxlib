package ru.tardyon.botframework.examples.sprint2;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import okhttp3.OkHttpClient;
import ru.tardyon.botframework.client.DefaultMaxBotClient;
import ru.tardyon.botframework.client.MaxApiClientConfig;
import ru.tardyon.botframework.client.MaxBotClient;
import ru.tardyon.botframework.client.http.MaxHttpClient;
import ru.tardyon.botframework.client.http.okhttp.OkHttpMaxHttpClient;
import ru.tardyon.botframework.client.serialization.JacksonJsonCodec;
import ru.tardyon.botframework.ingestion.DefaultLongPollingRunner;
import ru.tardyon.botframework.ingestion.LongPollingRunner;
import ru.tardyon.botframework.ingestion.LongPollingRunnerConfig;
import ru.tardyon.botframework.ingestion.PollingFetchRequest;
import ru.tardyon.botframework.ingestion.SdkPollingUpdateSource;
import ru.tardyon.botframework.ingestion.UpdateHandlingResult;
import ru.tardyon.botframework.ingestion.UpdateConsumer;
import ru.tardyon.botframework.model.UpdateEventType;

public final class LongPollingExample {

    public static void main(String[] args) {
        MaxApiClientConfig config = MaxApiClientConfig.builder()
                .token(System.getenv("MAX_BOT_TOKEN"))
                .build();

        OkHttpClient okHttp = new OkHttpClient.Builder()
                .connectTimeout(config.connectTimeout())
                .readTimeout(config.readTimeout())
                .build();

        MaxHttpClient httpClient = new OkHttpMaxHttpClient(config.baseUri(), okHttp);
        MaxBotClient botClient = new DefaultMaxBotClient(config, httpClient, new JacksonJsonCodec());

        SdkPollingUpdateSource source = new SdkPollingUpdateSource(botClient);
        UpdateConsumer sink = update -> {
            System.out.println("Update " + update.updateId().value() + " type=" + update.type().value());
            return CompletableFuture.completedFuture(UpdateHandlingResult.success());
        };

        LongPollingRunner runner = new DefaultLongPollingRunner(
                source,
                sink,
                LongPollingRunnerConfig.builder()
                        .request(new PollingFetchRequest(
                                null,
                                30,
                                100,
                                List.of(UpdateEventType.MESSAGE_CREATED, UpdateEventType.MESSAGE_CALLBACK)
                        ))
                        .idleDelay(Duration.ofMillis(200))
                        .shutdownTimeout(Duration.ofSeconds(2))
                        .build()
        );

        runner.start();
        Runtime.getRuntime().addShutdownHook(new Thread(runner::shutdown));
    }
}
