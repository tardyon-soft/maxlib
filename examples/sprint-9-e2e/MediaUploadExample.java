package ru.max.botframework.examples.sprint9;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import ru.max.botframework.dispatcher.BuiltInFilters;
import ru.max.botframework.dispatcher.Dispatcher;
import ru.max.botframework.dispatcher.Router;
import ru.max.botframework.upload.InputFile;
import ru.max.botframework.upload.UploadService;

/**
 * Optional upload/media example using runtime shortcuts and media facade.
 */
public final class MediaUploadExample {

    public static void main(String[] args) {
        Dispatcher dispatcher = new Dispatcher()
                .withBotClient(createConfiguredBotClient())
                .withUploadService(createConfiguredUploadService());

        Router router = new Router("media");

        router.message(BuiltInFilters.textEquals("/photo"), (message, ctx) -> {
            ctx.replyImage(InputFile.fromPath(Path.of("./assets/photo.jpg")));
            return CompletableFuture.completedFuture(null);
        });

        router.message(BuiltInFilters.textEquals("/video"), (message, ctx) -> {
            ctx.sendVideo(InputFile.fromPath(Path.of("./assets/video.mp4")));
            return CompletableFuture.completedFuture(null);
        });

        router.message(BuiltInFilters.textEquals("/audio"), (message, ctx) -> {
            ctx.media().sendAudio(message.chat().id(), InputFile.fromBytes(loadAudioBytes(), "voice.mp3"));
            return CompletableFuture.completedFuture(null);
        });

        dispatcher.includeRouter(router);
    }

    private static ru.max.botframework.client.MaxBotClient createConfiguredBotClient() {
        throw new UnsupportedOperationException("Provide configured MaxBotClient instance");
    }

    private static UploadService createConfiguredUploadService() {
        throw new UnsupportedOperationException("Provide configured UploadService instance");
    }

    private static byte[] loadAudioBytes() {
        throw new UnsupportedOperationException("Load audio bytes from your source");
    }
}
