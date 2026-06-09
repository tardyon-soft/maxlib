package ru.tardyon.botframework.micronaut.autoconfigure;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import ru.tardyon.botframework.micronaut.widget.AnnotatedWidgetRegistry;
import ru.tardyon.botframework.micronaut.widget.annotation.WidgetController;

/**
 * Auto-registers {@code @WidgetController} beans into {@link AnnotatedWidgetRegistry}.
 */
@Singleton
@Context
public final class MicronautAnnotatedWidgetBootstrap {
    private final ApplicationContext applicationContext;
    private final AnnotatedWidgetRegistry widgetRegistry;
    private final MicronautWidgetBeanRegistrar widgetBeanRegistrar;

    public MicronautAnnotatedWidgetBootstrap(
            ApplicationContext applicationContext,
            AnnotatedWidgetRegistry widgetRegistry,
            MicronautWidgetBeanRegistrar widgetBeanRegistrar
    ) {
        this.applicationContext = applicationContext;
        this.widgetRegistry = widgetRegistry;
        this.widgetBeanRegistrar = widgetBeanRegistrar;
    }

    @PostConstruct
    void registerWidgets() {
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var reference : applicationContext.getBeanDefinitionReferences()) {
            if (!reference.isPresent()) {
                continue;
            }
            Class<?> beanType = reference.getBeanType();
            WidgetController controller = userClass(beanType).getAnnotation(WidgetController.class);
            if (controller == null || !controller.autoRegister()) {
                continue;
            }
            applicationContext.findBean(beanType).ifPresent(bean -> {
                if (seen.add(bean)) {
                    widgetRegistry.register(bean);
                }
            });
        }
        if (!isRouteComponentScanEnabled()) {
            return;
        }
        for (Object bean : widgetBeanRegistrar.discoverWidgetControllerInstances()) {
            if (!seen.add(bean)) {
                continue;
            }
            widgetRegistry.register(bean);
        }
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
