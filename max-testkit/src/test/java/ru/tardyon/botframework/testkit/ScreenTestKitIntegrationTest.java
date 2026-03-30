package ru.tardyon.botframework.testkit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.dispatcher.BuiltInFilters;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.screen.InMemoryScreenRegistry;
import ru.tardyon.botframework.screen.ScreenButton;
import ru.tardyon.botframework.screen.ScreenContext;
import ru.tardyon.botframework.screen.ScreenDefinition;
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

        assertEquals(1, textProbe.steps().size());
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
}
