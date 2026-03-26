package ru.tardyon.botframework.dispatcher;

import java.util.Objects;
import java.util.Optional;
import ru.tardyon.botframework.fsm.FSMContext;
import ru.tardyon.botframework.fsm.FSMStorage;
import ru.tardyon.botframework.fsm.StateKey;
import ru.tardyon.botframework.fsm.StateKeyStrategy;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.UserId;

final class FSMRuntimeSupport {
    static final RuntimeDataKey<FSMStorage> FSM_STORAGE_KEY =
            RuntimeDataKey.framework("runtime.fsm.storage", FSMStorage.class);
    static final RuntimeDataKey<StateKeyStrategy> STATE_KEY_STRATEGY_KEY =
            RuntimeDataKey.framework("runtime.fsm.state-key-strategy", StateKeyStrategy.class);

    private FSMRuntimeSupport() {
    }

    static void bootstrap(RuntimeContext context, FSMStorage storage, StateKeyStrategy stateKeyStrategy) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(stateKeyStrategy, "stateKeyStrategy");
        if (storage == null) {
            return;
        }
        context.putData(FSM_STORAGE_KEY, storage);
        context.putData(STATE_KEY_STRATEGY_KEY, stateKeyStrategy);
    }

    static Optional<FSMContext> resolve(RuntimeContext context) {
        Objects.requireNonNull(context, "context");

        Optional<FSMStorage> storageOpt = context.dataValue(FSM_STORAGE_KEY);
        if (storageOpt.isEmpty()) {
            return Optional.empty();
        }

        StateKeyStrategy strategy = context.dataValue(STATE_KEY_STRATEGY_KEY)
                .orElseThrow(() -> new IllegalStateException("StateKeyStrategy is not configured in runtime context"));
        return Optional.of(FSMContext.of(storageOpt.orElseThrow(), strategy.resolve(context.update())));
    }

    static Optional<FSMContext> resolveNamespaced(RuntimeContext context, String namespace) {
        Objects.requireNonNull(context, "context");
        String normalizedNamespace = normalizeNamespace(namespace);

        Optional<FSMStorage> storageOpt = context.dataValue(FSM_STORAGE_KEY);
        if (storageOpt.isEmpty()) {
            return Optional.empty();
        }

        StateKeyStrategy strategy = context.dataValue(STATE_KEY_STRATEGY_KEY)
                .orElseThrow(() -> new IllegalStateException("StateKeyStrategy is not configured in runtime context"));
        StateKey baseKey = strategy.resolve(context.update());
        return Optional.of(FSMContext.of(storageOpt.orElseThrow(), namespacedKey(baseKey, normalizedNamespace)));
    }

    private static StateKey namespacedKey(StateKey baseKey, String namespace) {
        return switch (baseKey.scope()) {
            case USER -> StateKey.user(new UserId(namespace + "::" + baseKey.userId().value()));
            case CHAT -> StateKey.chat(new ChatId(namespace + "::" + baseKey.chatId().value()));
            case USER_IN_CHAT -> StateKey.userInChat(
                    new UserId(namespace + "::" + baseKey.userId().value()),
                    new ChatId(namespace + "::" + baseKey.chatId().value())
            );
        };
    }

    private static String normalizeNamespace(String namespace) {
        Objects.requireNonNull(namespace, "namespace");
        String value = namespace.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("namespace must not be blank");
        }
        return value;
    }
}
