package ru.max.botframework.dispatcher;

final class MiddlewareExecutionException extends RuntimeException {

    enum Phase {
        OUTER,
        INNER
    }

    private final Phase phase;

    private MiddlewareExecutionException(Phase phase, Throwable cause) {
        super("middleware execution failed in %s phase".formatted(phase), cause);
        this.phase = phase;
    }

    static MiddlewareExecutionException wrap(Phase phase, Throwable error) {
        if (error instanceof MiddlewareExecutionException existing) {
            return existing;
        }
        return new MiddlewareExecutionException(phase, error);
    }

    Phase phase() {
        return phase;
    }

    Throwable rootCause() {
        return getCause() == null ? this : getCause();
    }
}
