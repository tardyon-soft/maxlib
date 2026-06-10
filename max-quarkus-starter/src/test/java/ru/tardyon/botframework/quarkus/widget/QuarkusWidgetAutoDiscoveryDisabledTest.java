package ru.tardyon.botframework.quarkus.widget;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.quarkus.runtime.MaxBotProducer;
import ru.tardyon.botframework.screen.WidgetViewResolver;

@QuarkusComponentTest({
        MaxBotProducer.class,
        QuarkusWidgetAutoRegistrationBootstrap.class,
        SampleWidgetController.class
})
@TestConfigProperty(key = "max.bot.token", value = "test-token")
@TestConfigProperty(key = "max.bot.polling.enabled", value = "false")
@TestConfigProperty(key = "max.bot.route-component-scan.enabled", value = "false")
class QuarkusWidgetAutoDiscoveryDisabledTest {
    @Inject
    Dispatcher dispatcher;

    @Inject
    WidgetViewResolver widgetViewResolver;

    @Test
    void autodetectedWidgetIsSkippedWhenComponentScanIsDisabled() {
        var view = widgetViewResolver.resolve(WidgetTestSupport.widgetContext("autodetected.widget"), java.util.Map.of())
                .toCompletableFuture()
                .join();
        assertTrue(view.textLines().get(0).contains("Widget not found: autodetected.widget"));
        assertTrue(dispatcher != null);
    }
}
