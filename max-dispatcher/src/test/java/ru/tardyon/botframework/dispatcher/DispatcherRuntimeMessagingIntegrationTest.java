package ru.tardyon.botframework.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.tardyon.botframework.action.ChatActionsFacade;
import ru.tardyon.botframework.callback.CallbackFacade;
import ru.tardyon.botframework.client.MaxBotClient;
import ru.tardyon.botframework.message.MediaMessagingFacade;
import ru.tardyon.botframework.message.MediaAttachment;
import ru.tardyon.botframework.message.MessagingFacade;
import ru.tardyon.botframework.message.Messages;
import ru.tardyon.botframework.model.Callback;
import ru.tardyon.botframework.model.CallbackId;
import ru.tardyon.botframework.model.Chat;
import ru.tardyon.botframework.model.ChatAction;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.ChatType;
import ru.tardyon.botframework.model.Message;
import ru.tardyon.botframework.model.MessageId;
import ru.tardyon.botframework.model.Update;
import ru.tardyon.botframework.model.UpdateId;
import ru.tardyon.botframework.model.UpdateType;
import ru.tardyon.botframework.model.User;
import ru.tardyon.botframework.model.UserId;
import ru.tardyon.botframework.model.request.AnswerCallbackRequest;
import ru.tardyon.botframework.model.request.SendMessageRequest;
import ru.tardyon.botframework.upload.InputFile;
import ru.tardyon.botframework.upload.UploadFlowType;
import ru.tardyon.botframework.upload.UploadMediaKind;
import ru.tardyon.botframework.upload.UploadRef;
import ru.tardyon.botframework.upload.UploadResult;
import ru.tardyon.botframework.upload.UploadService;

class DispatcherRuntimeMessagingIntegrationTest {

    @Test
    void messageHandlerCanReplyViaRuntimeContext() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        when(client.sendMessage(any(SendMessageRequest.class))).thenReturn(sampleMessage("m-out", "reply"));

