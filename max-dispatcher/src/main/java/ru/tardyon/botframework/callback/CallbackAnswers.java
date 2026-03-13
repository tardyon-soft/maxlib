package ru.tardyon.botframework.callback;

/**
 * Factory entrypoints for callback answer builder.
 */
public final class CallbackAnswers {
    private CallbackAnswers() {
    }

    public static CallbackAnswerBuilder answer() {
        return CallbackAnswerBuilder.empty();
    }

    public static CallbackAnswerBuilder text(String text) {
        return CallbackAnswerBuilder.withText(text);
    }
}
