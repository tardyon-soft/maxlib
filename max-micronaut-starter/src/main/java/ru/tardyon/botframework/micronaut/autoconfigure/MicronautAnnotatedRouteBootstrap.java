package ru.tardyon.botframework.micronaut.autoconfigure;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import jakarta.inject.Singleton;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import ru.tardyon.botframework.dispatcher.AnnotatedRouteRegistrar;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.dispatcher.annotation.Route;

/**
 * Auto-registers {@code @Route(autoRegister = true)} beans into dispatcher graph.
 */
@Singleton
public final class MicronautAnnotatedRouteBootstrap implements BeanCreatedEventListener<Dispatcher> {
    private static final String ROUTE_COMPONENT_SCAN_ENABLED = "max.bot.route-component-scan.enabled";
    private final AnnotatedRouteRegistrar registrar;
    private final ApplicationContext applicationContext;

    public MicronautAnnotatedRouteBootstrap(
            AnnotatedRouteRegistrar registrar,
            ApplicationContext applicationContext
    ) {
        this.registrar = registrar;
        this.applicationContext = applicationContext;
    }

    @Override
    public Dispatcher onCreated(BeanCreatedEvent<Dispatcher> event) {
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        registerExplicitRouteBeans(event.getBean(), seen);
        if (!isRouteComponentScanEnabled()) {
            return event.getBean();
        }
        MicronautRouteBeanRegistrar registrarBean = new MicronautRouteBeanRegistrar(applicationContext);
        for (Object discovered : registrarBean.discoverRouteInstances()) {
            registerIfAnnotated(discovered, seen, event.getBean());
        }
        return event.getBean();
    }

    private void registerExplicitRouteBeans(Dispatcher dispatcher, Set<Object> seen) {
        for (var reference : applicationContext.getBeanDefinitionReferences()) {
            if (!reference.isPresent()) {
                continue;
            }
            Class<?> beanType = reference.getBeanType();
            Route route = userClass(beanType).getAnnotation(Route.class);
            if (route == null || !route.autoRegister()) {
                continue;
            }
            applicationContext.findBean(beanType).ifPresent(bean -> registerIfAnnotated(bean, seen, dispatcher));
        }
    }

    private void registerIfAnnotated(Object bean, Set<Object> seen, Dispatcher dispatcher) {
        if (!seen.add(bean)) {
            return;
        }
        Class<?> userClass = userClass(bean.getClass());
        Route route = userClass.getAnnotation(Route.class);
        if (route == null || !route.autoRegister()) {
            return;
        }
        dispatcher.includeRouter(registrar.register(bean));
    }

    private static Class<?> userClass(Class<?> type) {
        String name = type.getName();
        if (name.contains("$$")) {
            Class<?> superclass = type.getSuperclass();
            if (superclass != null && superclass != Object.class) {
                return superclass;
            }
        }
        return type;
    }

    private boolean isRouteComponentScanEnabled() {
        return applicationContext.getProperty(ROUTE_COMPONENT_SCAN_ENABLED, String.class)
                .map(value -> !value.equalsIgnoreCase("false"))
                .orElse(true);
    }
}
