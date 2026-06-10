package ru.tardyon.botframework.quarkus.route;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexReader;
import ru.tardyon.botframework.dispatcher.annotation.Route;

/**
 * Discovers {@link Route}-annotated route classes using indexed Quarkus metadata first.
 */
final class QuarkusRouteDiscovery {
    private static final DotName ROUTE = DotName.createSimple(Route.class.getName());
    private static final DotName PRIORITY = DotName.createSimple(jakarta.annotation.Priority.class.getName());
    private static final String INDEX_RESOURCE = "META-INF/jandex.idx";
    private static final String FALLBACK_PACKAGE_PREFIX = "ru/tardyon/botframework/";

    List<RouteCandidate> discover() {
        LinkedHashMap<String, RouteCandidate> candidates = new LinkedHashMap<>();
        try {
            discoverFromIndex(candidates);
            if (candidates.isEmpty()) {
                discoverFromClasspath(candidates);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to discover @Route classes", e);
        }
        return candidates.values().stream()
                .sorted(Comparator.comparingInt(RouteCandidate::priority).thenComparing(RouteCandidate::className))
                .toList();
    }

    private void discoverFromIndex(Map<String, RouteCandidate> candidates) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(INDEX_RESOURCE);
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            try (InputStream inputStream = url.openStream()) {
                var index = new IndexReader(inputStream).read();
                for (AnnotationInstance annotation : index.getAnnotations(ROUTE)) {
                    if (annotation.target().kind() != AnnotationTarget.Kind.CLASS) {
                        continue;
                    }
                    ClassInfo classInfo = annotation.target().asClass();
                    registerCandidate(candidates, classInfo.name().toString(), annotation);
                }
            }
        }
    }

    private void discoverFromClasspath(Map<String, RouteCandidate> candidates) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources("");
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            String protocol = url.getProtocol();
            if ("file".equals(protocol)) {
                scanDirectory(new File(URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8)), candidates);
            } else if ("jar".equals(protocol)) {
                scanJar(url, candidates);
            }
        }
    }

    private void scanDirectory(File directory, Map<String, RouteCandidate> candidates) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, candidates);
                continue;
            }
            if (!file.getName().endsWith(".class")) {
                continue;
            }
            String absolutePath = file.getAbsolutePath().replace(File.separatorChar, '/');
            int prefixIndex = absolutePath.indexOf(FALLBACK_PACKAGE_PREFIX);
            if (prefixIndex < 0) {
                continue;
            }
            String className = absolutePath.substring(prefixIndex, absolutePath.length() - 6).replace('/', '.');
            loadCandidate(className, candidates);
        }
    }

    private void scanJar(URL url, Map<String, RouteCandidate> candidates) throws IOException {
        JarURLConnection connection = (JarURLConnection) url.openConnection();
        try (var jarFile = connection.getJarFile()) {
            var entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith(FALLBACK_PACKAGE_PREFIX) || !name.endsWith(".class")) {
                    continue;
                }
                String className = name.substring(0, name.length() - 6).replace('/', '.');
                loadCandidate(className, candidates);
            }
        }
    }

    private void loadCandidate(String className, Map<String, RouteCandidate> candidates) {
        try {
            Class<?> type = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            Route route = type.getAnnotation(Route.class);
            if (route == null || type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
                return;
            }
            candidates.putIfAbsent(className, new RouteCandidate(className, route.autoRegister(), priority(type), type));
        } catch (LinkageError | ClassNotFoundException ignored) {
            // Skip classes that cannot be resolved in the current runtime.
        }
    }

    private void registerCandidate(Map<String, RouteCandidate> candidates, String className, AnnotationInstance annotation) {
        try {
            Class<?> type = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
                return;
            }
            boolean autoRegister = annotation.value("autoRegister") != null && annotation.value("autoRegister").asBoolean();
            candidates.putIfAbsent(className, new RouteCandidate(className, autoRegister, priority(type), type));
        } catch (LinkageError | ClassNotFoundException ignored) {
            // Skip classes that cannot be resolved in the current runtime.
        }
    }

    private int priority(Class<?> type) {
        jakarta.annotation.Priority annotation = type.getAnnotation(jakarta.annotation.Priority.class);
        return annotation == null ? 0 : annotation.value();
    }

    record RouteCandidate(String className, boolean autoRegister, int priority, Class<?> type) {
    }
}
