package ru.tardyon.botframework.spring.autoconfigure;

import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import ru.tardyon.botframework.screen.annotation.Screen;
import ru.tardyon.botframework.spring.screen.annotation.ScreenController;
import ru.tardyon.botframework.spring.widget.annotation.WidgetController;

/**
 * Registers {@link Screen} / {@link ScreenController} / {@link WidgetController}
 * annotated classes as Spring beans even without {@code @Component}.
 */
final class ScreenComponentAutoRegistrar implements BeanDefinitionRegistryPostProcessor, BeanFactoryAware, ResourceLoaderAware, EnvironmentAware {
    private static final Logger log = LoggerFactory.getLogger(ScreenComponentAutoRegistrar.class);
    private final BeanNameGenerator beanNameGenerator = AnnotationBeanNameGenerator.INSTANCE;

    private ConfigurableListableBeanFactory beanFactory;
    private ResourceLoader resourceLoader;
    private Environment environment;

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        if (beanFactory == null) {
            log.debug("Skipping @Screen component auto-registration: beanFactory unavailable");
            return;
        }

        Set<String> basePackages = resolveBasePackages(registry);
        if (basePackages.isEmpty()) {
            log.debug("Skipping @Screen component auto-registration: no base packages resolved");
            return;
        }
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false, environment);
        if (resourceLoader != null) {
            scanner.setResourceLoader(resourceLoader);
        }
        scanner.addIncludeFilter(new AnnotationTypeFilter(Screen.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(ScreenController.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(WidgetController.class));

        for (String basePackage : basePackages) {
            for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
                String className = candidate.getBeanClassName();
                if (className == null || hasBeanClass(registry, className)) {
                    continue;
                }
                String beanName = beanNameGenerator.generateBeanName(candidate, registry);
                registry.registerBeanDefinition(beanName, candidate);
                log.debug("Registered @Screen class as bean: {} -> {}", className, beanName);
            }
        }
    }

    private Set<String> resolveBasePackages(BeanDefinitionRegistry registry) {
        LinkedHashSet<String> packages = new LinkedHashSet<>();
        if (AutoConfigurationPackages.has(beanFactory)) {
            packages.addAll(AutoConfigurationPackages.get(beanFactory));
        }
        for (String beanName : registry.getBeanDefinitionNames()) {
            BeanDefinition definition = registry.getBeanDefinition(beanName);
            String className = definition.getBeanClassName();
            if (className == null || className.isBlank()) {
                continue;
            }
            String packageName = ClassUtils.getPackageName(className);
            if (!packageName.isBlank() && !packageName.startsWith("org.springframework")) {
                packages.add(packageName);
            }
        }
        return packages;
    }

    private static boolean hasBeanClass(BeanDefinitionRegistry registry, String className) {
        for (String beanName : registry.getBeanDefinitionNames()) {
            BeanDefinition definition = registry.getBeanDefinition(beanName);
            if (className.equals(definition.getBeanClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // no-op
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (beanFactory instanceof ConfigurableListableBeanFactory configurableListableBeanFactory) {
            this.beanFactory = configurableListableBeanFactory;
        } else if (beanFactory instanceof DefaultListableBeanFactory defaultListableBeanFactory) {
            this.beanFactory = defaultListableBeanFactory;
        }
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
