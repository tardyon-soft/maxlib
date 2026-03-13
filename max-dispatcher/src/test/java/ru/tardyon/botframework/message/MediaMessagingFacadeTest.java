package ru.tardyon.botframework.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.tardyon.botframework.client.MaxBotClient;
import ru.tardyon.botframework.model.Chat;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.ChatType;
import ru.tardyon.botframework.model.Message;
import ru.tardyon.botframework.model.MessageAttachmentType;
import ru.tardyon.botframework.model.MessageId;
import ru.tardyon.botframework.model.User;
import ru.tardyon.botframework.model.UserId;
import ru.tardyon.botframework.model.request.SendMessageRequest;
import ru.tardyon.botframework.upload.InputFile;
import ru.tardyon.botframework.upload.UploadFlowType;
import ru.tardyon.botframework.upload.UploadMediaKind;
import ru.tardyon.botframework.upload.UploadRef;
import ru.tardyon.botframework.upload.UploadRequest;
import ru.tardyon.botframework.upload.UploadResult;
import ru.tardyon.botframework.upload.UploadService;

class MediaMessagingFacadeTest {

    @Test
    void sendImagePerformsUploadThenSendWithPhotoAttachment() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        UploadService uploadService = Mockito.mock(UploadService.class);

        when(uploadService.upload(any(InputFile.class), any(UploadRequest.class))).thenReturn(
                CompletableFuture.completedFuture(uploadResult("ref-image", UploadMediaKind.IMAGE, "image/png", 123L))
        );
        when(client.sendMessage(any(SendMessageRequest.class))).thenReturn(sampleMessage("m-out", ""));

        MessagingFacade messagingFacade = new MessagingFacade(client);
        MediaMessagingFacade mediaFacade = new MediaMessagingFacade(uploadService, messagingFacade);

        mediaFacade.sendImage(new ChatId("chat-1"), bytesInput(new byte[]{1, 2, 3}, "photo.png"));

        ArgumentCaptor<UploadRequest> uploadRequestCaptor = ArgumentCaptor.forClass(UploadRequest.class);
        verify(uploadService).upload(any(InputFile.class), uploadRequestCaptor.capture());
        assertEquals("image", uploadRequestCaptor.getValue().mediaTypeHint());

        ArgumentCaptor<SendMessageRequest> sendCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(client).sendMessage(sendCaptor.capture());
        SendMessageRequest request = sendCaptor.getValue();
        assertEquals("chat-1", request.chatId().value());
        assertEquals(1, request.body().attachments().size());
        assertEquals(MessageAttachmentType.PHOTO, request.body().attachments().getFirst().type());
        assertEquals("ref-image", request.body().attachments().getFirst().input().uploadRef());
    }

    @Test
    void replyVideoPerformsUploadThenReplyWithVideoAttachment() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        UploadService uploadService = Mockito.mock(UploadService.class);

        when(uploadService.upload(any(InputFile.class), any(UploadRequest.class))).thenReturn(
                CompletableFuture.completedFuture(uploadResult(
                        "ref-video",
                        UploadMediaKind.VIDEO,
                        "video/mp4",
                        456L,
                        Map.of("videoToken", "video-token-1")
                ))
        );
        when(client.sendMessage(any(SendMessageRequest.class))).thenReturn(sampleMessage("m-reply", ""));

        MessagingFacade messagingFacade = new MessagingFacade(client);
        MediaMessagingFacade mediaFacade = new MediaMessagingFacade(uploadService, messagingFacade);
        Message source = sampleMessage("m-src", "incoming");

        mediaFacade.replyVideo(source, bytesInput(new byte[]{4, 5, 6}, "clip.mp4"), "clip");

        ArgumentCaptor<SendMessageRequest> sendCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(client).sendMessage(sendCaptor.capture());
        SendMessageRequest request = sendCaptor.getValue();

        assertEquals(source.chat().id().value(), request.chatId().value());
        assertEquals(source.messageId().value(), request.replyToMessageId().value());
        assertEquals(MessageAttachmentType.VIDEO, request.body().attachments().getFirst().type());
        assertEquals("video-token-1", request.body().attachments().getFirst().input().uploadRef());
        assertEquals("clip", request.body().attachments().getFirst().caption());
    }

    @Test
    void sendFileAndSendAudioMapToExpectedAttachmentTypes() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        UploadService uploadService = Mockito.mock(UploadService.class);

        when(uploadService.upload(any(InputFile.class), any(UploadRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(uploadResult("ref-file", UploadMediaKind.FILE, "application/pdf", 10L)))
                .thenReturn(CompletableFuture.completedFuture(uploadResult(
                        "ref-audio",
                        UploadMediaKind.AUDIO,
                        "audio/mpeg",
                        11L,
                        Map.of("audioToken", "audio-token-1")
                )));
        when(client.sendMessage(any(SendMessageRequest.class))).thenReturn(sampleMessage("m-out", ""));

        MessagingFacade messagingFacade = new MessagingFacade(client);
        MediaMessagingFacade mediaFacade = new MediaMessagingFacade(uploadService, messagingFacade);

        mediaFacade.sendFile(new ChatId("chat-1"), bytesInput(new byte[]{1}, "doc.pdf"));
        mediaFacade.sendAudio(MessageTarget.chat(new ChatId("chat-1")), bytesInput(new byte[]{2}, "voice.mp3"));

        ArgumentCaptor<SendMessageRequest> sendCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(client, org.mockito.Mockito.times(2)).sendMessage(sendCaptor.capture());
        List<SendMessageRequest> requests = sendCaptor.getAllValues();

        assertEquals(MessageAttachmentType.FILE, requests.getFirst().body().attachments().getFirst().type());
        assertEquals("ref-file", requests.getFirst().body().attachments().getFirst().input().uploadRef());
        assertEquals(MessageAttachmentType.AUDIO, requests.get(1).body().attachments().getFirst().type());
        assertEquals("audio-token-1", requests.get(1).body().attachments().getFirst().input().uploadRef());
    }

    private static UploadResult uploadResult(String ref, UploadMediaKind kind, String contentType, long size) {
        return uploadResult(ref, kind, contentType, size, Map.of());
    }

    private static UploadResult uploadResult(
            String ref,
            UploadMediaKind kind,
            String contentType,
            long size,
            Map<String, String> payload
    ) {
        return new UploadResult(
                new UploadRef(ref),
                UploadFlowType.MULTIPART,
                size,
                contentType,
                kind,
                payload
        );
    }

    private static Message sampleMessage(String messageId, String text) {
        return new Message(
                new MessageId(messageId),
                new Chat(new ChatId("chat-1"), ChatType.PRIVATE, "chat", null, null),
                new User(new UserId("user-1"), "demo", "Demo", "User", "Demo User", false, "en"),
                text,
                Instant.parse("2026-03-12T00:00:00Z"),
                null,
                List.of(),
                List.of()
        );
    }

    private static InputFile bytesInput(byte[] bytes, String fileName) {
        return new InputFile.BytesInputFile(bytes, fileName, Optional.empty());
    }
}
