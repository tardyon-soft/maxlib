package ru.tardyon.botframework.quarkus.widget;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import org.eclipse.microprofile.config.Config;

/**
 * Auto-registers {@code @WidgetController} beans and indexed widget classes into the widget registry.
 */
@ApplicationScoped
public class QuarkusWidgetAutoRegistrationBootstrap {
    private static final String ROUTE_COMPONENT_SCAN_ENABLED = "max.bot.route-component-scan.enabled";

    public void registerWidgets(AnnotatedWidgetRegistry widgetRegistry, Instance<Object> beanLookup, Config config) {
        QuarkusWidgetDiscovery discovery = new QuarkusWidgetDiscovery();
        for (QuarkusWidgetDiscovery.WidgetCandidate candidate : discovery.discover()) {
            if (!candidate.autoRegister()) {
                continue;
            }
            Object widgetBean = resolveBean(beanLookup, candidate.type());
            if (widgetBean == null) {
                if (!isRouteComponentScanEnabled(config)) {
                    continue;
                }
                widgetBean = instantiate(candidate.type());
            }
            widgetRegistry.register(widgetBean);
        }
    }

    private Object resolveBean(Instance<Object> beanLookup, Class<?> type) {
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
            throw new IllegalStateException("Failed to instantiate autodetected @WidgetController class: " + type.getName(), e);
        }
    }

    private boolean isRouteComponentScanEnabled(Config config) {
        return config.getOptionalValue(ROUTE_COMPONENT_SCAN_ENABLED, String.class)
                .map(value -> !value.equalsIgnoreCase("false"))
                .orElse(true);
    }
}
