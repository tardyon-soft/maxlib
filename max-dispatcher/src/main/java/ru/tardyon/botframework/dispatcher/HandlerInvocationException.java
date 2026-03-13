package ru.tardyon.botframework.dispatcher;

final class HandlerInvocationException extends RuntimeException {

    private HandlerInvocationException(Throwable cause) {
        super("handler invocation failed", cause);
    }

    static HandlerInvocationException wrap(Throwable error) {
        if (error instanceof HandlerInvocationException existing) {
            return existing;
        }
        return new HandlerInvocationException(error);
    }

    Throwable rootCause() {
        return getCause() == null ? this : getCause();
    }
}
