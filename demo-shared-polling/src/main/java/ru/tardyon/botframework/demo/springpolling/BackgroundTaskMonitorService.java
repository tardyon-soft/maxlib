package ru.tardyon.botframework.demo.springpolling;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tardyon.botframework.screen.ScreenContext;

/**
 * Demo background task store that refreshes its screen while the task is running.
 */
public final class BackgroundTaskMonitorService {
    private static final Logger log = LoggerFactory.getLogger(BackgroundTaskMonitorService.class);
    private static final Duration TASK_DURATION = Duration.ofSeconds(60);
    private static final Duration REFRESH_INTERVAL = Duration.ofSeconds(5);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "demo-background-task-monitor");
        thread.setDaemon(true);
        return thread;
    });
    private final ConcurrentMap<String, MonitoredTask> tasks = new ConcurrentHashMap<>();

    public Optional<TaskSnapshot> snapshot(String scopeId) {
        return Optional.ofNullable(tasks.get(scopeId)).map(MonitoredTask::snapshot);
    }

    public void start(ScreenContext context) {
        Objects.requireNonNull(context, "context");
        String scopeId = context.session().scopeId();
        MonitoredTask next = new MonitoredTask(scopeId, Instant.now(), TASK_DURATION);
        MonitoredTask previous = tasks.put(scopeId, next);
        if (previous != null) {
            previous.cancel();
        }

        ScheduledFuture<?> progressFuture = scheduler.scheduleAtFixedRate(
                () -> tick(next),
                0,
                1,
                TimeUnit.SECONDS
        );
        ScheduledFuture<?> refreshFuture = scheduler.scheduleAtFixedRate(
                () -> refresh(context, next),
                REFRESH_INTERVAL.toSeconds(),
                REFRESH_INTERVAL.toSeconds(),
                TimeUnit.SECONDS
        );
        next.attach(progressFuture, refreshFuture);
    }

    public void stop(ScreenContext context) {
        Objects.requireNonNull(context, "context");
        MonitoredTask task = tasks.get(context.session().scopeId());
        if (task != null) {
            task.stop();
        }
    }

    @PreDestroy
    public void shutdown() {
        tasks.values().forEach(MonitoredTask::cancel);
        scheduler.shutdownNow();
    }

    private void tick(MonitoredTask task) {
        if (task.finished()) {
            return;
        }
        TaskSnapshot snapshot = task.snapshot();
        if (snapshot.progressPercent() >= 100) {
            task.complete();
        }
    }

    private void refresh(ScreenContext context, MonitoredTask task) {
        if (!tasks.containsKey(task.scopeId())) {
            task.cancel();
            return;
        }
        context.nav().rerender().whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                log.debug("Background monitor screen refresh failed: scopeId={}", task.scopeId(), throwable);
            }
            if (task.finished()) {
                task.cancel();
            }
        });
    }

    public record TaskSnapshot(
            TaskState state,
            int progressPercent,
            long elapsedSeconds,
            long remainingSeconds
    ) {
    }

    public enum TaskState {
        IDLE,
        RUNNING,
        DONE,
        STOPPED
    }

    private static final class MonitoredTask {
        private final String scopeId;
        private final Instant startedAt;
        private final Duration duration;
        private volatile TaskState state = TaskState.RUNNING;
        private volatile ScheduledFuture<?> progressFuture;
        private volatile ScheduledFuture<?> refreshFuture;

        private MonitoredTask(String scopeId, Instant startedAt, Duration duration) {
            this.scopeId = scopeId;
            this.startedAt = startedAt;
            this.duration = duration;
        }

        private String scopeId() {
            return scopeId;
        }

        private void attach(ScheduledFuture<?> progressFuture, ScheduledFuture<?> refreshFuture) {
            this.progressFuture = progressFuture;
            this.refreshFuture = refreshFuture;
        }

        private TaskSnapshot snapshot() {
            long elapsed = Math.max(0, Duration.between(startedAt, Instant.now()).toSeconds());
            long total = duration.toSeconds();
            int progress = state == TaskState.DONE ? 100 : (int) Math.min(100, elapsed * 100 / total);
            long remaining = state == TaskState.RUNNING ? Math.max(0, total - elapsed) : 0;
            return new TaskSnapshot(state, progress, elapsed, remaining);
        }

        private boolean finished() {
            return state == TaskState.DONE || state == TaskState.STOPPED;
        }

        private void complete() {
            state = TaskState.DONE;
            cancelProgress();
        }

        private void stop() {
            state = TaskState.STOPPED;
            cancelProgress();
        }

        private void cancel() {
            cancelProgress();
            ScheduledFuture<?> refresh = refreshFuture;
            if (refresh != null) {
                refresh.cancel(false);
            }
        }

        private void cancelProgress() {
            ScheduledFuture<?> progress = progressFuture;
            if (progress != null) {
                progress.cancel(false);
            }
        }
    }
}
