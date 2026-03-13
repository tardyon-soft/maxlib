package ru.max.botframework.testkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import ru.max.botframework.dispatcher.DispatchResult;
import ru.max.botframework.dispatcher.Dispatcher;
import ru.max.botframework.dispatcher.Router;
import ru.max.botframework.fsm.FSMStorage;
import ru.max.botframework.fsm.SceneRegistry;
import ru.max.botframework.fsm.SceneStorage;
import ru.max.botframework.fsm.StateScope;
import ru.max.botframework.ingestion.UpdateHandlingResult;
import ru.max.botframework.model.Update;
import ru.max.botframework.upload.UploadService;

/**
 * Test harness for feeding updates into real dispatcher runtime with minimal setup.
 *
 * <p>Harness reuses production dispatcher pipeline and provides optional recording bot client
 * to assert messaging/callback/action side effects.</p>
 */
public final class DispatcherTestKit {
    private final Dispatcher dispatcher;
    private final RecordingMaxBotClient botClient;

    private DispatcherTestKit(Dispatcher dispatcher, RecordingMaxBotClient botClient) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.botClient = Objects.requireNonNull(botClient, "botClient");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static DispatcherTestKit withRouter(Router router) {
        return builder().includeRouter(router).build();
    }

    /**
     * Backward-compatible factory for direct dispatcher creation.
     */
    public static Dispatcher dispatcher() {
        return new Dispatcher();
    }

    /**
     * Returns runtime dispatcher used by this harness.
     */
    public Dispatcher runtime() {
        return dispatcher;
    }

    /**
     * @deprecated use {@link #runtime()} for clearer naming.
     */
    @Deprecated(since = "0.1.0")
    public Dispatcher dispatcherRef() {
        return dispatcher;
    }

    public RecordingMaxBotClient botClient() {
        return botClient;
    }

    /**
     * Dispatches one update through runtime pipeline.
     */
    public DispatchResult feed(Update update) {
        Objects.requireNonNull(update, "update");
        return dispatcher.feedUpdate(update).toCompletableFuture().join();
    }

    /**
     * Dispatches one update and captures side effects emitted during this call.
     */
    public DispatchProbe feedAndCapture(Update update) {
        int before = botClient.calls().size();
        DispatchResult result = feed(update);
        return new DispatchProbe(result, botClient.calls().subList(before, botClient.calls().size()));
    }

    /**
     * Dispatches several updates in sequence.
     */
    public List<DispatchResult> feedAll(Update... updates) {
        Objects.requireNonNull(updates, "updates");
        ArrayList<DispatchResult> results = new ArrayList<>(updates.length);
        for (Update update : updates) {
            results.add(feed(update));
        }
        return List.copyOf(results);
    }

    /**
     * Dispatches several updates in sequence using iterable source.
     */
    public List<DispatchResult> feedAll(Iterable<Update> updates) {
        Objects.requireNonNull(updates, "updates");
        ArrayList<DispatchResult> results = new ArrayList<>();
        for (Update update : updates) {
            results.add(feed(update));
        }
        return List.copyOf(results);
    }

    /**
     * Uses ingestion-facing contract for testing sink integration.
     */
    public UpdateHandlingResult handle(Update update) {
        Objects.requireNonNull(update, "update");
        return dispatcher.handle(update).toCompletableFuture().join();
    }

    public record DispatchProbe(DispatchResult result, List<CapturedApiCall> sideEffects) {
        public DispatchProbe {
            Objects.requireNonNull(result, "result");
            sideEffects = List.copyOf(Objects.requireNonNull(sideEffects, "sideEffects"));
        }

        public boolean hasCall(String path) {
            Objects.requireNonNull(path, "path");
            return sideEffects.stream().anyMatch(call -> path.equals(call.path()));
        }

        public List<CapturedApiCall> callsTo(String path) {
            Objects.requireNonNull(path, "path");
            return sideEffects.stream().filter(call -> path.equals(call.path())).toList();
        }
    }

    public static final class Builder {
        private Dispatcher dispatcher;
        private final List<Router> routers = new ArrayList<>();
        private RecordingMaxBotClient botClient = new RecordingMaxBotClient();
        private FSMStorage fsmStorage;
        private StateScope stateScope;
        private SceneRegistry sceneRegistry;
        private SceneStorage sceneStorage;
        private UploadService uploadService;
        private boolean wireBotClient = true;

        private Builder() {
        }

        /**
         * Reuses existing dispatcher instance instead of creating a new one.
         */
        public Builder dispatcher(Dispatcher dispatcher) {
            this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
            return this;
        }

        /**
         * Includes one router into runtime under test.
         */
        public Builder includeRouter(Router router) {
            routers.add(Objects.requireNonNull(router, "router"));
            return this;
        }

        /**
         * Includes routers in iteration order.
         */
        public Builder includeRouters(List<Router> routers) {
            Objects.requireNonNull(routers, "routers");
            for (Router router : routers) {
                includeRouter(router);
            }
            return this;
        }

        /**
         * Sets recording MAX client used for side-effect assertions.
         */
        public Builder botClient(RecordingMaxBotClient botClient) {
            this.botClient = Objects.requireNonNull(botClient, "botClient");
            return this;
        }

        /**
         * Enables/disables automatic wiring of recording client into dispatcher.
         */
        public Builder wireBotClient(boolean wireBotClient) {
            this.wireBotClient = wireBotClient;
            return this;
        }

        /**
         * Configures FSM storage used by runtime.
         */
        public Builder fsmStorage(FSMStorage fsmStorage) {
            this.fsmStorage = Objects.requireNonNull(fsmStorage, "fsmStorage");
            return this;
        }

        /**
         * Configures FSM state scope strategy used by runtime.
         */
        public Builder stateScope(StateScope stateScope) {
            this.stateScope = Objects.requireNonNull(stateScope, "stateScope");
            return this;
        }

        /**
         * Configures scene registry used by runtime.
         */
        public Builder sceneRegistry(SceneRegistry sceneRegistry) {
            this.sceneRegistry = Objects.requireNonNull(sceneRegistry, "sceneRegistry");
            return this;
        }

        /**
         * Configures scene metadata storage used by runtime.
         */
        public Builder sceneStorage(SceneStorage sceneStorage) {
            this.sceneStorage = Objects.requireNonNull(sceneStorage, "sceneStorage");
            return this;
        }

        /**
         * Configures upload service used by runtime media APIs.
         */
        public Builder uploadService(UploadService uploadService) {
            this.uploadService = Objects.requireNonNull(uploadService, "uploadService");
            return this;
        }

        public DispatcherTestKit build() {
            Dispatcher runtime = dispatcher == null ? new Dispatcher() : dispatcher;

            if (wireBotClient) {
                runtime.withBotClient(botClient);
            }
            if (fsmStorage != null) {
                runtime.withFsmStorage(fsmStorage);
            }
            if (stateScope != null) {
                runtime.withStateScope(stateScope);
            }
            if (sceneRegistry != null) {
                runtime.withSceneRegistry(sceneRegistry);
            }
            if (sceneStorage != null) {
                runtime.withSceneStorage(sceneStorage);
            }
            if (uploadService != null) {
                runtime.withUploadService(uploadService);
            }
            for (Router router : routers) {
                runtime.includeRouter(router);
            }
            return new DispatcherTestKit(runtime, botClient);
        }
    }
}
