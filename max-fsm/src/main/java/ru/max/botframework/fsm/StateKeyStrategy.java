package ru.max.botframework.fsm;

import ru.max.botframework.model.Update;

/**
 * Resolves {@link StateKey} from an incoming update according to configured scope.
 */
public interface StateKeyStrategy {

    StateScope scope();

    StateKey resolve(Update update);
}
