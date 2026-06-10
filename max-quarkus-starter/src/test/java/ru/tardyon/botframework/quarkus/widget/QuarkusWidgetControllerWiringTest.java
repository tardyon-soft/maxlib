package ru.tardyon.botframework.quarkus.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.dispatcher.RuntimeDataKey;
import ru.tardyon.botframework.quarkus.runtime.MaxBotProducer;
import ru.tardyon.botframework.screen.ScreenButton;
import ru.tardyon.botframework.screen.WidgetActions;
import ru.tardyon.botframework.screen.WidgetActionDispatcher;
import ru.tardyon.botframework.screen.WidgetContext;
import ru.tardyon.botframework.screen.WidgetEffect;
import ru.tardyon.botframework.screen.WidgetView;
import ru.tardyon.botframework.screen.WidgetViewResolver;

@QuarkusComponentTest({
        MaxBotProducer.class,
        QuarkusWidgetAutoRegistrationBootstrap.class,
        SampleWidgetController.class,
        DisabledWidgetController.class
})
@TestConfigProperty(key = "max.bot.token", value = "test-token")
@TestConfigProperty(key = "max.bot.polling.enabled", value = "false")
@TestConfigProperty(key = "max.bot.route-component-scan.enabled", value = "true")
class QuarkusWidgetControllerWiringTest {
    @Inject
    Dispatcher dispatcher;

    @Inject
    WidgetViewResolver widgetViewResolver;

    @Inject
    WidgetActionDispatcher widgetActionDispatcher;

    @Test
    void widgetControllerFacadeIsRegisteredIntoRuntimeResolver() {
        WidgetContext context = WidgetTestSupport.widgetContext("widget.home");
        WidgetView view = widgetViewResolver.resolve(context, java.util.Map.of()).toCompletableFuture().join();

        assertEquals("widget home", view.textLines().get(0));
        assertEquals(WidgetActions.callbackAction("widget.home", "go"), view.buttons().get(0).get(0).action());
        assertEquals(ScreenButton.Kind.CALLBACK, view.buttons().get(0).get(0).kind());

        WidgetEffect effect = widgetActionDispatcher.dispatch(context, "go", java.util.Map.of("id", "42")).toCompletableFuture().join();
        assertEquals(WidgetEffect.RERENDER, effect);

        WidgetView autodetected = widgetViewResolver.resolve(WidgetTestSupport.widgetContext("autodetected.widget"), java.util.Map.of())
                .toCompletableFuture()
                .join();
        assertEquals("autodetected widget", autodetected.textLines().get(0));

        WidgetView missing = widgetViewResolver.resolve(WidgetTestSupport.widgetContext("missing.widget"), java.util.Map.of())
                .toCompletableFuture()
                .join();
        assertTrue(missing.textLines().get(0).contains("Widget not found: missing.widget"));
        assertTrue(missing.buttons().isEmpty());
        assertTrue(dispatcher.applicationData().containsKey(RuntimeDataKey.application(
                "service:" + WidgetViewResolver.class.getName(),
                WidgetViewResolver.class
        )));
        assertTrue(dispatcher.applicationData().containsKey(RuntimeDataKey.application(
                "service:" + ru.tardyon.botframework.screen.WidgetActionDispatcher.class.getName(),
                ru.tardyon.botframework.screen.WidgetActionDispatcher.class
        )));
    }

    @Test
    void disabledWidgetControllerIsNotAutoRegistered() {
        WidgetView view = widgetViewResolver.resolve(WidgetTestSupport.widgetContext("disabled.widget"), java.util.Map.of())
                .toCompletableFuture()
                .join();
        assertTrue(view.textLines().get(0).contains("Widget not found: disabled.widget"));
        assertTrue(view.buttons().isEmpty());
    }
}
