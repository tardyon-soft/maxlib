package ru.tardyon.botframework.quarkus.screen;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import org.eclipse.microprofile.config.Config;
import ru.tardyon.botframework.quarkus.screen.annotation.ScreenController;
import ru.tardyon.botframework.screen.AnnotatedScreenRegistrar;
import ru.tardyon.botframework.screen.ScreenRegistry;

/**
 * Auto-registers {@code @Screen(autoRegister = true)} beans and indexed screen classes into the screen registry.
 */
@ApplicationScoped
public class QuarkusScreenAutoRegistrationBootstrap {
    private static final String ROUTE_COMPONENT_SCAN_ENABLED = "max.bot.route-component-scan.enabled";

    @Inject
    AnnotatedScreenRegistrar registrar;

    @Inject
    QuarkusScreenControllerRegistrar controllerRegistrar;

    @Inject
    Instance<Object> beanLookup;

    @Inject
    BeanManager beanManager;

    @Inject
    Config config;

    public void registerScreens(ScreenRegistry screenRegistry) {
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        QuarkusScreenDiscovery discovery = new QuarkusScreenDiscovery();
        for (QuarkusScreenDiscovery.ScreenCandidate candidate : discovery.discover()) {
            if (!candidate.autoRegister()) {
                continue;
            }
            Object screenBean = resolveBean(candidate.type());
            if (screenBean == null) {
                if (!isRouteComponentScanEnabled()) {
                    continue;
                }
                screenBean = instantiate(candidate.type());
            }
            if (!seen.add(screenBean)) {
                continue;
            }
            screenRegistry.register(registrar.register(screenBean));
        }

        for (Bean<?> bean : beanManager.getBeans(Object.class, Any.Literal.INSTANCE)) {
            Object controllerBean = registerScreenControllerBean(bean);
            if (controllerBean == null) {
                continue;
            }
            if (!seen.add(controllerBean)) {
                continue;
            }
            for (var definition : controllerRegistrar.register(controllerBean)) {
                screenRegistry.register(definition);
            }
        }
    }

    private Object resolveBean(Class<?> type) {
        Instance<?> selection = beanLookup.select(type);
        if (!selection.isResolvable()) {
            return null;
        }
        return selection.get();
    }

    private Object instantiate(Class<?> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            if (!constructor.canAccess(null)) {
                constructor.setAccessible(true);
            }
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to instantiate autodetected @Screen class: " + type.getName(), e);
        }
    }

    private Object registerScreenControllerBean(Bean<?> bean) {
        for (java.lang.reflect.Type beanType : bean.getTypes()) {
            if (!(beanType instanceof Class<?> candidateType)) {
                continue;
            }
            ScreenController screenController = userClass(candidateType).getAnnotation(ScreenController.class);
            if (screenController == null || !screenController.autoRegister()) {
                continue;
            }
            return beanManager.getReference(bean, candidateType, beanManager.createCreationalContext(bean));
        }
        return null;
    }

    private boolean isRouteComponentScanEnabled() {
        return config.getOptionalValue(ROUTE_COMPONENT_SCAN_ENABLED, String.class)
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
