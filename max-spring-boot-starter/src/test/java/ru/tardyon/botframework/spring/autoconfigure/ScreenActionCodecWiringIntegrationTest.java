package ru.tardyon.botframework.spring.autoconfigure;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.dispatcher.RuntimeDataKey;
import ru.tardyon.botframework.screen.ScreenActionCodec;
import ru.tardyon.botframework.screen.TypedV1ScreenActionCodec;

@SpringBootTest(
        classes = ScreenActionCodecWiringIntegrationTest.TestApp.class,
        properties = {
                "max.bot.token=test-token",
                "max.bot.polling.enabled=false",
                "max.bot.route-component-scan.enabled=false",
                "max.bot.screen.callback.codec.mode=TYPED_V1"
        },
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class ScreenActionCodecWiringIntegrationTest {

    @Autowired
    private ScreenActionCodec screenActionCodec;

    @Autowired
    private Dispatcher dispatcher;

    @Test
    void typedCodecBeanIsWiredAndRegisteredInDispatcherServices() {
        assertTrue(screenActionCodec instanceof TypedV1ScreenActionCodec);
        assertTrue(dispatcher.applicationData().containsKey(
                RuntimeDataKey.application("service:" + ScreenActionCodec.class.getName(), ScreenActionCodec.class)
        ));
    }

    @Configuration
    @EnableAutoConfiguration
    static class TestApp {
    }
}

