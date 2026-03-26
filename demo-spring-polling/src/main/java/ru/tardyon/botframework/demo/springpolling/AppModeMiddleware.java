package ru.tardyon.botframework.demo.springpolling;

import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tardyon.botframework.dispatcher.DispatchResult;
import ru.tardyon.botframework.dispatcher.InnerMiddleware;
import ru.tardyon.botframework.dispatcher.MiddlewareNext;
import ru.tardyon.botframework.dispatcher.RuntimeContext;
import ru.tardyon.botframework.message.MessagingFacade;
import ru.tardyon.botframework.model.Message;
import ru.tardyon.botframework.model.MessageId;
import ru.tardyon.botframework.model.Update;

/**
 * Middleware that creates an "app-like" UX by deleting processed messages.
 *
 * <p>Before handler execution:
 * <ul>
 *   <li>Deletes the user's incoming text message (so it doesn't clutter the chat)</li>
 *   <li>Deletes the previous bot message (stored in FSM data under {@code _lastBotMsgId})</li>
 * </ul>
 *
 * <p>After handler execution the handler is expected to store the new bot message ID
 * in FSM data via {@code ctx.fsm().updateData(Map.of("_lastBotMsgId", sentMessage.messageId().value()))}.
 *
 * <p>For callback updates, no deletion is needed — handlers should use
 * {@code ctx.callbacks().updateCurrentMessage()} to edit the message in place.
 */
public final class AppModeMiddleware implements InnerMiddleware {

    private static final Logger log = LoggerFactory.getLogger(AppModeMiddleware.class);
    static final String LAST_BOT_MSG_KEY = "_lastBotMsgId";

    @Override
    public CompletionStage<DispatchResult> invoke(RuntimeContext context, MiddlewareNext next) {
        Update update = context.update();

        // For callback updates — just proceed, handlers edit message in place
        if (update.callback() != null) {
            return next.proceed();
        }

        // For message updates — delete user message + previous bot message, then proceed
        Message userMessage = update.message();
        if (userMessage == null) {
            return next.proceed();
        }

        MessagingFacade messaging = context.messaging();

        // Delete the user's message
        tryDelete(messaging, userMessage.messageId(), "user message");

        // Delete previous bot message from FSM data
        return context.fsm().data().thenCompose(stateData -> {
            stateData.get(LAST_BOT_MSG_KEY, String.class)
                    .ifPresent(prevId -> tryDelete(messaging, new MessageId(prevId), "previous bot message"));
            return next.proceed();
        });
    }

    private static void tryDelete(MessagingFacade messaging, MessageId messageId, String label) {
        try {
            boolean deleted = messaging.delete(messageId);
            if (!deleted) {
                if (isSyntheticUnknownId(messageId)) {
                    log.debug("App-mode: skipping delete {} (id={}) — synthetic unknown id", label, messageId.value());
                } else {
                    log.warn("App-mode: delete {} (id={}) returned false — API refused deletion", label, messageId.value());
                }
            } else {
                log.info("App-mode: deleted {} (id={})", label, messageId.value());
            }
        } catch (Exception e) {
            log.warn("App-mode: failed to delete {} (id={}): {} — {}", label, messageId.value(), e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private static boolean isSyntheticUnknownId(MessageId messageId) {
        return messageId != null && messageId.value() != null && messageId.value().startsWith("msg-unknown");
    }
}
