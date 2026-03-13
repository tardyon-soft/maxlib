package ru.max.botframework.dispatcher;

import java.util.Objects;
import java.util.Optional;
import ru.max.botframework.action.ChatActionsFacade;
import ru.max.botframework.callback.CallbackFacade;
import ru.max.botframework.client.MaxBotClient;
import ru.max.botframework.message.MessagingFacade;

final class RuntimeMessagingSupport {
    static final RuntimeDataKey<MaxBotClient> BOT_CLIENT_KEY =
            RuntimeDataKey.application("service:" + MaxBotClient.class.getName(), MaxBotClient.class);
    static final RuntimeDataKey<MessagingFacade> MESSAGING_FACADE_KEY =
            RuntimeDataKey.framework("runtime.messaging.facade", MessagingFacade.class);
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
        context.putData(MESSAGING_FACADE_KEY, new MessagingFacade(client));
        context.putData(CALLBACK_FACADE_KEY, new CallbackFacade(client));
        context.putData(CHAT_ACTIONS_FACADE_KEY, new ChatActionsFacade(client));
    }
}
