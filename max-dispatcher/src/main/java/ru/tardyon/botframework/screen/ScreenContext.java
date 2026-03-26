package ru.tardyon.botframework.screen;

import java.util.Map;
import ru.tardyon.botframework.dispatcher.RuntimeContext;
import ru.tardyon.botframework.fsm.FSMContext;

/**
 * Screen runtime context for rendering and actions.
 */
public interface ScreenContext {
    RuntimeContext runtime();

    FSMContext fsm();

    ScreenSession session();

    Map<String, Object> params();

    ScreenNavigator nav();
}
