package ru.tardyon.botframework.quarkus.widget;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexReader;
import ru.tardyon.botframework.quarkus.widget.annotation.WidgetController;

/**
 * Discovers {@link WidgetController}-annotated classes using indexed metadata first.
 */
final class QuarkusWidgetDiscovery {
    private static final DotName WIDGET_CONTROLLER = DotName.createSimple(WidgetController.class.getName());
    private static final String INDEX_RESOURCE = "META-INF/jandex.idx";
    private static final String FALLBACK_PACKAGE_PREFIX = "ru/tardyon/botframework/";

    List<WidgetCandidate> discover() {
        LinkedHashMap<String, WidgetCandidate> candidates = new LinkedHashMap<>();
        try {
            discoverFromIndex(candidates);
            if (candidates.isEmpty()) {
                discoverFromClasspath(candidates);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to discover @WidgetController classes", e);
        }
        return candidates.values().stream()
                .sorted(Comparator.comparing(WidgetCandidate::className))
                .toList();
    }

    private void discoverFromIndex(Map<String, WidgetCandidate> candidates) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(INDEX_RESOURCE);
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            try (var inputStream = url.openStream()) {
                var index = new IndexReader(inputStream).read();
                for (AnnotationInstance annotation : index.getAnnotations(WIDGET_CONTROLLER)) {
                    if (annotation.target().kind() != AnnotationTarget.Kind.CLASS) {
                        continue;
                    }
                    ClassInfo classInfo = annotation.target().asClass();
                    registerCandidate(candidates, classInfo.name().toString(), annotation);
                }
            }
        }
    }

    private void discoverFromClasspath(Map<String, WidgetCandidate> candidates) throws IOException {
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

    private void scanDirectory(File directory, Map<String, WidgetCandidate> candidates) {
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
            if (prefixIndex < 0 || absolutePath.contains("$")) {
                continue;
            }
            String className = absolutePath.substring(prefixIndex, absolutePath.length() - 6).replace('/', '.');
            loadCandidate(className, candidates);
        }
    }

    private void scanJar(URL url, Map<String, WidgetCandidate> candidates) throws IOException {
        JarURLConnection connection = (JarURLConnection) url.openConnection();
        try (var jarFile = connection.getJarFile()) {
            var entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith(FALLBACK_PACKAGE_PREFIX) || !name.endsWith(".class") || name.contains("$")) {
                    continue;
                }
                String className = name.substring(0, name.length() - 6).replace('/', '.');
                loadCandidate(className, candidates);
            }
        }
    }

    private void loadCandidate(String className, Map<String, WidgetCandidate> candidates) {
        try {
            Class<?> type = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            WidgetController widgetController = type.getAnnotation(WidgetController.class);
            if (widgetController == null || type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
                return;
            }
            candidates.putIfAbsent(className, new WidgetCandidate(className, widgetController.autoRegister(), type));
        } catch (LinkageError | ClassNotFoundException ignored) {
            // Skip classes that cannot be resolved in the current runtime.
        }
    }

    private void registerCandidate(Map<String, WidgetCandidate> candidates, String className, AnnotationInstance annotation) {
        try {
            if (className.contains("$")) {
                return;
            }
            Class<?> type = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
                return;
            }
            boolean autoRegister = annotation.value("autoRegister") == null || annotation.value("autoRegister").asBoolean();
            candidates.putIfAbsent(className, new WidgetCandidate(className, autoRegister, type));
        } catch (LinkageError | ClassNotFoundException ignored) {
            // Skip classes that cannot be resolved in the current runtime.
        }
    }

    record WidgetCandidate(String className, boolean autoRegister, Class<?> type) {
    }
}
