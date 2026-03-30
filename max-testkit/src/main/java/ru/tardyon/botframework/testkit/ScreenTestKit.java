package ru.tardyon.botframework.testkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.fsm.FSMContext;
import ru.tardyon.botframework.fsm.FSMStorage;
import ru.tardyon.botframework.fsm.MemoryStorage;
import ru.tardyon.botframework.fsm.StateKey;
import ru.tardyon.botframework.fsm.StateScope;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.Update;
import ru.tardyon.botframework.model.UserId;
import ru.tardyon.botframework.screen.FsmScreenStorage;
import ru.tardyon.botframework.screen.InMemoryScreenRegistry;
import ru.tardyon.botframework.screen.LegacyStringScreenActionCodec;
import ru.tardyon.botframework.screen.ScreenActionCodec;
import ru.tardyon.botframework.screen.ScreenDefinition;
import ru.tardyon.botframework.screen.ScreenRegistry;
import ru.tardyon.botframework.screen.ScreenRouter;
import ru.tardyon.botframework.screen.ScreenSession;

/**
 * Test harness focused on screen-flow scenarios.
 */
public final class ScreenTestKit {
    private static final String DEFAULT_NAMESPACE = "screen";

    private final DispatcherTestKit dispatcher;
    private final FSMStorage fsmStorage;
    private final StateScope stateScope;
    private final String namespace;
    private final ScreenActionCodec actionCodec;
    private final FsmScreenStorage screenStorage;

    private ScreenTestKit(
            DispatcherTestKit dispatcher,
            FSMStorage fsmStorage,
            StateScope stateScope,
            String namespace,
            ScreenActionCodec actionCodec
    ) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.fsmStorage = Objects.requireNonNull(fsmStorage, "fsmStorage");
        this.stateScope = Objects.requireNonNull(stateScope, "stateScope");
        this.namespace = normalizeNamespace(namespace);
        this.actionCodec = Objects.requireNonNull(actionCodec, "actionCodec");
        this.screenStorage = new FsmScreenStorage();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ScreenTestKit withRegistry(ScreenRegistry registry) {
        return builder().registry(registry).build();
    }

    public DispatcherTestKit dispatcher() {
        return dispatcher;
    }

    public String actionPayload(String action) {
        return actionPayload(action, Map.of());
    }

    public String actionPayload(String action, Map<String, String> args) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(args, "args");
        return actionCodec.encode(action, args);
    }

    public ScreenFlowProbe feed(Update update) {
        Objects.requireNonNull(update, "update");
        DispatcherTestKit.DispatchProbe probe = dispatcher.feedAndCapture(update);
        return new ScreenFlowProbe(this, List.of(new ScreenFlowProbe.Step(update, probe)));
    }

    public ScreenFlowProbe feedAll(Update... updates) {
        Objects.requireNonNull(updates, "updates");
        ArrayList<ScreenFlowProbe.Step> steps = new ArrayList<>(updates.length);
        for (Update update : updates) {
            Objects.requireNonNull(update, "update");
            steps.add(new ScreenFlowProbe.Step(update, dispatcher.feedAndCapture(update)));
        }
        return new ScreenFlowProbe(this, steps);
    }

    Optional<ScreenSession> screenSession(Update update) {
        Objects.requireNonNull(update, "update");
        return screenStorage.get(screenFsm(update)).toCompletableFuture().join();
    }

    private FSMContext screenFsm(Update update) {
        StateKey base = baseStateKey(update, stateScope);
        StateKey namespaced = switch (base.scope()) {
            case USER -> StateKey.user(new UserId(namespace + "::" + base.userId().value()));
            case CHAT -> StateKey.chat(new ChatId(namespace + "::" + base.chatId().value()));
            case USER_IN_CHAT -> StateKey.userInChat(
                    new UserId(namespace + "::" + base.userId().value()),
                    new ChatId(namespace + "::" + base.chatId().value())
            );
        };
        return FSMContext.of(fsmStorage, namespaced);
    }

    private static StateKey baseStateKey(Update update, StateScope stateScope) {
        UserId userId = resolveUserId(update);
        ChatId chatId = resolveChatId(update);
        return switch (stateScope) {
            case USER -> StateKey.user(userId);
            case CHAT -> StateKey.chat(chatId);
            case USER_IN_CHAT -> StateKey.userInChat(userId, chatId);
        };
    }

    private static UserId resolveUserId(Update update) {
        if (update.message() != null && update.message().from() != null && update.message().from().id() != null) {
            return update.message().from().id();
        }
        if (update.callback() != null && update.callback().from() != null && update.callback().from().id() != null) {
            return update.callback().from().id();
        }
        throw new IllegalArgumentException("Unable to resolve user id from update");
    }

    private static ChatId resolveChatId(Update update) {
        if (update.message() != null && update.message().chat() != null && update.message().chat().id() != null) {
            return update.message().chat().id();
        }
        if (update.callback() != null
                && update.callback().message() != null
                && update.callback().message().chat() != null
                && update.callback().message().chat().id() != null) {
            return update.callback().message().chat().id();
        }
        throw new IllegalArgumentException("Unable to resolve chat id from update");
    }

    private static String normalizeNamespace(String namespace) {
        Objects.requireNonNull(namespace, "namespace");
        String value = namespace.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("namespace must not be blank");
        }
        return value;
    }

    public static final class Builder {
        private ScreenRegistry registry = new InMemoryScreenRegistry();
        private final ArrayList<Router> routers = new ArrayList<>();
        private FSMStorage fsmStorage = new MemoryStorage();
        private StateScope stateScope = StateScope.USER_IN_CHAT;
        private String namespace = DEFAULT_NAMESPACE;
        private ScreenActionCodec actionCodec = new LegacyStringScreenActionCodec();

        private Builder() {
        }

        public Builder registry(ScreenRegistry screenRegistry) {
            this.registry = Objects.requireNonNull(screenRegistry, "screenRegistry");
            return this;
        }

        public Builder registerScreen(ScreenDefinition definition) {
            registry.register(Objects.requireNonNull(definition, "definition"));
            return this;
        }

        public Builder includeRouter(Router router) {
            routers.add(Objects.requireNonNull(router, "router"));
            return this;
        }

        public Builder fsmStorage(FSMStorage storage) {
            this.fsmStorage = Objects.requireNonNull(storage, "storage");
            return this;
        }

        public Builder stateScope(StateScope scope) {
            this.stateScope = Objects.requireNonNull(scope, "scope");
            return this;
        }

        public Builder namespace(String namespace) {
            this.namespace = normalizeNamespace(namespace);
            return this;
        }

        public Builder actionCodec(ScreenActionCodec codec) {
            this.actionCodec = Objects.requireNonNull(codec, "codec");
            return this;
        }

        public ScreenTestKit build() {
            Router screenRouter = new Router("testkit-screen-router");
            ScreenRouter.attach(screenRouter, registry, actionCodec);

            DispatcherTestKit.Builder dispatcherBuilder = DispatcherTestKit.builder()
                    .fsmStorage(fsmStorage)
                    .stateScope(stateScope)
                    .includeRouter(screenRouter);
            for (Router router : routers) {
                dispatcherBuilder.includeRouter(router);
            }

            return new ScreenTestKit(
                    dispatcherBuilder.build(),
                    fsmStorage,
                    stateScope,
                    namespace,
                    actionCodec
            );
        }
    }
}
