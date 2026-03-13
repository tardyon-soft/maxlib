package ru.max.botframework.fsm;

/**
 * Scene lifecycle context bound to one FSM scope/session.
 */
public interface SceneContext {

    FSMContext fsm();

    SceneSession session();

    default StateKey scope() {
        return fsm().scope();
    }
}
