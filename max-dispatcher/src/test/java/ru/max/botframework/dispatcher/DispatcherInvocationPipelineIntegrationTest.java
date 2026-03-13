package ru.max.botframework.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import ru.max.botframework.fsm.FSMContext;
import ru.max.botframework.fsm.InMemorySceneRegistry;
import ru.max.botframework.fsm.MemorySceneStorage;
import ru.max.botframework.fsm.MemoryStorage;
import ru.max.botframework.fsm.SceneManager;
import ru.max.botframework.fsm.SceneSession;
import ru.max.botframework.fsm.SceneStateBinding;
import ru.max.botframework.fsm.StateScope;
import ru.max.botframework.fsm.Wizard;
import ru.max.botframework.fsm.WizardManager;
import ru.max.botframework.fsm.WizardStep;
import ru.max.botframework.model.Chat;
import ru.max.botframework.model.ChatId;
import ru.max.botframework.model.ChatType;
import ru.max.botframework.model.Message;
import ru.max.botframework.model.MessageId;
import ru.max.botframework.model.Update;
import ru.max.botframework.model.UpdateId;
import ru.max.botframework.model.UpdateType;
import ru.max.botframework.model.User;
import ru.max.botframework.model.UserId;

class DispatcherInvocationPipelineIntegrationTest {

    @Test
    void reflectiveInvocationInjectsFsmContext() throws Exception {
        MemoryStorage storage = new MemoryStorage();
        Dispatcher dispatcher = new Dispatcher()
                .withFsmStorage(storage)
                .withStateScope(StateScope.USER_IN_CHAT);
        Router router = new Router("fsm");
        FsmProbe probe = new FsmProbe();
        Method method = FsmProbe.class.getDeclaredMethod("onMessage", Message.class, FSMContext.class);

        router.message(BuiltInFilters.textStartsWith("pay:"), probe, method);
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate("pay:100")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals("checkout.email", probe.lastState);
        assertEquals("pay:100", probe.lastText);
        assertEquals("u-invoke-1", probe.lastScopeUserId);
        assertEquals("c-invoke-1", probe.lastScopeChatId);
    }

    @Test
    void reflectiveInvocationResolvesMultipleParameters() throws Exception {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("orders");
        PaymentService service = suffix -> "payment:" + suffix;
        MultiParamProbe probe = new MultiParamProbe();
        Method method = MultiParamProbe.class.getDeclaredMethod(
                "onMessage",
                Message.class,
                Update.class,
                User.class,
                Chat.class,
                RuntimeContext.class,
                String.class,
                Integer.class,
                PaymentService.class
        );

        dispatcher.registerService(PaymentService.class, service);
        dispatcher.outerMiddleware((ctx, next) -> {
            ctx.putEnrichment("attempt", 3);
            return next.proceed();
        });
        router.message(BuiltInFilters.textStartsWith("pay:"), probe, method);
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate("pay:42")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals("pay:42", probe.lastText);
        assertEquals("42", probe.lastSuffix);
        assertEquals(3, probe.lastAttempt);
        assertEquals("u-invoke-1", probe.lastUserId);
        assertEquals("c-invoke-1", probe.lastChatId);
        assertSame(service, probe.lastService);
        assertEquals("payment:42", probe.lastServiceResult);
    }

    @Test
    void reflectiveInvocationRunsInsideFullDispatchPipeline() throws Exception {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("main");
        List<String> order = new ArrayList<>();
        PipelineProbe probe = new PipelineProbe(order);
        Method method = PipelineProbe.class.getDeclaredMethod("handle", Message.class);

        dispatcher.outerMiddleware((ctx, next) -> {
            order.add("outer-pre");
            return next.proceed().thenApply(result -> {
                order.add("outer-post");
                return result;
            });
        });
        router.innerMiddleware((ctx, next) -> {
            order.add("inner-pre");
            return next.proceed().thenApply(result -> {
                order.add("inner-post");
                return result;
            });
        });
        router.message(BuiltInFilters.textStartsWith("pay:"), probe, method);
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate("pay:5")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals(List.of("outer-pre", "inner-pre", "handler", "inner-post", "outer-post"), order);
    }

    @Test
    void reflectiveInvocationUsesMixedResolutionSources() throws Exception {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("mix");
        OrderService service = id -> "order-" + id;
        MixedSourceProbe probe = new MixedSourceProbe();
        Method method = MixedSourceProbe.class.getDeclaredMethod(
                "onMessage",
                Message.class,
                String.class,
                Long.class,
                OrderService.class
        );

        dispatcher.registerApplicationData(RuntimeDataKey.application("service.order", OrderService.class), service);
        dispatcher.outerMiddleware((ctx, next) -> {
            ctx.putEnrichment("requestId", 9001L);
            return next.proceed();
        });
        router.message(BuiltInFilters.textStartsWith("pay:"), probe, method);
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate("pay:777")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals("777", probe.paymentSuffix);
        assertEquals(9001L, probe.requestId);
        assertSame(service, probe.service);
        assertEquals("order-777", probe.serviceResult);
        assertEquals(1, probe.calls.get());
    }

