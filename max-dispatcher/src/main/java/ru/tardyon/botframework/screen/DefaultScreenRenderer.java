package ru.tardyon.botframework.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tardyon.botframework.message.Buttons;
import ru.tardyon.botframework.message.InlineKeyboardButton;
import ru.tardyon.botframework.message.Keyboards;
import ru.tardyon.botframework.message.MessageBuilder;
import ru.tardyon.botframework.message.Messages;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.Message;
import ru.tardyon.botframework.model.MessageId;
import ru.tardyon.botframework.model.Update;
import ru.tardyon.botframework.model.request.NewMessageAttachment;

/**
 * Default renderer that maps {@link ScreenModel} to one editable chat message.
 */
public final class DefaultScreenRenderer implements ScreenRenderer {
    private static final Logger log = LoggerFactory.getLogger(DefaultScreenRenderer.class);
    private final ScreenCallbackCodec callbackCodec;

    public DefaultScreenRenderer() {
        this(new LegacyStringScreenActionCodec());
    }

    public DefaultScreenRenderer(ScreenActionCodec actionCodec) {
        this.callbackCodec = new ScreenCallbackCodec(actionCodec);
    }

    @Override
    public CompletionStage<RenderResult> render(ScreenContext context, ScreenModel model) {
        ArrayList<String> lines = new ArrayList<>();
        if (model.title() != null && !model.title().isBlank()) {
            lines.add(model.title());
        }

        ArrayList<List<ScreenButton>> buttonRows = new ArrayList<>();
        ArrayList<NewMessageAttachment> attachments = new ArrayList<>();
        return renderWidgets(context, model.widgets()).thenApply(renders -> {
            for (WidgetRender render : renders) {
                lines.addAll(render.textLines());
                buttonRows.addAll(render.buttons());
                attachments.addAll(render.attachments());
            }
            if (model.showBackButton() && context.session().canGoBack()) {
                buttonRows.add(List.of(ScreenButton.of("Назад", "__nav_back")));
            }
            return new RenderPayload(composeText(lines), buttonRows, attachments);
        }).thenApply(payload -> sendOrEdit(context, payload.text(), payload.buttons(), payload.attachments()));
    }

    private RenderResult sendOrEdit(
            ScreenContext context,
            String text,
            List<List<ScreenButton>> buttons,
            List<NewMessageAttachment> attachments
    ) {
        ChatId chatId = currentChatId(context.runtime().update());
        MessageBuilder builder = Messages.text(text);
        for (NewMessageAttachment attachment : attachments) {
            builder = builder.attachment(attachment);
        }
        if (!buttons.isEmpty()) {
            builder = builder.keyboard(Keyboards.inline(keyboard -> {
                for (List<ScreenButton> row : buttons) {
                    ArrayList<InlineKeyboardButton> mapped = new ArrayList<>();
                    for (ScreenButton button : row) {
                        String payload = "__nav_back".equals(button.action())
                                ? callbackCodec.navBack()
                                : callbackCodec.action(button.action(), button.args());
                        mapped.add(Buttons.callback(button.text(), payload));
                    }
                    keyboard.row(mapped);
                }
                return keyboard;
            }));
        }

        String rootMessageId = context.session().rootMessageId();
        if (chatId != null && rootMessageId != null && !rootMessageId.isBlank() && !isUnknownMessageId(rootMessageId)) {
            try {
                boolean edited = context.runtime().messaging().edit(chatId, new MessageId(rootMessageId), builder);
                if (edited) {
                    return new RenderResult(rootMessageId, true);
                }
            } catch (RuntimeException runtimeException) {
                log.debug("Screen edit failed for messageId={}; fallback to send", rootMessageId, runtimeException);
            }
        }
        if (chatId == null) {
            throw new IllegalStateException("Cannot resolve chat id for screen rendering");
        }
        Message sent = context.runtime().messaging().send(chatId, builder);
        if (rootMessageId != null && !rootMessageId.isBlank() && !isUnknownMessageId(rootMessageId)
                && sent.messageId() != null && !rootMessageId.equals(sent.messageId().value())) {
            try {
                boolean deleted = context.runtime().messaging().delete(new MessageId(rootMessageId));
                if (!deleted) {
                    log.debug("Screen fallback send: previous screen delete returned false (messageId={})", rootMessageId);
                }
            } catch (RuntimeException runtimeException) {
                log.debug("Screen fallback send: failed to delete previous screen (messageId={})", rootMessageId, runtimeException);
            }
        }
        return new RenderResult(sent.messageId().value(), false);
    }

    private static CompletionStage<List<WidgetRender>> renderWidgets(ScreenContext context, List<Widget> widgets) {
        CompletionStage<List<WidgetRender>> stage = CompletableFuture.completedFuture(new ArrayList<>());
        for (Widget widget : widgets) {
            stage = stage.thenCompose(items -> widget.render(context).thenApply(render -> {
                items.add(render);
                return items;
            }));
        }
        return stage.thenApply(List::copyOf);
    }

    private static String composeText(List<String> lines) {
        String joined = lines.stream()
                .filter(line -> line != null)
                .map(String::stripTrailing)
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("...");
        if (joined.isBlank()) {
            return "...";
        }
        return joined;
    }

    private static ChatId currentChatId(Update update) {
        if (update.message() != null && update.message().chat() != null) {
            return update.message().chat().id();
        }
        if (update.callback() != null && update.callback().message() != null && update.callback().message().chat() != null) {
            return update.callback().message().chat().id();
        }
        return null;
    }

    private static boolean isUnknownMessageId(String value) {
        return value.endsWith("-unknown");
    }

    private record RenderPayload(
            String text,
            List<List<ScreenButton>> buttons,
            List<NewMessageAttachment> attachments
    ) {
    }
}
