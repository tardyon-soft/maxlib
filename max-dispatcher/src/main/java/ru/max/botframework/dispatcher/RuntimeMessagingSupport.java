package ru.max.botframework.dispatcher;

import java.util.Objects;
import java.util.Optional;
import ru.max.botframework.action.ChatActionsFacade;
import ru.max.botframework.callback.CallbackFacade;
import ru.max.botframework.client.MaxBotClient;
import ru.max.botframework.message.MediaMessagingFacade;
import ru.max.botframework.message.MessagingFacade;
import ru.max.botframework.upload.UploadService;

final class RuntimeMessagingSupport {
    static final RuntimeDataKey<MaxBotClient> BOT_CLIENT_KEY =
            RuntimeDataKey.application("service:" + MaxBotClient.class.getName(), MaxBotClient.class);
    static final RuntimeDataKey<UploadService> UPLOAD_SERVICE_KEY =
            RuntimeDataKey.application("service:" + UploadService.class.getName(), UploadService.class);
    static final RuntimeDataKey<MessagingFacade> MESSAGING_FACADE_KEY =
            RuntimeDataKey.framework("runtime.messaging.facade", MessagingFacade.class);
    static final RuntimeDataKey<MediaMessagingFacade> MEDIA_MESSAGING_FACADE_KEY =
            RuntimeDataKey.framework("runtime.media-messaging.facade", MediaMessagingFacade.class);
    static final RuntimeDataKey<CallbackFacade> CALLBACK_FACADE_KEY =
            RuntimeDataKey.framework("runtime.callback.facade", CallbackFacade.class);
    static final RuntimeDataKey<ChatActionsFacade> CHAT_ACTIONS_FACADE_KEY =
            RuntimeDataKey.framework("runtime.chat-actions.facade", ChatActionsFacade.class);

    private RuntimeMessagingSupport() {
    }

    static void bootstrap(RuntimeContext context) {
        Objects.requireNonNull(context, "context");
        Optional<MaxBotClient> clientOptional = context.dataValue(BOT_CLIENT_KEY);
        if (clientOptional.isEmpty()) {
            return;
        }

        MaxBotClient client = clientOptional.orElseThrow();
        MessagingFacade messagingFacade = new MessagingFacade(client);
        context.putData(MESSAGING_FACADE_KEY, messagingFacade);
        context.putData(CALLBACK_FACADE_KEY, new CallbackFacade(client));
        context.putData(CHAT_ACTIONS_FACADE_KEY, new ChatActionsFacade(client));

        context.dataValue(UPLOAD_SERVICE_KEY)
                .ifPresent(uploadService -> context.putData(
                        MEDIA_MESSAGING_FACADE_KEY,
                        new MediaMessagingFacade(uploadService, messagingFacade)
                ));
    }
}
