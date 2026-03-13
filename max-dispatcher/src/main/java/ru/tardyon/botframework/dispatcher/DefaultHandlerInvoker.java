package ru.tardyon.botframework.dispatcher;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import ru.tardyon.botframework.model.Callback;
import ru.tardyon.botframework.model.Chat;
import ru.tardyon.botframework.model.Message;
import ru.tardyon.botframework.model.Update;
import ru.tardyon.botframework.model.User;

/**
 * Default reflective handler invoker with lightweight method metadata cache.
 */
public final class DefaultHandlerInvoker implements HandlerInvoker {
    private final ResolverRegistry registry;
    private final ConcurrentHashMap<Method, HandlerMethodMetadata> metadataCache = new ConcurrentHashMap<>();

    public DefaultHandlerInvoker(ResolverRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public static DefaultHandlerInvoker withDefaults() {
        return new DefaultHandlerInvoker(new ResolverRegistry()
                .register(new RuntimeContextParameterResolver())
                .register(new FSMContextParameterResolver())
                .register(new SceneManagerParameterResolver())
                .register(new WizardManagerParameterResolver())
                .register(new RuntimeMessagingFacadeParameterResolver())
                .register(new UpdateParameterResolver())
                .register(new MessageParameterResolver())
                .register(new CallbackParameterResolver())
                .register(new UserParameterResolver())
                .register(new ChatParameterResolver())
                .register(new EventParameterResolver())
                .register(new FilterDataParameterResolver())
                .register(new MiddlewareDataParameterResolver())
                .register(new ApplicationDataParameterResolver())
        );
    }

    @Override
    public CompletionStage<Void> invoke(Object target, Method method, HandlerInvocationContext context) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(context, "context");

        HandlerMethodMetadata metadata = metadataCache.computeIfAbsent(method, this::introspect);
        try {
            Object[] args = resolveArguments(method, metadata, context);
            if (!method.canAccess(target)) {
                method.setAccessible(true);
            }
            Object result = method.invoke(target, args);
            return normalizeResult(method, result);
        } catch (InvocationTargetException e) {
            return CompletableFuture.failedFuture(unwrap(e.getTargetException()));
        } catch (ParameterResolutionException known) {
            return CompletableFuture.failedFuture(known);
        } catch (Throwable throwable) {
            return CompletableFuture.failedFuture(ReflectiveInvocationException.invocationFailure(method, unwrap(throwable)));
        }
    }

    private Object[] resolveArguments(
            Method method,
            HandlerMethodMetadata metadata,
            HandlerInvocationContext context
    ) {
        Object[] args = new Object[metadata.parameters().length];
        for (int i = 0; i < metadata.parameters().length; i++) {
            HandlerParameterDescriptor parameter = metadata.parameters()[i];
            Optional<Object> resolved;
            try {
                resolved = registry.resolve(parameter, context);
            } catch (ParameterResolutionException known) {
                throw known;
            } catch (ResolverExecutionException resolverFailure) {
                Throwable cause = resolverFailure.getCause() == null ? resolverFailure : resolverFailure.getCause();
                if (cause instanceof IllegalStateException ambiguous
                        && ambiguous.getMessage() != null
                        && ambiguous.getMessage().toLowerCase().contains("ambiguous")) {
                    throw ParameterResolutionException.ambiguous(
                            method,
                            parameter,
                            resolverFailure.resolverType().getSimpleName(),
                            ambiguous
                    );
                }
                throw ParameterResolutionException.resolverFailure(
                        method,
                        parameter,
                        resolverFailure.resolverType().getSimpleName(),
                        cause
                );
            } catch (Throwable throwable) {
                throw ParameterResolutionException.resolverFailure(
                        method,
                        parameter,
                        "unknown",
                        throwable
                );
            }
            if (resolved.isEmpty()) {
                if (isLikelyDependencyParameter(parameter, context)) {
                    throw new MissingHandlerDependencyException(method, parameter);
                }
                throw new UnsupportedHandlerParameterException(method, parameter);
            }
            args[i] = resolved.orElseThrow();
        }
        return args;
    }

    private static CompletionStage<Void> normalizeResult(Method method, Object result) {
        if (result == null) {
            return CompletableFuture.completedFuture(null);
        }
        if (result instanceof CompletionStage<?> stage) {
            return stage.thenApply(ignored -> null);
        }
        throw ReflectiveInvocationException.invalidReturnType(method, result);
    }

    private HandlerMethodMetadata introspect(Method method) {
        Parameter[] parameters = method.getParameters();
        HandlerParameterDescriptor[] descriptors = new HandlerParameterDescriptor[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            descriptors[i] = new HandlerParameterDescriptor(i, parameters[i]);
        }
        return new HandlerMethodMetadata(descriptors);
    }

    private static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof CompletionException completion && completion.getCause() != null) {
            return completion.getCause();
        }
        return throwable;
    }

    private static boolean isLikelyDependencyParameter(
            HandlerParameterDescriptor parameter,
            HandlerInvocationContext context
    ) {
        Class<?> type = parameter.type();
        if (type == RuntimeContext.class
                || type == Update.class
                || type == Message.class
                || type == Callback.class
                || type == User.class
                || type == Chat.class) {
            return false;
        }
        return !type.isInstance(context.event());
    }

    private record HandlerMethodMetadata(HandlerParameterDescriptor[] parameters) {
    }
}
