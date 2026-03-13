package ru.tardyon.botframework.fsm;

import ru.tardyon.botframework.model.Update;

/**
 * Resolves {@link StateKey} from an incoming update according to configured scope.
 */
public interface StateKeyStrategy {

    StateScope scope();

    StateKey resolve(Update update);
}
