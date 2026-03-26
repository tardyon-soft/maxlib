package ru.tardyon.botframework.screen;

import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * Screen stack navigation API.
 */
public interface ScreenNavigator {
    CompletionStage<Void> start(String screenId, Map<String, Object> params);

    CompletionStage<Void> push(String screenId, Map<String, Object> params);

    CompletionStage<Void> replace(String screenId, Map<String, Object> params);

    CompletionStage<Boolean> back();

    CompletionStage<Void> rerender();

    CompletionStage<ScreenSession> session();

    CompletionStage<Void> clear();
}
