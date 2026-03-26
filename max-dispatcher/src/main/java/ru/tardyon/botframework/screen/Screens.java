package ru.tardyon.botframework.screen;

import java.util.Objects;
import ru.tardyon.botframework.dispatcher.RuntimeContext;
import ru.tardyon.botframework.fsm.FSMContext;

/**
 * Entrypoint for creating screen navigator over runtime context.
 */
public final class Screens {
    private Screens() {
    }

    public static ScreenNavigator navigator(RuntimeContext context, ScreenRegistry registry) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(registry, "registry");
        FSMContext fsm = context.fsm();
        return new DefaultScreenNavigator(context, fsm, registry, new FsmScreenStorage(), new DefaultScreenRenderer());
    }
}
