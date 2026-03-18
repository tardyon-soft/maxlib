package ru.tardyon.botframework.spring.autoconfigure;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ClassUtils;
import ru.tardyon.botframework.dispatcher.AnnotatedRouteRegistrar;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.dispatcher.annotation.Route;

/**
 * Auto-registers {@code @Route(autoRegister = true)} beans into dispatcher graph.
 */
final class SpringAnnotatedRouteBootstrap implements SmartInitializingSingleton {
    private final Dispatcher dispatcher;
    private final AnnotatedRouteRegistrar registrar;
    private final ObjectProvider<Object> beanProvider;

    SpringAnnotatedRouteBootstrap(
            Dispatcher dispatcher,
            AnnotatedRouteRegistrar registrar,
            ObjectProvider<Object> beanProvider
    ) {
        this.dispatcher = dispatcher;
        this.registrar = registrar;
        this.beanProvider = beanProvider;
    }

    @Override
    public void afterSingletonsInstantiated() {
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        beanProvider.orderedStream().forEach(bean -> {
            if (!seen.add(bean)) {
                return;
            }
            Class<?> userClass = ClassUtils.getUserClass(bean);
            Route route = AnnotatedElementUtils.findMergedAnnotation(userClass, Route.class);
            if (route == null || !route.autoRegister()) {
                return;
            }
            dispatcher.includeRouter(registrar.register(bean));
        });
    }
}
