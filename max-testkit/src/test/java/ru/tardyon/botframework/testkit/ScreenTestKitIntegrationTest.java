package ru.tardyon.botframework.testkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.dispatcher.BuiltInFilters;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.model.TextFormat;
import ru.tardyon.botframework.model.transport.ApiOutgoingMessageBody;
import ru.tardyon.botframework.screen.InMemoryScreenRegistry;
import ru.tardyon.botframework.screen.ScreenButton;
import ru.tardyon.botframework.screen.ScreenContext;
import ru.tardyon.botframework.screen.ScreenDefinition;
import ru.tardyon.botframework.screen.ScreenMiddleware;
import ru.tardyon.botframework.screen.ScreenModel;
import ru.tardyon.botframework.screen.Screens;
import ru.tardyon.botframework.screen.Widgets;

class ScreenTestKitIntegrationTest {

    @Test
    void screenFlowRunsWithoutSpringContext() {
        InMemoryScreenRegistry registry = new InMemoryScreenRegistry();
        registry.register(new HomeScreen()).register(new ProfileScreen());

        Router entryRouter = new Router("entry");
        entryRouter.message(BuiltInFilters.command("screen"), (message, context) ->
                Screens.navigator(context, registry).start("home", Map.of())
        );

        ScreenTestKit kit = ScreenTestKit.builder()
                .registry(registry)
                .includeRouter(entryRouter)
                .build();

        ScreenFlowProbe startProbe = kit.feed(TestUpdates.message("u-1", "c-1", "/screen"));
        startProbe.assertLastHandled()
                .assertLastHasCall("/messages")
                .assertTopScreen("home")
                .assertLastRenderedPayload(kit.actionPayload("open_profile"));
        ApiOutgoingMessageBody rendered = (ApiOutgoingMessageBody) startProbe.lastStep()
                .probe()
                .sideEffects()
                .get(0)
                .body()
                .orElseThrow();
        assertEquals(TextFormat.MARKDOWN, rendered.format());

        String openProfilePayload = ScreenFixtures.actionPayload("open_profile");
        ScreenFlowProbe callbackProbe = kit.feed(
                UpdateFixtures.callback()
                        .userId("u-1")
                        .chatId("c-1")
                        .sourceMessageId("m-test-1")
                        .data(openProfilePayload)
                        .build()
        );
        callbackProbe.assertLastHandled()
                .assertTopScreen("profile")
                .assertTopParam("name", "Guest");

        ScreenFlowProbe textProbe = kit.feed(TestUpdates.message("u-1", "c-1", "Alice"));
        textProbe.assertLastHandled()
                .assertTopScreen("profile")
                .assertTopParam("name", "Alice");
        assertFalse(textProbe.lastStep().probe().sideEffects().stream()
                .anyMatch(call -> call.method() == HttpMethod.DELETE && "/messages".equals(call.path())));

        assertEquals(1, textProbe.steps().size());
    }

    @Test
    void screenTextDoesNotAutoRerenderAfterHandlerNavigation() {
        InMemoryScreenRegistry registry = new InMemoryScreenRegistry();
        registry.register(new RerenderingTextScreen());

        Router entryRouter = new Router("entry-rerender");
        entryRouter.message(BuiltInFilters.command("wait"), (message, context) ->
                Screens.navigator(context, registry).start("wait", Map.of())
        );

        ScreenTestKit kit = ScreenTestKit.builder()
                .registry(registry)
                .includeRouter(entryRouter)
                .build();

        kit.feed(TestUpdates.message("u-1", "c-1", "/wait")).assertLastHandled().assertTopScreen("wait");

        ScreenFlowProbe textProbe = kit.feed(TestUpdates.message("u-1", "c-1", "hello"));

        textProbe.assertLastHandled().assertTopScreen("wait");
        long editCalls = textProbe.lastStep().probe().sideEffects().stream()
                .filter(call -> call.method() == HttpMethod.PUT && "/messages".equals(call.path()))
                .count();
        assertEquals(1, editCalls);
    }

    @Test
    void screenTextHandlerDoesNotRerenderImplicitly() {
        InMemoryScreenRegistry registry = new InMemoryScreenRegistry();
        registry.register(new HandledOnlyTextScreen());

        Router entryRouter = new Router("entry-handled-only");
        entryRouter.message(BuiltInFilters.command("handled"), (message, context) ->
                Screens.navigator(context, registry).start("handled", Map.of())
        );

        ScreenTestKit kit = ScreenTestKit.builder()
                .registry(registry)
                .includeRouter(entryRouter)
                .build();

        kit.feed(TestUpdates.message("u-1", "c-1", "/handled")).assertLastHandled().assertTopScreen("handled");

        ScreenFlowProbe textProbe = kit.feed(TestUpdates.message("u-1", "c-1", "hello"));

        textProbe.assertLastHandled().assertTopScreen("handled");
        long messageMutations = textProbe.lastStep().probe().sideEffects().stream()
                .filter(call -> "/messages".equals(call.path()))
                .count();
        assertEquals(0, messageMutations);
    }

