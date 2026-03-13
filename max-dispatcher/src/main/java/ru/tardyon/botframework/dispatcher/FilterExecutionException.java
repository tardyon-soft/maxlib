package ru.tardyon.botframework.dispatcher;

final class FilterExecutionException extends RuntimeException {

    private FilterExecutionException(Throwable cause) {
        super("filter execution failed", cause);
    }

    static FilterExecutionException wrap(Throwable error) {
        if (error instanceof FilterExecutionException existing) {
            return existing;
        }
        return new FilterExecutionException(error);
    }

    Throwable rootCause() {
        return getCause() == null ? this : getCause();
    }
}
