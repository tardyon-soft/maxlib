package ru.max.botframework.dispatcher;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import ru.max.botframework.action.ChatActionsFacade;
import ru.max.botframework.callback.CallbackFacade;
import ru.max.botframework.fsm.FSMContext;
import ru.max.botframework.message.MessageBuilder;
import ru.max.botframework.message.MediaMessagingFacade;
import ru.max.botframework.message.MessagingFacade;
import ru.max.botframework.model.Callback;
import ru.max.botframework.model.ChatAction;
import ru.max.botframework.model.ChatId;
import ru.max.botframework.model.Message;
import ru.max.botframework.model.Update;
import ru.max.botframework.upload.InputFile;

/**
 * Request-scoped runtime context used by middleware and future DI/resolution layers.
 *
 * <p>Context exposes:
 * request attributes (`ContextKey`) and typed runtime data container (`RuntimeDataContainer`).
 * Runtime data keeps framework/filter/middleware/application scopes isolated per update lifecycle.</p>
 */
public final class RuntimeContext {
    private static final String CONTEXT_KEY_PREFIX = "ctx.";
    private final Update update;
    private final Map<ContextKey<?>, Object> attributes;
    private final RuntimeDataContainer data;

    public RuntimeContext(Update update) {
        this.update = Objects.requireNonNull(update, "update");
        this.attributes = new ConcurrentHashMap<>();
        this.data = new RuntimeDataContainer();
    }

    public Update update() {
        return update;
    }

    public <T> RuntimeContext put(ContextKey<T> key, T value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        if (!key.type().isInstance(value)) {
            throw new IllegalArgumentException("value type does not match context key type");
        }
        attributes.put(key, value);
        data.put(RuntimeDataKey.framework(contextKeyName(key), key.type()), value);
        return this;
    }

    public <T> Optional<T> get(ContextKey<T> key) {
        Objects.requireNonNull(key, "key");
        Object raw = attributes.get(key);
        if (raw == null) {
            return Optional.empty();
        }
        if (!key.type().isInstance(raw)) {
            return Optional.empty();
        }
        return Optional.of(key.type().cast(raw));
    }

    public Map<ContextKey<?>, Object> attributes() {
        return Map.copyOf(attributes);
    }

    public RuntimeDataContainer data() {
        return data;
    }

    public <T> RuntimeContext putData(RuntimeDataKey<T> key, T value) {
        data.put(key, value);
        return this;
    }

    public <T> RuntimeContext replaceData(RuntimeDataKey<T> key, T value) {
        data.replace(key, value);
        return this;
    }

    public <T> Optional<T> dataValue(RuntimeDataKey<T> key) {
        return data.get(key);
    }

    public <T> RuntimeContext putEnrichment(ContextKey<T> key, T value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        if (!key.type().isInstance(value)) {
            throw new IllegalArgumentException("value type does not match context key type");
        }
        putEnrichmentValue(key.name(), value, RuntimeDataScope.MIDDLEWARE, "context key");
        return this;
    }

    public RuntimeContext putEnrichment(String key, Object value) {
        putEnrichmentValue(key, value, RuntimeDataScope.MIDDLEWARE, "middleware");
        return this;
    }

    /**
     * @deprecated Prefer {@link #putEnrichment(String, Object)} for middleware and internal filter merge path.
     */
    @Deprecated(forRemoval = false)
    public RuntimeContext putAllEnrichment(Map<String, Object> values) {
        return mergeFilterEnrichment(values);
    }

