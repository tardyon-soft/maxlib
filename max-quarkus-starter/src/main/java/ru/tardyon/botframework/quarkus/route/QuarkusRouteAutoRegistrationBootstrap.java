package ru.tardyon.botframework.quarkus.route;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import ru.tardyon.botframework.dispatcher.AnnotatedRouteRegistrar;
import ru.tardyon.botframework.dispatcher.Dispatcher;

/**
 * Auto-registers {@code @Route(autoRegister = true)} beans and indexed route classes into the dispatcher graph.
 */
@jakarta.inject.Singleton
public class QuarkusRouteAutoRegistrationBootstrap {
    private static final String ROUTE_COMPONENT_SCAN_ENABLED = "max.bot.route-component-scan.enabled";

    @Inject
    AnnotatedRouteRegistrar registrar;

    @Inject
    Instance<Object> beanLookup;

    @Inject
    Config config;

    public void registerRoutes(Dispatcher dispatcher) {
        QuarkusRouteDiscovery discovery = new QuarkusRouteDiscovery();
        for (QuarkusRouteDiscovery.RouteCandidate candidate : discovery.discover()) {
            if (!candidate.autoRegister()) {
                continue;
            }
            Object routeBean = resolveBean(candidate.type());
            if (routeBean == null) {
                if (!isRouteComponentScanEnabled()) {
                    continue;
                }
                routeBean = instantiate(candidate.type());
            }
            dispatcher.includeRouter(registrar.register(routeBean));
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
            throw new IllegalStateException("Failed to instantiate autodetected @Route class: " + type.getName(), e);
        }
    }

    private boolean isRouteComponentScanEnabled() {
        return config.getOptionalValue(ROUTE_COMPONENT_SCAN_ENABLED, String.class)
                .map(value -> !value.equalsIgnoreCase("false"))
                .orElse(true);
    }
}
