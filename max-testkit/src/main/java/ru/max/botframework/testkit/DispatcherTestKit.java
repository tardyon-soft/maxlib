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

    /**
     * Backward-compatible factory for direct dispatcher creation.
     */
    public static Dispatcher dispatcher() {
        return new Dispatcher();
    }

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

        public Builder dispatcher(Dispatcher dispatcher) {
            this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
            return this;
        }

        public Builder includeRouter(Router router) {
            routers.add(Objects.requireNonNull(router, "router"));
            return this;
        }

        public Builder includeRouters(List<Router> routers) {
            Objects.requireNonNull(routers, "routers");
            for (Router router : routers) {
                includeRouter(router);
            }
            return this;
        }

        public Builder botClient(RecordingMaxBotClient botClient) {
            this.botClient = Objects.requireNonNull(botClient, "botClient");
            return this;
        }

        public Builder wireBotClient(boolean wireBotClient) {
            this.wireBotClient = wireBotClient;
            return this;
        }

        public Builder fsmStorage(FSMStorage fsmStorage) {
            this.fsmStorage = Objects.requireNonNull(fsmStorage, "fsmStorage");
            return this;
        }

        public Builder stateScope(StateScope stateScope) {
            this.stateScope = Objects.requireNonNull(stateScope, "stateScope");
            return this;
        }

        public Builder sceneRegistry(SceneRegistry sceneRegistry) {
            this.sceneRegistry = Objects.requireNonNull(sceneRegistry, "sceneRegistry");
            return this;
        }

        public Builder sceneStorage(SceneStorage sceneStorage) {
            this.sceneStorage = Objects.requireNonNull(sceneStorage, "sceneStorage");
            return this;
        }

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
