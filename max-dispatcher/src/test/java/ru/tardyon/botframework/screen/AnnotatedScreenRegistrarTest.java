package ru.tardyon.botframework.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.screen.annotation.OnAction;
import ru.tardyon.botframework.screen.annotation.OnText;
import ru.tardyon.botframework.screen.annotation.Render;
import ru.tardyon.botframework.screen.annotation.Screen;

class AnnotatedScreenRegistrarTest {

    @Test
    void registersAnnotatedScreenAndInvokesRenderActionText() {
        AnnotatedScreenRegistrar registrar = new AnnotatedScreenRegistrar();
        SampleScreen bean = new SampleScreen();
        ScreenDefinition definition = registrar.register(bean);

        ScreenContext context = new TestScreenContext(Map.of("name", "Neo"));
        ScreenModel model = definition.render(context).toCompletableFuture().join();
        definition.onAction(context, "save", Map.of("mode", "fast")).toCompletableFuture().join();
        definition.onText(context, "  Alice  ").toCompletableFuture().join();

        assertEquals("profile", definition.id());
        assertEquals("Profile Neo", model.title());
        assertEquals("fast", bean.lastMode);
        assertEquals("Alice", bean.lastText);
    }

    @Test
    void widgetsSupportMediaAttachments() {
        var attachment = ru.tardyon.botframework.model.request.NewMessageAttachment.media(
                ru.tardyon.botframework.model.MessageAttachmentType.PHOTO,
                new ru.tardyon.botframework.model.request.AttachmentInput(null, "upload-ref", null),
                null,
                null,
                null
        );
        Widget widget = Widgets.attachment(attachment);
        WidgetRender render = widget.render(new TestScreenContext(Map.of())).toCompletableFuture().join();

        assertEquals(1, render.attachments().size());
        assertTrue(render.textLines().isEmpty());
    }

    @Screen("profile")
    static final class SampleScreen {
        private String lastMode;
        private String lastText;

        @Render
        public ScreenModel render(ScreenContext context) {
            String name = String.valueOf(context.params().getOrDefault("name", "Guest"));
            return ScreenModel.builder().title("Profile " + name).build();
        }

        @OnAction("save")
        public CompletionStage<Void> onSave(ScreenContext context, Map<String, String> args) {
            this.lastMode = args.get("mode");
            return CompletableFuture.completedFuture(null);
        }

        @OnText
        public void onText(ScreenContext context, String text) {
            this.lastText = text == null ? null : text.trim();
        }
    }

    static final class TestScreenContext implements ScreenContext {
        private final Map<String, Object> params;

        private TestScreenContext(Map<String, Object> params) {
            this.params = params;
        }

        @Override
        public ru.tardyon.botframework.dispatcher.RuntimeContext runtime() {
            return null;
        }

        @Override
        public ru.tardyon.botframework.fsm.FSMContext fsm() {
            return null;
        }

        @Override
        public ScreenSession session() {
            return new ScreenSession("scope", List.of(), null, Instant.parse("2026-03-26T00:00:00Z"));
        }

        @Override
        public Map<String, Object> params() {
            return params;
        }

        @Override
        public ScreenNavigator nav() {
            return new ScreenNavigator() {
                @Override
                public java.util.concurrent.CompletionStage<Void> start(String screenId, Map<String, Object> params) {
                    return CompletableFuture.completedFuture(null);
                }

                @Override
                public java.util.concurrent.CompletionStage<Void> push(String screenId, Map<String, Object> params) {
                    return CompletableFuture.completedFuture(null);
                }

                @Override
                public java.util.concurrent.CompletionStage<Void> replace(String screenId, Map<String, Object> params) {
                    return CompletableFuture.completedFuture(null);
                }

                @Override
                public java.util.concurrent.CompletionStage<Boolean> back() {
                    return CompletableFuture.completedFuture(false);
                }

                @Override
                public java.util.concurrent.CompletionStage<Void> rerender() {
                    return CompletableFuture.completedFuture(null);
                }

                @Override
                public java.util.concurrent.CompletionStage<Void> clear() {
                    return CompletableFuture.completedFuture(null);
                }

                @Override
                public java.util.concurrent.CompletionStage<ScreenSession> session() {
                    return CompletableFuture.completedFuture(
                            new ScreenSession("scope", List.of(), null, Instant.parse("2026-03-26T00:00:00Z"))
                    );
                }
            };
        }
    }
}
