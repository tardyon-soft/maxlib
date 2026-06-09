package ru.tardyon.botframework.micronaut.autoconfigure;

import io.micronaut.context.ApplicationContext;
import jakarta.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import ru.tardyon.botframework.dispatcher.annotation.Route;

/**
 * Discovers {@link Route}-annotated classes even without explicit bean declaration.
 */
@Singleton
public final class MicronautRouteBeanRegistrar {
    private final ApplicationContext applicationContext;

    public MicronautRouteBeanRegistrar(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public List<Object> discoverRouteInstances() {
        LinkedHashSet<Object> instances = new LinkedHashSet<>();
        LinkedHashSet<String> basePackages = new LinkedHashSet<>();
        for (var reference : applicationContext.getBeanDefinitionReferences()) {
            if (!reference.isPresent()) {
                continue;
            }
            Class<?> beanType = reference.getBeanType();
            String packageName = beanType.getPackageName();
            if (!packageName.isBlank() && !packageName.startsWith("io.micronaut") && !packageName.startsWith("org.springframework")) {
                basePackages.add(packageName);
            }
        }

        for (String basePackage : basePackages) {
            for (Class<?> candidate : scanClasses(basePackage)) {
                Route route = candidate.getAnnotation(Route.class);
                if (route == null) {
                    continue;
                }
                instances.add(instantiate(candidate));
            }
        }
        return List.copyOf(instances);
    }

    private List<Class<?>> scanClasses(String basePackage) {
        String packagePath = basePackage.replace('.', '/');
        ArrayList<Class<?>> classes = new ArrayList<>();
        try {
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(packagePath);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    scanDirectory(basePackage, new File(URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8)), classes);
                } else if ("jar".equals(protocol)) {
                    scanJar(basePackage, url, classes);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to scan package " + basePackage, e);
        }
        return classes;
    }

    private void scanDirectory(String basePackage, File directory, List<Class<?>> classes) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(basePackage + "." + file.getName(), file, classes);
            } else if (file.getName().endsWith(".class") && !file.getName().contains("$")) {
                String className = basePackage + "." + file.getName().substring(0, file.getName().length() - 6);
                loadIfCandidate(className, classes);
            }
        }
    }

    private void scanJar(String basePackage, URL url, List<Class<?>> classes) throws IOException {
        JarURLConnection connection = (JarURLConnection) url.openConnection();
        try (JarFile jarFile = connection.getJarFile()) {
            String prefix = basePackage.replace('.', '/') + "/";
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith(prefix) || !name.endsWith(".class") || name.contains("$")) {
                    continue;
                }
                String className = name.substring(0, name.length() - 6).replace('/', '.');
                loadIfCandidate(className, classes);
            }
        }
    }

    private void loadIfCandidate(String className, List<Class<?>> classes) {
        try {
            Class<?> candidate = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            if (candidate.isAnnotationPresent(Route.class)
                    && !candidate.isInterface()
                    && !Modifier.isAbstract(candidate.getModifiers())) {
                classes.add(candidate);
            }
        } catch (LinkageError | ClassNotFoundException ignored) {
            // Skip classes that cannot be resolved in current classpath.
        }
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
}
