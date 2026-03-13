package ru.max.botframework.examples.sprint7;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import ru.max.botframework.client.MaxBotClient;
import ru.max.botframework.dispatcher.DispatchResult;
import ru.max.botframework.dispatcher.Dispatcher;
import ru.max.botframework.dispatcher.Router;
import ru.max.botframework.message.MediaMessagingFacade;
import ru.max.botframework.model.Chat;
import ru.max.botframework.model.ChatId;
import ru.max.botframework.model.ChatType;
import ru.max.botframework.model.Message;
import ru.max.botframework.model.MessageId;
import ru.max.botframework.model.Update;
import ru.max.botframework.model.UpdateId;
import ru.max.botframework.model.UpdateType;
import ru.max.botframework.model.User;
import ru.max.botframework.model.UserId;
import ru.max.botframework.upload.InputFile;
import ru.max.botframework.upload.UploadService;

/**
 * Sprint 7 runtime handler ergonomics example for upload/media layer.
 */
public final class RuntimeMediaHandlersExample {

    public static void main(String[] args) throws Exception {
        Dispatcher dispatcher = new Dispatcher()
                .withBotClient(createConfiguredBotClient())
                .withUploadService(createConfiguredUploadService());

        Router router = new Router("media-runtime");
        ExampleHandlers handlers = new ExampleHandlers();

        // RuntimeContext shortcuts in handlers.
        router.message((message, context) -> {
            context.replyImage(InputFile.fromPath(Path.of("./assets/reply.jpg")));
            context.replyFile(InputFile.fromBytes(loadReportBytes(), "report.pdf"));
            context.sendVideo(InputFile.fromPath(Path.of("./assets/clip.mp4")));
            context.sendAudio(InputFile.fromBytes(loadAudioBytes(), "voice.mp3"));
            return CompletableFuture.completedFuture(null);
        });

        // Reflective handler parameter resolution of MediaMessagingFacade.
        Method method = ExampleHandlers.class.getDeclaredMethod(
                "onMessage",
                Message.class,
                MediaMessagingFacade.class
        );
        router.message(handlers, method);

        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(sampleMessageUpdate()).toCompletableFuture().join();
        System.out.println("Dispatch result: " + result.status());
    }

    public static final class ExampleHandlers {
        @SuppressWarnings("unused")
        public CompletableFuture<Void> onMessage(Message message, MediaMessagingFacade media) {
            media.replyVideo(message, InputFile.fromPath(Path.of("./assets/reply.mp4")), "Видео-ответ");
            media.replyAudio(message, InputFile.fromBytes(loadAudioBytes(), "reply.mp3"), "Аудио-ответ");
            return CompletableFuture.completedFuture(null);
        }
    }

    private static MaxBotClient createConfiguredBotClient() {
        throw new UnsupportedOperationException("Provide configured MaxBotClient instance");
    }

    private static UploadService createConfiguredUploadService() {
        throw new UnsupportedOperationException("Provide configured UploadService instance");
    }

    private static byte[] loadReportBytes() {
        throw new UnsupportedOperationException("Load report bytes from your source");
    }

    private static byte[] loadAudioBytes() {
        throw new UnsupportedOperationException("Load audio bytes from your source");
    }

    private static Update sampleMessageUpdate() {
        return new Update(
                new UpdateId("u-s7-msg"),
                UpdateType.MESSAGE,
                new Message(
                        new MessageId("m-s7-msg"),
                        new Chat(new ChatId("c-s7"), ChatType.PRIVATE, "chat", null, null),
                        new User(new UserId("u-s7"), "demo", "Demo", "User", "Demo User", false, "en"),
                        "ping",
                        Instant.parse("2026-03-12T00:00:00Z"),
                        null,
                        List.of(),
                        List.of()
                ),
                null,
                null,
                Instant.parse("2026-03-12T00:00:00Z")
        );
    }
}
