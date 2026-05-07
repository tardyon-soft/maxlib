package ru.tardyon.botframework.demo.springpolling;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.screen.ScreenButton;
import ru.tardyon.botframework.screen.ScreenContext;
import ru.tardyon.botframework.screen.ScreenModel;
import ru.tardyon.botframework.screen.Widgets;
import ru.tardyon.botframework.spring.screen.annotation.OnScreenAction;
import ru.tardyon.botframework.spring.screen.annotation.OnScreenText;
import ru.tardyon.botframework.spring.screen.annotation.ScreenController;
import ru.tardyon.botframework.spring.screen.annotation.ScreenView;

/**
 * Controller-facade example for screen API in spring starter.
 */
@ScreenController
public final class FacadeScreenController {
    private final BackgroundTaskMonitorService taskMonitor;

    public FacadeScreenController(BackgroundTaskMonitorService taskMonitor) {
        this.taskMonitor = taskMonitor;
    }

    @ScreenView(screen = "facade.home")
    public ScreenModel home(ScreenContext context) {
        return ScreenModel.builder()
                .title("Facade Screen: Home")
                .widget(Widgets.text("Это пример @ScreenController + @ScreenView + @OnScreenAction + @OnScreenText"))
                .widget(Widgets.ref("demo.counter"))
                .widget(Widgets.buttonRow(
                        ScreenButton.of("Открыть профиль", "open_profile"),
                        ScreenButton.of("Мониторинг задачи", "open_monitor")
                ))
                .showBackButton(false)
                .build();
    }

    @OnScreenAction(screen = "facade.home", action = "open_profile")
    public CompletionStage<Void> openProfile(ScreenContext context) {
        return context.nav().push("facade.profile", Map.of("name", "Гость"));
    }

    @OnScreenAction(screen = "facade.home", action = "open_monitor")
    public CompletionStage<Void> openMonitor(ScreenContext context) {
        return context.nav().push("facade.monitor", Map.of());
    }

    @ScreenView(screen = "facade.profile")
    public ScreenModel profile(ScreenContext context) {
        String name = String.valueOf(context.params().getOrDefault("name", "Гость"));
        return ScreenModel.builder()
                .title("Facade Screen: Profile")
                .widget(Widgets.text("Имя: " + name))
                .widget(Widgets.text("Отправьте текст, чтобы изменить имя"))
                .widget(Widgets.buttonRow(ScreenButton.of("Сбросить имя", "reset_name")))
                .showBackButton(true)
                .build();
    }

    @OnScreenAction(screen = "facade.profile", action = "reset_name")
    public CompletionStage<Void> resetProfile(ScreenContext context) {
        return context.nav().replace("facade.profile", Map.of("name", "Гость"));
    }

    @OnScreenText(screen = "facade.profile")
    public CompletionStage<Void> profileText(ScreenContext context, String text) {
        String next = text == null || text.isBlank() ? "Гость" : text.trim();
        return context.nav().replace("facade.profile", Map.of("name", next));
    }

    @ScreenView(screen = "facade.monitor")
    public ScreenModel monitor(ScreenContext context) {
        BackgroundTaskMonitorService.TaskSnapshot snapshot = taskMonitor
                .snapshot(context.session().scopeId())
                .orElse(new BackgroundTaskMonitorService.TaskSnapshot(
                        BackgroundTaskMonitorService.TaskState.IDLE,
                        0,
                        0,
                        0
                ));

        ScreenModel.Builder builder = ScreenModel.builder()
                .title("Facade Screen: Background Task")
                .widget(Widgets.text("Статус: " + statusText(snapshot.state())))
                .widget(Widgets.text("Прогресс: " + progressBar(snapshot.progressPercent())
                        + " " + snapshot.progressPercent() + "%"))
                .widget(Widgets.text("Прошло: " + snapshot.elapsedSeconds() + " сек. Осталось: "
                        + snapshot.remainingSeconds() + " сек."))
                .showBackButton(true);

        if (snapshot.state() == BackgroundTaskMonitorService.TaskState.RUNNING) {
            builder.widget(Widgets.buttonRow(ScreenButton.of("Остановить", "stop_task")));
        } else {
            builder.widget(Widgets.buttonRow(ScreenButton.of("Запустить", "start_task")));
        }
        return builder.build();
    }

    @OnScreenAction(screen = "facade.monitor", action = "start_task")
    public CompletionStage<Void> startTask(ScreenContext context) {
        taskMonitor.start(context);
        return context.nav().rerender();
    }

    @OnScreenAction(screen = "facade.monitor", action = "stop_task")
    public CompletionStage<Void> stopTask(ScreenContext context) {
        taskMonitor.stop(context);
        return context.nav().rerender();
    }

    private static String statusText(BackgroundTaskMonitorService.TaskState state) {
        return switch (state) {
            case IDLE -> "ожидает запуска";
            case RUNNING -> "выполняется";
            case DONE -> "завершена";
            case STOPPED -> "остановлена";
        };
    }

    private static String progressBar(int progressPercent) {
        int filled = Math.max(0, Math.min(10, progressPercent / 10));
        return "[" + "#".repeat(filled) + ".".repeat(10 - filled) + "]";
    }
}
