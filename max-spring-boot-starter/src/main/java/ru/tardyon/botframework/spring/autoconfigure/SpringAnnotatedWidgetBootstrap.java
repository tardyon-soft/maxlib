package ru.tardyon.botframework.spring.autoconfigure;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ClassUtils;
import ru.tardyon.botframework.spring.widget.AnnotatedWidgetRegistry;
import ru.tardyon.botframework.spring.widget.annotation.WidgetController;

/**
 * Auto-registers {@link WidgetController} beans into {@link AnnotatedWidgetRegistry}.
 */
final class SpringAnnotatedWidgetBootstrap implements SmartInitializingSingleton {
    private static final Logger log = LoggerFactory.getLogger(SpringAnnotatedWidgetBootstrap.class);
    private final AnnotatedWidgetRegistry widgetRegistry;
    private final ObjectProvider<Object> beanProvider;

    SpringAnnotatedWidgetBootstrap(AnnotatedWidgetRegistry widgetRegistry, ObjectProvider<Object> beanProvider) {
        this.widgetRegistry = widgetRegistry;
        this.beanProvider = beanProvider;
    }

    @Override
    public void afterSingletonsInstantiated() {
        log.debug("Starting widget controller auto-registration");
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        beanProvider.orderedStream().forEach(bean -> {
            if (!seen.add(bean)) {
                return;
            }
            Class<?> userClass = ClassUtils.getUserClass(bean);
            WidgetController controller = AnnotatedElementUtils.findMergedAnnotation(userClass, WidgetController.class);
            if (controller == null || !controller.autoRegister()) {
                return;
            }
            log.debug("Auto-registering widget controller bean: type={}", userClass.getName());
            widgetRegistry.register(bean);
        });
        log.debug("Widget controller auto-registration completed");
    }
}