    /**
     * Framework-internal filter enrichment merge.
     *
     * <p>Conflict policy: if the same key already exists with a different value,
     * {@link EnrichmentConflictException} is thrown.</p>
     */
    RuntimeContext mergeFilterEnrichment(Map<String, Object> values) {
        Objects.requireNonNull(values, "values");
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            putEnrichmentValue(entry.getKey(), entry.getValue(), RuntimeDataScope.FILTER, "filter");
        }
        return this;
    }

    public <T> Optional<T> enrichmentValue(ContextKey<T> key) {
        Objects.requireNonNull(key, "key");
        return enrichmentValue(key.name(), key.type());
    }

    public <T> Optional<T> enrichmentValue(String key, Class<T> type) {
        return data.find(key, Objects.requireNonNull(type, "type"));
    }

    public Optional<Object> enrichmentValue(String key) {
        return data.find(Objects.requireNonNull(key, "key"));
    }

    public Map<String, Object> enrichment() {
        return data.snapshot(RuntimeDataScope.FILTER, RuntimeDataScope.MIDDLEWARE);
    }

    /**
     * Returns runtime messaging facade if bot client is configured on dispatcher.
     */
    public MessagingFacade messaging() {
        return dataValue(RuntimeMessagingSupport.MESSAGING_FACADE_KEY)
                .orElseThrow(() -> new IllegalStateException(
                        "MessagingFacade is unavailable. Register MaxBotClient via Dispatcher.withBotClient(...)"
                ));
    }

    /**
     * Returns runtime callback facade if bot client is configured on dispatcher.
     */
    public CallbackFacade callbacks() {
        return dataValue(RuntimeMessagingSupport.CALLBACK_FACADE_KEY)
                .orElseThrow(() -> new IllegalStateException(
                        "CallbackFacade is unavailable. Register MaxBotClient via Dispatcher.withBotClient(...)"
                ));
    }

    /**
     * Returns runtime chat actions facade if bot client is configured on dispatcher.
     */
    public ChatActionsFacade actions() {
        return dataValue(RuntimeMessagingSupport.CHAT_ACTIONS_FACADE_KEY)
                .orElseThrow(() -> new IllegalStateException(
                        "ChatActionsFacade is unavailable. Register MaxBotClient via Dispatcher.withBotClient(...)"
                ));
    }

    /**
     * Returns runtime media facade if bot client and upload service are configured on dispatcher.
     */
    public MediaMessagingFacade media() {
        return dataValue(RuntimeMessagingSupport.MEDIA_MESSAGING_FACADE_KEY)
                .orElseThrow(() -> new IllegalStateException(
                        "MediaMessagingFacade is unavailable. Register MaxBotClient via Dispatcher.withBotClient(...) "
                                + "and UploadService via Dispatcher.withUploadService(...)"
                ));
    }

    /**
     * Returns runtime FSM context if FSM storage is configured on dispatcher.
     */
    public FSMContext fsm() {
        return FSMRuntimeSupport.resolve(this)
                .orElseThrow(() -> new IllegalStateException(
                        "FSMContext is unavailable. Configure Dispatcher.withFsmStorage(...) and state scope strategy."
                ));
    }

    /**
     * Replies to current message (message update or callback source message).
     */
    public Message reply(MessageBuilder builder) {
        Objects.requireNonNull(builder, "builder");
        Message source = currentMessage();
        return messaging().reply(source, builder);
    }

    /**
     * Sends callback answer for current callback update.
     */
    public boolean answerCallback(String text) {
        return callbacks().notify(currentCallback(), text);
    }

    /**
     * Sends chat action for current update chat context.
     */
    public boolean chatAction(ChatAction action) {
        Objects.requireNonNull(action, "action");
        return actions().send(this, action);
    }

    /**
     * Replies to current message context with uploaded image.
     */
    public Message replyImage(InputFile inputFile) {
        return media().replyImage(currentMessage(), Objects.requireNonNull(inputFile, "inputFile"));
    }

    /**
     * Replies to current message context with uploaded file.
     */
    public Message replyFile(InputFile inputFile) {
        return media().replyFile(currentMessage(), Objects.requireNonNull(inputFile, "inputFile"));
    }

    /**
     * Sends uploaded video to current chat context.
     */
    public Message sendVideo(InputFile inputFile) {
        return media().sendVideo(currentChatId(), Objects.requireNonNull(inputFile, "inputFile"));
    }

    /**
     * Sends uploaded audio to current chat context.
     */
    public Message sendAudio(InputFile inputFile) {
        return media().sendAudio(currentChatId(), Objects.requireNonNull(inputFile, "inputFile"));
    }

    private Message currentMessage() {
        if (update.message() != null) {
            return update.message();
        }
        if (update.callback() != null && update.callback().message() != null) {
            return update.callback().message();
        }
        throw new IllegalStateException("current update does not contain message context");
    }

    private Callback currentCallback() {
        if (update.callback() != null) {
            return update.callback();
        }
        throw new IllegalStateException("current update does not contain callback context");
    }

    private ChatId currentChatId() {
        return currentMessage().chat().id();
    }

    private void putEnrichmentValue(String key, Object value, RuntimeDataScope scope, String source) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        if (key.isBlank()) {
            throw new IllegalArgumentException("enrichment key must not be blank");
        }
        try {
            data.put(new RuntimeDataKey<>(key, Object.class, scope), value);
        } catch (RuntimeDataConflictException conflict) {
            throw EnrichmentConflictException.conflict(
                    conflict.keyName(),
                    conflict.existingValue(),
                    conflict.incomingValue(),
                    source
            );
        }
    }

    private static <T> String contextKeyName(ContextKey<T> key) {
        return CONTEXT_KEY_PREFIX + key.name();
    }
}
