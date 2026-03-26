package ru.tardyon.botframework.demo.springpolling;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.screen.InMemoryScreenRegistry;
import ru.tardyon.botframework.screen.ScreenContext;
import ru.tardyon.botframework.screen.ScreenDefinition;
import ru.tardyon.botframework.screen.ScreenModel;
import ru.tardyon.botframework.screen.ScreenRegistry;
import ru.tardyon.botframework.screen.ScreenButton;
import ru.tardyon.botframework.screen.Widgets;

/**
 * Demo screen registry for /screen flow.
 */
public final class ScreenDemoSupport {
    private ScreenDemoSupport() {
    }

    public static ScreenRegistry buildRegistry() {
        return registerDefaults(new InMemoryScreenRegistry());
    }

    public static ScreenRegistry registerDefaults(ScreenRegistry registry) {
        return registry
                .register(new HomeScreen())
                .register(new ProfileScreen())
                .register(new SettingsScreen());
    }

    private static final class HomeScreen implements ScreenDefinition {
        @Override
        public String id() {
            return "home";
        }

        @Override
        public CompletionStage<ScreenModel> render(ScreenContext context) {
            return CompletableFuture.completedFuture(ScreenModel.builder()
                    .title("Экран: Главная")
                    .widget(Widgets.text("Это единый экран в чате с навигацией по стеку."))
                    .widget(Widgets.buttonRow(
                            ScreenButton.of("Профиль", "open_profile"),
                            ScreenButton.of("Настройки", "open_settings")
                    ))
                    .showBackButton(false)
                    .build());
        }

        @Override
        public CompletionStage<Void> onAction(ScreenContext context, String action, Map<String, String> args) {
            if ("open_profile".equals(action)) {
                return context.nav().push("profile", Map.of());
            }
            if ("open_settings".equals(action)) {
                return context.nav().push("settings", Map.of());
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class ProfileScreen implements ScreenDefinition {
        @Override
        public String id() {
            return "profile";
        }

        @Override
        public CompletionStage<ScreenModel> render(ScreenContext context) {
            String name = context.params().getOrDefault("name", "Гость").toString();
            return CompletableFuture.completedFuture(ScreenModel.builder()
                    .title("Экран: Профиль")
                    .widget(Widgets.text("Имя: " + name))
                    .widget(Widgets.text("Напишите любое сообщение, чтобы изменить имя профиля."))
                    .widget(Widgets.buttonRow(ScreenButton.of("Сбросить имя", "reset_name")))
                    .showBackButton(true)
                    .build());
        }

        @Override
        public CompletionStage<Void> onAction(ScreenContext context, String action, Map<String, String> args) {
            if ("reset_name".equals(action)) {
                return context.nav().replace("profile", Map.of("name", "Гость"));
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<Void> onText(ScreenContext context, String text) {
            String next = text == null || text.isBlank() ? "Гость" : text.trim();
            return context.nav().replace("profile", Map.of("name", next));
        }
    }

    private static final class SettingsScreen implements ScreenDefinition {
        @Override
        public String id() {
            return "settings";
        }

        @Override
        public CompletionStage<ScreenModel> render(ScreenContext context) {
            return CompletableFuture.completedFuture(ScreenModel.builder()
                    .title("Экран: Настройки")
                    .widget(Widgets.text("Пример composable-виджетов."))
                    .widget(Widgets.section(
                            "Действия:",
                            Widgets.buttonRow(ScreenButton.of("Открыть профиль", "open_profile"))
                    ))
                    .showBackButton(true)
                    .build());
        }

        @Override
        public CompletionStage<Void> onAction(ScreenContext context, String action, Map<String, String> args) {
            if ("open_profile".equals(action)) {
                return context.nav().push("profile", Map.of());
            }
            return CompletableFuture.completedFuture(null);
        }
    }
}
