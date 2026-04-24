package ru.tardyon.botframework.screen;

import java.util.Objects;
import ru.tardyon.botframework.dispatcher.RuntimeDataKey;
import ru.tardyon.botframework.dispatcher.RuntimeContext;
import ru.tardyon.botframework.fsm.FSMContext;

/**
 * Entrypoint for creating screen navigator over runtime context.
 */
public final class Screens {
    public static final String DEFAULT_SCREEN_FSM_NAMESPACE = "screen";
    public static final RuntimeDataKey<String> SCREEN_FSM_NAMESPACE_KEY =
            RuntimeDataKey.application("service:" + Screens.class.getName() + ".fsmNamespace", String.class);
    private static final RuntimeDataKey<ScreenActionCodec> SCREEN_ACTION_CODEC_KEY =
            RuntimeDataKey.application("service:" + ScreenActionCodec.class.getName(), ScreenActionCodec.class);

    private Screens() {
    }

    public static ScreenNavigator navigator(RuntimeContext context, ScreenRegistry registry) {
        String namespace = context.dataValue(SCREEN_FSM_NAMESPACE_KEY)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .orElse(DEFAULT_SCREEN_FSM_NAMESPACE);
        return navigator(context, registry, namespace);
    }

    public static ScreenNavigator navigator(RuntimeContext context, ScreenRegistry registry, String fsmNamespace) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(fsmNamespace, "fsmNamespace");
        String namespace = fsmNamespace.trim();
        if (namespace.isBlank()) {
            throw new IllegalArgumentException("fsmNamespace must not be blank");
        }
        FSMContext fsm = context.fsm(namespace);
        ScreenActionCodec actionCodec = context.dataValue(SCREEN_ACTION_CODEC_KEY).orElseGet(LegacyStringScreenActionCodec::new);
        return new DefaultScreenNavigator(
                context,
                fsm,
                registry,
                new FsmScreenStorage(context.fsm()),
                new DefaultScreenRenderer(actionCodec)
        );
    }
}
