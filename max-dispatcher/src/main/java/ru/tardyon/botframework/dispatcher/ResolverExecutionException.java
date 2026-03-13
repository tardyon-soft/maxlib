package ru.tardyon.botframework.dispatcher;

/**
 * Internal marker for resolver-level execution failure.
 */
final class ResolverExecutionException extends RuntimeException {
    private final Class<?> resolverType;

    ResolverExecutionException(Class<?> resolverType, Throwable cause) {
        super(cause);
        this.resolverType = resolverType;
    }

    Class<?> resolverType() {
        return resolverType;
    }
}
