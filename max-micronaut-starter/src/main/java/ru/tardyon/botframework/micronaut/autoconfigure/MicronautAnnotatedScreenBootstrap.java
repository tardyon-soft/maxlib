package ru.tardyon.botframework.micronaut.autoconfigure;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.BeanProvider;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import ru.tardyon.botframework.screen.AnnotatedScreenRegistrar;
import ru.tardyon.botframework.screen.ScreenRegistry;

/**
 * Auto-registers {@code @Screen(autoRegister = true)} beans into {@link ScreenRegistry}.
 */
@Singleton
@Context
public final class MicronautAnnotatedScreenBootstrap implements BeanCreatedEventListener<ScreenRegistry> {
    private final ApplicationContext applicationContext;
    private final AnnotatedScreenRegistrar registrar;
    private final BeanProvider<MicronautScreenBeanRegistrar> screenBeanRegistrarProvider;
    private final BeanProvider<MicronautScreenControllerRegistrar> screenControllerRegistrarProvider;
    private final BeanProvider<MicronautScreenControllerBeanRegistrar> screenControllerBeanRegistrarProvider;

    public MicronautAnnotatedScreenBootstrap(
            ApplicationContext applicationContext,
            AnnotatedScreenRegistrar registrar,
            BeanProvider<MicronautScreenBeanRegistrar> screenBeanRegistrarProvider,
            BeanProvider<MicronautScreenControllerRegistrar> screenControllerRegistrarProvider,
            BeanProvider<MicronautScreenControllerBeanRegistrar> screenControllerBeanRegistrarProvider
    ) {
        this.applicationContext = applicationContext;
        this.registrar = registrar;
        this.screenBeanRegistrarProvider = screenBeanRegistrarProvider;
        this.screenControllerRegistrarProvider = screenControllerRegistrarProvider;
        this.screenControllerBeanRegistrarProvider = screenControllerBeanRegistrarProvider;
    }

    @Override
    public ScreenRegistry onCreated(BeanCreatedEvent<ScreenRegistry> event) {
        ScreenRegistry screenRegistry = event.getBean();
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var reference : applicationContext.getBeanDefinitionReferences()) {
            if (!reference.isPresent()) {
                continue;
            }
            Class<?> beanType = reference.getBeanType();
            ru.tardyon.botframework.screen.annotation.Screen screen =
                    userClass(beanType).getAnnotation(ru.tardyon.botframework.screen.annotation.Screen.class);
            if (screen != null && screen.autoRegister()) {
                applicationContext.findBean(beanType).ifPresent(bean -> {
                    if (seen.add(bean)) {
                        screenRegistry.register(registrar.register(bean));
                    }
                });
                continue;
            }
            ru.tardyon.botframework.micronaut.screen.annotation.ScreenController controller =
                    userClass(beanType).getAnnotation(ru.tardyon.botframework.micronaut.screen.annotation.ScreenController.class);
            if (controller != null && controller.autoRegister()) {
                applicationContext.findBean(beanType).ifPresent(bean -> {
                    if (seen.add(bean)) {
                        screenControllerRegistrarProvider.ifPresent(registrar ->
                                registrar.register(bean).forEach(screenRegistry::register));
                    }
                });
            }
        }
        if (isRouteComponentScanEnabled() && screenBeanRegistrarProvider.isPresent()) {
            for (Object bean : screenBeanRegistrarProvider.get().discoverScreenBeans()) {
                if (!seen.add(bean)) {
                    continue;
                }
                screenRegistry.register(registrar.register(bean));
            }
        }
        if (isRouteComponentScanEnabled() && screenControllerRegistrarProvider.isPresent() && screenControllerBeanRegistrarProvider.isPresent()) {
            MicronautScreenControllerRegistrar screenControllerRegistrar = screenControllerRegistrarProvider.get();
            for (Object bean : screenControllerBeanRegistrarProvider.get().discoverScreenControllerInstances()) {
                if (!seen.add(bean)) {
                    continue;
                }
                screenControllerRegistrar.register(bean).forEach(screenRegistry::register);
            }
        }
        return event.getBean();
    }

    private boolean isRouteComponentScanEnabled() {
        return applicationContext.getProperty("max.bot.route-component-scan.enabled", String.class)
                .map(value -> !value.equalsIgnoreCase("false"))
                .orElse(true);
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
}
