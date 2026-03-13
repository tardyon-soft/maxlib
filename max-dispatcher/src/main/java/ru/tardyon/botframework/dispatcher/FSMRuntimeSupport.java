package ru.tardyon.botframework.dispatcher;

import java.util.Objects;
import java.util.Optional;
import ru.tardyon.botframework.fsm.FSMContext;
import ru.tardyon.botframework.fsm.FSMStorage;
import ru.tardyon.botframework.fsm.StateKeyStrategy;

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
}