        Dispatcher dispatcher = new Dispatcher().withBotClient(client);
        Router router = new Router("main");
        router.message((message, context) -> {
            context.reply(Messages.text("pong"));
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate("m-in", "ping")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(client).sendMessage(requestCaptor.capture());
        SendMessageRequest request = requestCaptor.getValue();
        assertEquals("chat-1", request.chatId().value());
        assertEquals("m-in", request.replyToMessageId().value());
        assertEquals("pong", request.body().text());
    }

    @Test
    void callbackHandlerCanAnswerViaRuntimeContext() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        when(client.answerCallback(any(AnswerCallbackRequest.class))).thenReturn(true);

        Dispatcher dispatcher = new Dispatcher().withBotClient(client);
        Router router = new Router("callbacks");
        router.callback((callback, context) -> {
            context.answerCallback("OK");
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(callbackUpdate("cb-1", "m-cb")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        ArgumentCaptor<AnswerCallbackRequest> requestCaptor = ArgumentCaptor.forClass(AnswerCallbackRequest.class);
        verify(client).answerCallback(requestCaptor.capture());
        AnswerCallbackRequest request = requestCaptor.getValue();
        assertEquals("cb-1", request.callbackId().value());
        assertEquals("OK", request.text());
    }

    @Test
    void handlerCanSendChatActionViaRuntimeContext() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        when(client.sendChatAction(new ChatId("chat-1"), ChatAction.TYPING)).thenReturn(true);

        Dispatcher dispatcher = new Dispatcher().withBotClient(client);
        Router router = new Router("actions");
        router.message((message, context) -> {
            boolean sent = context.chatAction(ChatAction.TYPING);
            assertTrue(sent);
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate("m-act", "hello")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        verify(client).sendChatAction(new ChatId("chat-1"), ChatAction.TYPING);
    }

    @Test
    void reflectiveHandlerCanResolveMessagingFacadeParameter() throws Exception {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        when(client.sendMessage(any(SendMessageRequest.class))).thenReturn(sampleMessage("m-out", "done"));

        Dispatcher dispatcher = new Dispatcher().withBotClient(client);
        Router router = new Router("reflective");
        ReflectiveProbe probe = new ReflectiveProbe();
        Method method = ReflectiveProbe.class.getDeclaredMethod("onMessage", Message.class, MessagingFacade.class);
        router.message(probe, method);
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate("m-ref", "hello")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertTrue(probe.invoked);
        verify(client).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void reflectiveCallbackHandlerCanResolveCallbackFacadeParameter() throws Exception {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        when(client.answerCallback(any(AnswerCallbackRequest.class))).thenReturn(true);

        Dispatcher dispatcher = new Dispatcher().withBotClient(client);
        Router router = new Router("cb-reflective");
        CallbackReflectiveProbe probe = new CallbackReflectiveProbe();
        Method method = CallbackReflectiveProbe.class.getDeclaredMethod(
                "onCallback",
                Callback.class,
                CallbackFacade.class
        );
        router.callback(probe, method);
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(callbackUpdate("cb-ref", "m-ref")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertTrue(probe.invoked);
        verify(client).answerCallback(any(AnswerCallbackRequest.class));
    }

    @Test
    void reflectiveMessageHandlerCanResolveChatActionsFacadeParameter() throws Exception {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        when(client.sendChatAction(new ChatId("chat-1"), ChatAction.TYPING)).thenReturn(true);

        Dispatcher dispatcher = new Dispatcher().withBotClient(client);
        Router router = new Router("actions-reflective");
        ActionReflectiveProbe probe = new ActionReflectiveProbe();
        Method method = ActionReflectiveProbe.class.getDeclaredMethod(
                "onMessage",
                Message.class,
                ChatActionsFacade.class
        );
        router.message(probe, method);
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate("m-action-ref", "hello")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertTrue(probe.invoked);
        verify(client).sendChatAction(new ChatId("chat-1"), ChatAction.TYPING);
    }

    @Test
    void handlerCanReplyImageAndFileViaRuntimeContextMediaFacade() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        UploadService uploadService = Mockito.mock(UploadService.class);

        when(uploadService.upload(any(InputFile.class), any())).thenReturn(
                CompletableFuture.completedFuture(uploadResult("ref-image", UploadMediaKind.IMAGE)),
                CompletableFuture.completedFuture(uploadResult("ref-file", UploadMediaKind.FILE))
        );
        when(client.sendMessage(any(SendMessageRequest.class))).thenReturn(sampleMessage("m-out", "media"));

        Dispatcher dispatcher = new Dispatcher()
                .withBotClient(client)
                .withUploadService(uploadService);
        Router router = new Router("media-reply");
        router.message((message, context) -> {
            context.replyImage(InputFile.fromBytes(new byte[]{1}, "img.jpg"));
            context.replyFile(InputFile.fromBytes(new byte[]{2}, "doc.pdf"));
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate("m-media-in", "ping")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(client, org.mockito.Mockito.times(2)).sendMessage(requestCaptor.capture());
        List<SendMessageRequest> requests = requestCaptor.getAllValues();
        assertEquals("m-media-in", requests.getFirst().replyToMessageId().value());
        assertEquals("ref-image", requests.getFirst().body().attachments().getFirst().input().uploadRef());
        assertEquals("m-media-in", requests.get(1).replyToMessageId().value());
        assertEquals("ref-file", requests.get(1).body().attachments().getFirst().input().uploadRef());
    }

    @Test
    void handlerCanSendVideoAndAudioViaRuntimeContextMediaFacade() {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        UploadService uploadService = Mockito.mock(UploadService.class);

        when(uploadService.upload(any(InputFile.class), any())).thenReturn(
                CompletableFuture.completedFuture(uploadResult(
                        "ref-video",
                        UploadMediaKind.VIDEO,
                        java.util.Map.of("videoToken", "video-token-1")
                )),
                CompletableFuture.completedFuture(uploadResult(
                        "ref-audio",
                        UploadMediaKind.AUDIO,
                        java.util.Map.of("audioToken", "audio-token-1")
                ))
        );
        when(client.sendMessage(any(SendMessageRequest.class))).thenReturn(sampleMessage("m-out", "media"));

        Dispatcher dispatcher = new Dispatcher()
                .withBotClient(client)
                .withUploadService(uploadService);
        Router router = new Router("media-send");
        router.message((message, context) -> {
            context.sendVideo(InputFile.fromBytes(new byte[]{3}, "clip.mp4"));
            context.sendAudio(InputFile.fromBytes(new byte[]{4}, "voice.mp3"));
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate("m-media-send", "ping")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(client, org.mockito.Mockito.times(2)).sendMessage(requestCaptor.capture());
        List<SendMessageRequest> requests = requestCaptor.getAllValues();
        assertEquals("video-token-1", requests.getFirst().body().attachments().getFirst().input().uploadRef());
        assertEquals("audio-token-1", requests.get(1).body().attachments().getFirst().input().uploadRef());
        assertEquals("chat-1", requests.getFirst().chatId().value());
        assertEquals("chat-1", requests.get(1).chatId().value());
    }

    @Test
    void reflectiveHandlerCanResolveMediaMessagingFacadeAndComposeBuilderWithAttachment() throws Exception {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        UploadService uploadService = Mockito.mock(UploadService.class);
        when(uploadService.upload(any(InputFile.class), any())).thenReturn(
                CompletableFuture.completedFuture(uploadResult("ref-media-param", UploadMediaKind.IMAGE))
        );
        when(client.sendMessage(any(SendMessageRequest.class))).thenReturn(sampleMessage("m-out", "ok"));

        Dispatcher dispatcher = new Dispatcher()
                .withBotClient(client)
                .withUploadService(uploadService);
        Router router = new Router("media-reflective");
        MediaReflectiveProbe probe = new MediaReflectiveProbe();
        Method method = MediaReflectiveProbe.class.getDeclaredMethod(
                "onMessage",
                Message.class,
                MediaMessagingFacade.class
        );
        router.message(probe, method);
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate("m-media-ref", "hello")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertTrue(probe.invoked);
        verify(client).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void reflectiveHandlerCanComposeBuilderWithMediaAttachmentFromUploadedResult() throws Exception {
        MaxBotClient client = Mockito.mock(MaxBotClient.class);
        UploadService uploadService = Mockito.mock(UploadService.class);
        when(uploadService.upload(any(InputFile.class), any())).thenReturn(
                CompletableFuture.completedFuture(uploadResult("ref-composed", UploadMediaKind.IMAGE))
        );
        when(client.sendMessage(any(SendMessageRequest.class))).thenReturn(sampleMessage("m-out", "ok"));

        Dispatcher dispatcher = new Dispatcher()
                .withBotClient(client)
                .withUploadService(uploadService);
        Router router = new Router("media-compose");
        BuilderComposeProbe probe = new BuilderComposeProbe();
        Method method = BuilderComposeProbe.class.getDeclaredMethod(
                "onMessage",
                Message.class,
                MessagingFacade.class,
                UploadService.class
        );
        router.message(probe, method);
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate("m-media-compose", "hello")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertTrue(probe.invoked);
        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(client).sendMessage(requestCaptor.capture());
        SendMessageRequest request = requestCaptor.getValue();
        assertEquals("with-media", request.body().text());
        assertEquals("ref-composed", request.body().attachments().getFirst().input().uploadRef());
    }

    private static Update messageUpdate(String messageId, String text) {
        return new Update(
                new UpdateId("upd-" + messageId),
                UpdateType.MESSAGE,
                sampleMessage(messageId, text),
                null,
                null,
                Instant.parse("2026-03-12T00:00:00Z")
        );
    }

    private static Update callbackUpdate(String callbackId, String messageId) {
        return new Update(
                new UpdateId("upd-" + callbackId),
                UpdateType.CALLBACK,
                null,
                new Callback(
                        new CallbackId(callbackId),
                        "button:data",
                        sampleUser(),
                        sampleMessage(messageId, "source"),
                        Instant.parse("2026-03-12T00:00:01Z")
                ),
                null,
                Instant.parse("2026-03-12T00:00:01Z")
        );
    }

    private static Message sampleMessage(String messageId, String text) {
        return new Message(
                new MessageId(messageId),
                new Chat(new ChatId("chat-1"), ChatType.PRIVATE, "chat", null, null),
                sampleUser(),
                text,
                Instant.parse("2026-03-12T00:00:00Z"),
                null,
                List.of(),
                List.of()
        );
    }

    private static User sampleUser() {
        return new User(new UserId("user-1"), "demo", "Demo", "User", "Demo User", false, "en");
    }

    private static UploadResult uploadResult(String ref, UploadMediaKind kind) {
        return uploadResult(ref, kind, java.util.Map.of());
    }

    private static UploadResult uploadResult(String ref, UploadMediaKind kind, java.util.Map<String, String> payload) {
        return new UploadResult(new UploadRef(ref), UploadFlowType.MULTIPART, 10L, null, kind, payload);
    }

    private static final class ReflectiveProbe {
        private boolean invoked;

        @SuppressWarnings("unused")
        public CompletableFuture<Void> onMessage(Message message, MessagingFacade messaging) {
            invoked = true;
            messaging.reply(message, Messages.text("from-reflective"));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class CallbackReflectiveProbe {
        private boolean invoked;

        @SuppressWarnings("unused")
        public CompletableFuture<Void> onCallback(Callback callback, CallbackFacade callbacks) {
            invoked = true;
            callbacks.notify(callback, "accepted");
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class ActionReflectiveProbe {
        private boolean invoked;

        @SuppressWarnings("unused")
        public CompletableFuture<Void> onMessage(Message message, ChatActionsFacade actions) {
            invoked = true;
            actions.typing(message.chat().id());
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class MediaReflectiveProbe {
        private boolean invoked;

        @SuppressWarnings("unused")
        public CompletableFuture<Void> onMessage(Message message, MediaMessagingFacade media) {
            invoked = true;
            media.sendImage(message.chat().id(), InputFile.fromBytes(new byte[]{9}, "p.jpg"));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class BuilderComposeProbe {
        private boolean invoked;

        @SuppressWarnings("unused")
        public CompletableFuture<Void> onMessage(Message message, MessagingFacade messaging, UploadService uploadService) {
            invoked = true;
            UploadResult uploaded = uploadService.upload(InputFile.fromBytes(new byte[]{8}, "img.jpg"))
                    .toCompletableFuture()
                    .join();
            messaging.send(
                    message.chat().id(),
                    Messages.text("with-media").attachment(MediaAttachment.image(uploaded))
            );
            return CompletableFuture.completedFuture(null);
        }
    }
}
