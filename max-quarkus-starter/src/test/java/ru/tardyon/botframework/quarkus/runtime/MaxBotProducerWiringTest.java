package ru.tardyon.botframework.quarkus.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import jakarta.inject.Inject;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.action.ChatActionsFacade;
import ru.tardyon.botframework.callback.CallbackFacade;
import ru.tardyon.botframework.client.DefaultMaxBotClient;
import ru.tardyon.botframework.client.MaxApiClientConfig;
import ru.tardyon.botframework.client.MaxBotClient;
import ru.tardyon.botframework.client.http.MaxHttpClient;
import ru.tardyon.botframework.client.http.okhttp.OkHttpMaxHttpClient;
import ru.tardyon.botframework.client.serialization.JacksonJsonCodec;
import ru.tardyon.botframework.client.serialization.JsonCodec;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.dispatcher.RuntimeDataKey;
import ru.tardyon.botframework.fsm.FSMStorage;
import ru.tardyon.botframework.fsm.InMemorySceneRegistry;
import ru.tardyon.botframework.fsm.MemorySceneStorage;
import ru.tardyon.botframework.fsm.MemoryStorage;
import ru.tardyon.botframework.fsm.SceneRegistry;
import ru.tardyon.botframework.fsm.SceneStorage;
import ru.tardyon.botframework.message.MessagingFacade;
import ru.tardyon.botframework.quarkus.properties.MaxBotStorageType;
import ru.tardyon.botframework.screen.InMemoryScreenRegistry;
import ru.tardyon.botframework.screen.LegacyStringScreenActionCodec;
import ru.tardyon.botframework.screen.ScreenActionCodec;
import ru.tardyon.botframework.screen.ScreenMiddleware;
import ru.tardyon.botframework.screen.ScreenRegistry;
import ru.tardyon.botframework.screen.Screens;

@QuarkusComponentTest(MaxBotProducer.class)
@TestConfigProperty(key = "max.bot.token", value = "test-token")
class MaxBotProducerWiringTest {
    @Inject
    MaxApiClientConfig config;

    @Inject
    okhttp3.OkHttpClient okHttpClient;

    @Inject
    MaxHttpClient maxHttpClient;

    @Inject
    JsonCodec jsonCodec;

    @Inject
    MaxBotClient maxBotClient;

    @Inject
    FSMStorage fsmStorage;

    @Inject
    SceneRegistry sceneRegistry;

    @Inject
    SceneStorage sceneStorage;

    @Inject
    ScreenRegistry screenRegistry;

    @Inject
    ScreenActionCodec screenActionCodec;

    @Inject
    MessagingFacade messagingFacade;

    @Inject
    CallbackFacade callbackFacade;

    @Inject
    ChatActionsFacade chatActionsFacade;

    @Inject
    Dispatcher dispatcher;

    @Test
    void wiresDefaultBeansAndDispatcherAssembly() {
        assertEquals("test-token", config.token());
        assertEquals("https://platform-api.max.ru", config.baseUri().toString());
        assertEquals(Duration.ofSeconds(5), config.connectTimeout());
        assertEquals(Duration.ofSeconds(30), config.readTimeout());
        assertEquals(5000, okHttpClient.connectTimeoutMillis());
        assertEquals(30000, okHttpClient.readTimeoutMillis());
        assertInstanceOf(OkHttpMaxHttpClient.class, maxHttpClient);
        assertInstanceOf(JacksonJsonCodec.class, jsonCodec);
        assertInstanceOf(DefaultMaxBotClient.class, maxBotClient);
        assertInstanceOf(MemoryStorage.class, fsmStorage);
        assertInstanceOf(InMemorySceneRegistry.class, sceneRegistry);
        assertInstanceOf(MemorySceneStorage.class, sceneStorage);
        assertInstanceOf(InMemoryScreenRegistry.class, screenRegistry);
        assertInstanceOf(LegacyStringScreenActionCodec.class, screenActionCodec);
        assertTrue(messagingFacade != null);
        assertTrue(callbackFacade != null);
        assertTrue(chatActionsFacade != null);
        assertEquals("max.screen", dispatcher.applicationData().get(Screens.SCREEN_FSM_NAMESPACE_KEY));
        assertSame(screenRegistry, dispatcher.applicationData().get(RuntimeDataKey.application(
                "service:" + ScreenRegistry.class.getName(), ScreenRegistry.class)));
        assertSame(screenActionCodec, dispatcher.applicationData().get(RuntimeDataKey.application(
                "service:" + ScreenActionCodec.class.getName(), ScreenActionCodec.class)));
        assertTrue(dispatcher.outerMiddlewares().stream().anyMatch(ScreenMiddleware.class::isInstance));
    }
}
