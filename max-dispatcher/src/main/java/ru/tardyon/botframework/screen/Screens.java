package ru.tardyon.botframework.screen;

import java.util.Objects;
import ru.tardyon.botframework.dispatcher.RuntimeDataKey;
import ru.tardyon.botframework.dispatcher.RuntimeContext;
import ru.tardyon.botframework.fsm.FSMContext;

/**
 * Entrypoint for creating screen navigator over runtime context.
 */
public final class Screens {
    private static final String SCREEN_FSM_NAMESPACE = "screen";
    private static final RuntimeDataKey<ScreenActionCodec> SCREEN_ACTION_CODEC_KEY =
            RuntimeDataKey.application("service:" + ScreenActionCodec.class.getName(), ScreenActionCodec.class);

    private Screens() {
    }

    public static ScreenNavigator navigator(RuntimeContext context, ScreenRegistry registry) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(registry, "registry");
        FSMContext fsm = context.fsm(SCREEN_FSM_NAMESPACE);
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