    @Test
    void fullPipelineRoutesResolutionFailureToErrorObserver() throws Exception {
        Dispatcher dispatcher = new Dispatcher();
        Router router = new Router("failures");
        Method method = MissingServiceProbe.class.getDeclaredMethod("onMessage", Message.class, PaymentService.class);
        List<String> order = new ArrayList<>();
        AtomicInteger errorCalls = new AtomicInteger();

        dispatcher.outerMiddleware((ctx, next) -> {
            order.add("outer-pre");
            return next.proceed().whenComplete((ignored, throwable) -> order.add("outer-post"));
        });
        router.innerMiddleware((ctx, next) -> {
            order.add("inner-pre");
            return next.proceed().whenComplete((ignored, throwable) -> order.add("inner-post"));
        });
        router.message(BuiltInFilters.textStartsWith("pay:"), new MissingServiceProbe(), method);
        router.error(error -> {
            errorCalls.incrementAndGet();
            assertEquals(RuntimeDispatchErrorType.PARAMETER_RESOLUTION_FAILURE, error.type());
            assertTrue(error.error() instanceof MissingHandlerDependencyException);
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate("pay:10")).toCompletableFuture().join();

        assertEquals(DispatchStatus.FAILED, result.status());
        assertTrue(result.errorOpt().orElseThrow() instanceof MissingHandlerDependencyException);
        assertEquals(1, errorCalls.get());
        assertEquals(List.of("outer-pre", "inner-pre", "inner-post", "outer-post"), order);
    }

    @Test
    void runtimeContextFsmShortcutWorksInDispatchPipeline() {
        MemoryStorage storage = new MemoryStorage();
        Dispatcher dispatcher = new Dispatcher()
                .withFsmStorage(storage)
                .withStateScope(StateScope.USER_IN_CHAT);
        Router router = new Router("fsm-runtime");
        AtomicInteger calls = new AtomicInteger();

        router.message(BuiltInFilters.textStartsWith("pay:"), (message, context) -> {
            calls.incrementAndGet();
            if (message.text().equals("pay:1")) {
                return context.fsm()
                        .setState("flow.started")
                        .thenCompose(ignored -> context.fsm().updateData(java.util.Map.of("last", message.text())))
                        .thenApply(ignored -> (Void) null);
            }
            return context.fsm().snapshot().thenAccept(snapshot -> {
                assertEquals("flow.started", snapshot.state().orElseThrow());
                assertEquals("pay:1", snapshot.data().get("last", String.class).orElseThrow());
            });
        });
        dispatcher.includeRouter(router);

        DispatchResult first = dispatcher.feedUpdate(messageUpdate("pay:1")).toCompletableFuture().join();
        DispatchResult second = dispatcher.feedUpdate(messageUpdate("pay:2")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, first.status());
        assertEquals(DispatchStatus.HANDLED, second.status());
        assertEquals(2, calls.get());
    }

    @Test
    void dispatchPipelineRoutesByStateFilter() {
        Dispatcher dispatcher = new Dispatcher()
                .withFsmStorage(new MemoryStorage())
                .withStateScope(StateScope.USER_IN_CHAT);
        Router router = new Router("fsm-filters");
        AtomicInteger setupCalls = new AtomicInteger();
        AtomicInteger gatedCalls = new AtomicInteger();

        router.message(BuiltInFilters.textEquals("start"), (message, context) -> context.fsm()
                .setState("checkout.email")
                .thenRun(setupCalls::incrementAndGet));
        router.message(BuiltInFilters.state("checkout.email"), message -> {
            gatedCalls.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult first = dispatcher.feedUpdate(messageUpdate("start")).toCompletableFuture().join();
        DispatchResult second = dispatcher.feedUpdate(messageUpdate("mail@example.com")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, first.status());
        assertEquals(DispatchStatus.HANDLED, second.status());
        assertEquals(1, setupCalls.get());
        assertEquals(1, gatedCalls.get());
    }

    @Test
    void handlerCanEnterSceneViaSceneManagerResolution() throws Exception {
        Dispatcher dispatcher = new Dispatcher()
                .withFsmStorage(new MemoryStorage())
                .withStateScope(StateScope.USER_IN_CHAT)
                .withSceneRegistry(new InMemorySceneRegistry().register(Wizard.named("checkout").step("email").build()))
                .withSceneStorage(new MemorySceneStorage())
                .withSceneStateBinding(SceneStateBinding.prefixed("scene:"));
        Router router = new Router("scenes-enter");
        SceneEnterProbe probe = new SceneEnterProbe();
        Method method = SceneEnterProbe.class.getDeclaredMethod("onMessage", Message.class, SceneManager.class);
        router.message(BuiltInFilters.textEquals("enter"), probe, method);
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate("enter")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals("checkout", probe.currentSceneId);
    }

    @Test
    void handlerCanAdvanceWizardStepViaWizardManager() {
        Dispatcher dispatcher = new Dispatcher()
                .withFsmStorage(new MemoryStorage())
                .withStateScope(StateScope.USER_IN_CHAT)
                .withSceneRegistry(new InMemorySceneRegistry()
                        .register(Wizard.named("checkout").step("email").step("confirm").build()))
                .withSceneStorage(new MemorySceneStorage());
        Router router = new Router("scenes-next");
        AtomicInteger calls = new AtomicInteger();

        router.message(BuiltInFilters.textEquals("wizard-start"), (message, context) ->
                context.wizard().enter("checkout"));
        router.message(BuiltInFilters.textEquals("wizard-next"), (message, context) -> {
            calls.incrementAndGet();
            return context.wizard().next()
                    .thenCompose(ignored -> context.wizard().currentStep())
                    .thenAccept(step -> assertEquals("confirm", step.orElseThrow().id()));
        });
        dispatcher.includeRouter(router);

        DispatchResult start = dispatcher.feedUpdate(messageUpdate("wizard-start")).toCompletableFuture().join();
        DispatchResult next = dispatcher.feedUpdate(messageUpdate("wizard-next")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, start.status());
        assertEquals(DispatchStatus.HANDLED, next.status());
        assertEquals(1, calls.get());
    }

    @Test
    void handlerCanExitSceneViaWizardManagerResolution() throws Exception {
        Dispatcher dispatcher = new Dispatcher()
                .withFsmStorage(new MemoryStorage())
                .withStateScope(StateScope.USER_IN_CHAT)
                .withSceneRegistry(new InMemorySceneRegistry().register(Wizard.named("checkout").step("email").build()))
                .withSceneStorage(new MemorySceneStorage());
        Router router = new Router("scenes-exit");
        SceneExitProbe probe = new SceneExitProbe();
        Method method = SceneExitProbe.class.getDeclaredMethod("onMessage", Message.class, WizardManager.class);
        router.message(BuiltInFilters.textEquals("exit"), probe, method);
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate("exit")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertTrue(probe.afterExit.isEmpty());
    }

    @Test
    void stateFilterPreservesFirstMatchSemantics() {
        MemoryStorage storage = new MemoryStorage();
        Dispatcher dispatcher = new Dispatcher()
                .withFsmStorage(storage)
                .withStateScope(StateScope.USER_IN_CHAT);
        Router router = new Router("fsm-first-match");
        AtomicInteger first = new AtomicInteger();
        AtomicInteger second = new AtomicInteger();
        Update update = messageUpdate("next");

        storage.setState(ru.max.botframework.fsm.StateKeyStrategies.userInChat().resolve(update), "checkout.email")
                .toCompletableFuture()
                .join();

        router.message(BuiltInFilters.state("checkout.email"), message -> {
            first.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        });
        router.message(BuiltInFilters.state("checkout.email"), message -> {
            second.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(update).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals(1, first.get());
        assertEquals(0, second.get());
    }

    @Test
    void stateFilterWorksWithOuterMiddleware() {
        Dispatcher dispatcher = new Dispatcher()
                .withFsmStorage(new MemoryStorage())
                .withStateScope(StateScope.USER_IN_CHAT);
        Router router = new Router("fsm-middleware");
        AtomicInteger calls = new AtomicInteger();

        dispatcher.outerMiddleware((context, next) -> context.fsm()
                .setState("middleware.state")
                .thenCompose(ignored -> next.proceed()));
        router.message(BuiltInFilters.state("middleware.state"), message -> {
            calls.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        });
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(messageUpdate("any")).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals(1, calls.get());
    }

    @Test
    void stateFilterComposesWithOtherFilters() {
        MemoryStorage storage = new MemoryStorage();
        Dispatcher dispatcher = new Dispatcher()
                .withFsmStorage(storage)
                .withStateScope(StateScope.USER_IN_CHAT);
        Router router = new Router("fsm-composition");
        AtomicInteger calls = new AtomicInteger();
        Update update = messageUpdate("email:user@example.com");

        storage.setState(ru.max.botframework.fsm.StateKeyStrategies.userInChat().resolve(update), "checkout.email")
                .toCompletableFuture()
                .join();

        router.message(
                BuiltInFilters.state("checkout.email").and(BuiltInFilters.textStartsWith("email:")),
                (message, context) -> {
                    calls.incrementAndGet();
                    assertEquals("user@example.com", context.enrichmentValue(BuiltInFilters.TEXT_SUFFIX_KEY, String.class).orElseThrow());
                    assertEquals("checkout.email", context.enrichmentValue(BuiltInFilters.STATE_KEY, String.class).orElseThrow());
                    return CompletableFuture.completedFuture(null);
                }
        );
        dispatcher.includeRouter(router);

        DispatchResult result = dispatcher.feedUpdate(update).toCompletableFuture().join();

        assertEquals(DispatchStatus.HANDLED, result.status());
        assertEquals(1, calls.get());
    }

    private interface PaymentService {
        String map(String suffix);
    }

    private interface OrderService {
        String make(String id);
    }

    private static final class MultiParamProbe {
        private String lastText;
        private String lastSuffix;
        private int lastAttempt;
        private String lastUserId;
        private String lastChatId;
        private PaymentService lastService;
        private String lastServiceResult;

        public CompletableFuture<Void> onMessage(
                Message message,
                Update update,
                User user,
                Chat chat,
                RuntimeContext context,
                String suffix,
                Integer attempt,
                PaymentService service
        ) {
            this.lastText = message.text();
            this.lastSuffix = suffix;
            this.lastAttempt = attempt;
            this.lastUserId = user.id().value();
            this.lastChatId = chat.id().value();
            this.lastService = service;
            this.lastServiceResult = service.map(suffix);
            assertSame(update, context.update());
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class PipelineProbe {
        private final List<String> order;

        private PipelineProbe(List<String> order) {
            this.order = order;
        }

        public CompletableFuture<Void> handle(Message message) {
            order.add("handler");
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class MixedSourceProbe {
        private String paymentSuffix;
        private long requestId;
        private OrderService service;
        private String serviceResult;
        private final AtomicInteger calls = new AtomicInteger();

        public CompletableFuture<Void> onMessage(Message message, String suffix, Long requestId, OrderService service) {
            this.paymentSuffix = suffix;
            this.requestId = requestId;
            this.service = service;
            this.serviceResult = service.make(suffix);
            calls.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class MissingServiceProbe {
        @SuppressWarnings("unused")
        public CompletableFuture<Void> onMessage(Message message, PaymentService service) {
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class FsmProbe {
        private String lastText;
        private String lastState;
        private String lastScopeUserId;
        private String lastScopeChatId;

        public CompletableFuture<Void> onMessage(Message message, FSMContext fsm) {
            this.lastText = message.text();
            this.lastScopeUserId = fsm.scope().userId().value();
            this.lastScopeChatId = fsm.scope().chatId().value();
            return fsm.setState("checkout.email")
                    .thenCompose(ignored -> fsm.updateData(java.util.Map.of("input", message.text())))
                    .thenCompose(updated -> fsm.currentState())
                    .thenAccept(state -> this.lastState = state.orElse("none"))
                    .toCompletableFuture();
        }
    }

    private static final class SceneEnterProbe {
        private String currentSceneId;

        public CompletableFuture<Void> onMessage(Message message, SceneManager scenes) {
            return scenes.enter("checkout")
                    .thenCompose(ignored -> scenes.currentScene())
                    .thenAccept(scene -> this.currentSceneId = scene.map(SceneSession::sceneId).orElse("none"))
                    .toCompletableFuture();
        }
    }

    private static final class SceneExitProbe {
        private java.util.Optional<WizardStep> afterExit = java.util.Optional.empty();

        public CompletableFuture<Void> onMessage(Message message, WizardManager wizard) {
            return wizard.enter("checkout")
                    .thenCompose(ignored -> wizard.exit())
                    .thenCompose(ignored -> wizard.currentStep())
                    .thenAccept(step -> this.afterExit = step)
                    .toCompletableFuture();
        }
    }

    private static Update messageUpdate(String text) {
        return new Update(
                new UpdateId("u-invoke-1"),
                UpdateType.MESSAGE,
                new Message(
                        new MessageId("m-invoke-1"),
                        new Chat(new ChatId("c-invoke-1"), ChatType.PRIVATE, "chat", null, null),
                        new User(new UserId("u-invoke-1"), "demo", "Demo", "User", "Demo User", false, "en"),
                        text,
                        Instant.parse("2026-03-12T00:00:00Z"),
                        null,
                        List.of(),
                        List.of()
                ),
                null,
                null,
                Instant.parse("2026-03-12T00:00:00Z")
        );
    }
}
