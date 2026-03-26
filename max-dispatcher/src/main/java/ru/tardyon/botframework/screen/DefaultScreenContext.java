package ru.tardyon.botframework.screen;

import java.util.Map;
import java.util.Objects;
import ru.tardyon.botframework.dispatcher.RuntimeContext;
import ru.tardyon.botframework.fsm.FSMContext;

final class DefaultScreenContext implements ScreenContext {
    private final RuntimeContext runtime;
    private final FSMContext fsm;
    private final ScreenSession session;
    private final Map<String, Object> params;
    private final ScreenNavigator navigator;

    DefaultScreenContext(
            RuntimeContext runtime,
            FSMContext fsm,
            ScreenSession session,
            Map<String, Object> params,
            ScreenNavigator navigator
    ) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.fsm = Objects.requireNonNull(fsm, "fsm");
        this.session = Objects.requireNonNull(session, "session");
        this.params = Map.copyOf(Objects.requireNonNull(params, "params"));
        this.navigator = Objects.requireNonNull(navigator, "navigator");
    }

    @Override
    public RuntimeContext runtime() {
        return runtime;
    }

    @Override
    public FSMContext fsm() {
        return fsm;
    }

    @Override
    public ScreenSession session() {
        return session;
    }

    @Override
    public Map<String, Object> params() {
        return params;
    }

    @Override
    public ScreenNavigator nav() {
        return navigator;
    }
}
