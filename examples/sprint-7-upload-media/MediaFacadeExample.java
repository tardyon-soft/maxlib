package ru.max.botframework.examples.sprint7;

import java.nio.file.Path;
import ru.max.botframework.client.MaxBotClient;
import ru.max.botframework.message.MediaAttachment;
import ru.max.botframework.message.MediaMessagingFacade;
import ru.max.botframework.message.MessageTarget;
import ru.max.botframework.message.Messages;
import ru.max.botframework.message.MessagingFacade;
import ru.max.botframework.model.ChatId;
import ru.max.botframework.model.Message;
import ru.max.botframework.upload.InputFile;
import ru.max.botframework.upload.UploadResult;
import ru.max.botframework.upload.UploadService;

/**
 * Sprint 7 low-level high-level usage example outside dispatcher runtime context.
 */
public final class MediaFacadeExample {

    public static void main(String[] args) {
        MaxBotClient botClient = createConfiguredBotClient();
        UploadService uploadService = createConfiguredUploadService();

        MessagingFacade messaging = new MessagingFacade(botClient);
        MediaMessagingFacade media = new MediaMessagingFacade(uploadService, messaging);

        // 1) Input sources
        InputFile imageFromPath = InputFile.fromPath(Path.of("./assets/photo.jpg"));
        InputFile documentFromBytes = InputFile.fromBytes(loadPdfBytes(), "invoice.pdf");

        // 2) sendImage / sendFile
        Message sentImage = media.sendImage(MessageTarget.chat(new ChatId("chat-1")), imageFromPath);
        Message sentFile = media.sendFile(MessageTarget.chat(new ChatId("chat-1")), documentFromBytes, "Счёт");

        // 3) replyVideo / replyAudio
        Message sourceMessage = loadSourceMessage();
        media.replyVideo(sourceMessage, InputFile.fromPath(Path.of("./assets/clip.mp4")), "Демо-видео");
        media.replyAudio(sourceMessage, InputFile.fromBytes(loadAudioBytes(), "voice.mp3"), "Голосовой ответ");

        // 4) builder + media attachment composition
        UploadResult uploadedPreview = uploadService
                .upload(InputFile.fromPath(Path.of("./assets/preview.jpg")))
                .toCompletableFuture()
                .join();

        Message composed = messaging.send(
                new ChatId("chat-1"),
                Messages.text("Материалы готовы")
                        .attachment(MediaAttachment.image(uploadedPreview).caption("Превью"))
        );

        System.out.println("sentImage=" + sentImage.messageId().value()
                + ", sentFile=" + sentFile.messageId().value()
                + ", composed=" + composed.messageId().value());
    }

    private static MaxBotClient createConfiguredBotClient() {
        throw new UnsupportedOperationException("Provide configured MaxBotClient instance");
    }

    private static UploadService createConfiguredUploadService() {
        throw new UnsupportedOperationException("Provide configured UploadService instance");
    }

    private static Message loadSourceMessage() {
        throw new UnsupportedOperationException("Load source message from update/runtime");
    }

    private static byte[] loadPdfBytes() {
        throw new UnsupportedOperationException("Load PDF bytes from your source");
    }

    private static byte[] loadAudioBytes() {
        throw new UnsupportedOperationException("Load audio bytes from your source");
    }
}
