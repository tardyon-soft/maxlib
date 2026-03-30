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
import ru.tardyon.botframework.screen.AnnotatedScreenRegistrar;
import ru.tardyon.botframework.screen.ScreenRegistry;
import ru.tardyon.botframework.screen.annotation.Screen;
import ru.tardyon.botframework.spring.screen.annotation.ScreenController;

/**
 * Auto-registers {@code @Screen(autoRegister = true)} beans into {@link ScreenRegistry}.
 */
final class SpringAnnotatedScreenBootstrap implements SmartInitializingSingleton {
    private static final Logger log = LoggerFactory.getLogger(SpringAnnotatedScreenBootstrap.class);
    private final ScreenRegistry screenRegistry;
    private final AnnotatedScreenRegistrar registrar;
    private final SpringScreenControllerRegistrar controllerRegistrar;
    private final ObjectProvider<Object> beanProvider;

    SpringAnnotatedScreenBootstrap(
            ScreenRegistry screenRegistry,
            AnnotatedScreenRegistrar registrar,
            SpringScreenControllerRegistrar controllerRegistrar,
            ObjectProvider<Object> beanProvider
    ) {
        this.screenRegistry = screenRegistry;
        this.registrar = registrar;
        this.controllerRegistrar = controllerRegistrar;
        this.beanProvider = beanProvider;
    }

    @Override
    public void afterSingletonsInstantiated() {
        log.debug("Starting annotated screen auto-registration");
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        beanProvider.orderedStream().forEach(bean -> {
            if (!seen.add(bean)) {
                return;
            }
            Class<?> userClass = ClassUtils.getUserClass(bean);
            Screen screen = AnnotatedElementUtils.findMergedAnnotation(userClass, Screen.class);
            if (screen == null || !screen.autoRegister()) {
                ScreenController screenController = AnnotatedElementUtils.findMergedAnnotation(userClass, ScreenController.class);
                if (screenController == null || !screenController.autoRegister()) {
                    return;
                }
                log.debug("Auto-registering screen controller bean: type={}", userClass.getName());
                controllerRegistrar.register(bean).forEach(screenRegistry::register);
                return;
            }
            log.debug("Auto-registering annotated screen bean: type={}, screen={}", userClass.getName(), screen.value());
            screenRegistry.register(registrar.register(bean));
        });
        log.debug("Annotated screen auto-registration completed");
    }
}
