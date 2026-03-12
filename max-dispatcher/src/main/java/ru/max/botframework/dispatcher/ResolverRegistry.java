package ru.max.botframework.dispatcher;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Ordered registry of handler parameter resolvers.
 */
public final class ResolverRegistry {
    private final CopyOnWriteArrayList<HandlerParameterResolver> resolvers = new CopyOnWriteArrayList<>();

    public ResolverRegistry register(HandlerParameterResolver resolver) {
        resolvers.add(Objects.requireNonNull(resolver, "resolver"));
        return this;
    }

    public List<HandlerParameterResolver> resolvers() {
        return List.copyOf(resolvers);
    }

    public Optional<Object> resolve(HandlerParameterDescriptor parameter, HandlerInvocationContext context) {
        Objects.requireNonNull(parameter, "parameter");
        Objects.requireNonNull(context, "context");
        for (HandlerParameterResolver resolver : resolvers) {
            HandlerParameterResolution resolution;
            try {
                resolution = Objects.requireNonNull(
                        resolver.resolve(parameter, context),
                        "resolver result"
                );
            } catch (ParameterResolutionException known) {
                throw known;
            } catch (Throwable throwable) {
                throw new ResolverExecutionException(resolver.getClass(), throwable);
            }
            if (resolution.supported()) {
                return Optional.of(resolution.value());
            }
        }
        return Optional.empty();
    }
}
