package ru.tardyon.botframework.dispatcher;

/**
 * Resolves one handler parameter from invocation context.
 *
 * <p>This is SPI: custom implementations can be registered in {@link ResolverRegistry} and used by
 * {@link DefaultHandlerInvoker}.</p>
 */
@FunctionalInterface
public interface HandlerParameterResolver {

    /**
     * Returns resolved value or {@link HandlerParameterResolution#unsupported()}.
     *
     * <p>Resolver must be deterministic and side-effect free. If resolver cannot provide value for
     * this parameter, it should return {@code unsupported()} and allow next resolver to try.</p>
     *
     * <p>In case of explicit resolution failure (for example, ambiguous candidates) resolver should throw
     * {@link ParameterResolutionException}.</p>
     */
    HandlerParameterResolution resolve(HandlerParameterDescriptor parameter, HandlerInvocationContext context) throws NoSuchMethodException;
}
