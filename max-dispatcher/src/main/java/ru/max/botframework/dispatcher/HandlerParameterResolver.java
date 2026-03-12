package ru.max.botframework.dispatcher;

/**
 * Resolves one handler parameter from invocation context.
 */
@FunctionalInterface
public interface HandlerParameterResolver {

    /**
     * Returns resolved value or {@link HandlerParameterResolution#unsupported()}.
     */
    HandlerParameterResolution resolve(HandlerParameterDescriptor parameter, HandlerInvocationContext context);
}