    @Test
    void screenOuterMiddlewareHandlesActiveScreenBeforeGenericRoutes() {
        InMemoryScreenRegistry registry = new InMemoryScreenRegistry();
        registry.register(new RerenderingTextScreen());

        Router entryRouter = new Router("entry-outer");
        entryRouter.message(BuiltInFilters.command("wait"), (message, context) ->
                Screens.navigator(context, registry).start("wait", Map.of())
        );
        entryRouter.message(message -> CompletableFuture.completedFuture(
                message != null && message.text() != null
                        ? ru.tardyon.botframework.dispatcher.FilterResult.matched()
                        : ru.tardyon.botframework.dispatcher.FilterResult.notMatched()
        ), (message, context) -> {
            context.messaging().send(message.chat().id(), ru.tardyon.botframework.message.Messages.text("generic"));
            return CompletableFuture.completedFuture(null);
        });

        DispatcherTestKit dispatcher = DispatcherTestKit.builder()
                .fsmStorage(new ru.tardyon.botframework.fsm.MemoryStorage())
                .stateScope(ru.tardyon.botframework.fsm.StateScope.USER_IN_CHAT)
                .includeRouter(entryRouter)
                .build();
        dispatcher.runtime().outerMiddleware(new ScreenMiddleware(registry));

        dispatcher.feed(TestUpdates.message("u-1", "c-1", "/wait"));

        DispatcherTestKit.DispatchProbe textProbe = dispatcher.feedAndCapture(TestUpdates.message("u-1", "c-1", "hello"));

        long genericSends = textProbe.sideEffects().stream()
                .filter(call -> call.method() == HttpMethod.POST && "/messages".equals(call.path()))
                .count();
        long screenEdits = textProbe.sideEffects().stream()
                .filter(call -> call.method() == HttpMethod.PUT && "/messages".equals(call.path()))
                .count();
        assertEquals(0, genericSends);
        assertEquals(1, screenEdits);
    }


    private static final class HomeScreen implements ScreenDefinition {
        @Override
        public String id() {
            return "home";
        }

        @Override
        public java.util.concurrent.CompletionStage<ScreenModel> render(ScreenContext context) {
            return CompletableFuture.completedFuture(
                    ScreenModel.builder()
                            .title("Home")
                            .markdown()
                            .widget(Widgets.buttonRow(ScreenButton.of("Open profile", "open_profile")))
                            .showBackButton(false)
                            .build()
            );
        }

        @Override
        public java.util.concurrent.CompletionStage<Void> onAction(
                ScreenContext context,
                String action,
                Map<String, String> args
        ) {
            if ("open_profile".equals(action)) {
                return context.nav().push("profile", Map.of("name", "Guest"));
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
        public java.util.concurrent.CompletionStage<ScreenModel> render(ScreenContext context) {
            String name = String.valueOf(context.params().getOrDefault("name", "Guest"));
            return CompletableFuture.completedFuture(
                    ScreenModel.builder()
                            .title("Profile: " + name)
                            .widget(Widgets.text("Type text to change profile name"))
                            .showBackButton(true)
                            .build()
            );
        }

        @Override
        public java.util.concurrent.CompletionStage<Void> onText(ScreenContext context, String text) {
            String next = text == null || text.isBlank() ? "Guest" : text.trim();
            return context.nav().replace("profile", Map.of("name", next));
        }
    }

    private static final class RerenderingTextScreen implements ScreenDefinition {
        @Override
        public String id() {
            return "wait";
        }

        @Override
        public java.util.concurrent.CompletionStage<ScreenModel> render(ScreenContext context) {
            return CompletableFuture.completedFuture(
                    ScreenModel.builder()
                            .title("Wait")
                            .widget(Widgets.text("Type text"))
                            .showBackButton(false)
                            .build()
            );
        }

        @Override
        public java.util.concurrent.CompletionStage<Void> onText(ScreenContext context, String text) {
            return context.nav().rerender();
        }
    }

    private static final class HandledOnlyTextScreen implements ScreenDefinition {
        @Override
        public String id() {
            return "handled";
        }

        @Override
        public java.util.concurrent.CompletionStage<ScreenModel> render(ScreenContext context) {
            return CompletableFuture.completedFuture(
                    ScreenModel.builder()
                            .title("Handled")
                            .widget(Widgets.text("Type text"))
                            .showBackButton(false)
                            .build()
            );
        }

        @Override
        public java.util.concurrent.CompletionStage<Void> onText(ScreenContext context, String text) {
            return CompletableFuture.completedFuture(null);
        }
    }
}
