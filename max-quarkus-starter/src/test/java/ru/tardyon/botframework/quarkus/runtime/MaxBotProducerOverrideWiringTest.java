package ru.tardyon.botframework.quarkus.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.action.ChatActionsFacade;
import ru.tardyon.botframework.client.MaxApiClientConfig;
import ru.tardyon.botframework.client.MaxBotClient;
import ru.tardyon.botframework.client.MaxRequest;
import ru.tardyon.botframework.client.http.MaxHttpClient;
import ru.tardyon.botframework.client.serialization.JsonCodec;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.fsm.FSMStorage;
import ru.tardyon.botframework.fsm.InMemorySceneRegistry;
import ru.tardyon.botframework.fsm.MemorySceneStorage;
import ru.tardyon.botframework.fsm.MemoryStorage;
import ru.tardyon.botframework.fsm.SceneRegistry;
import ru.tardyon.botframework.fsm.SceneStorage;
import ru.tardyon.botframework.message.MessageTarget;
import ru.tardyon.botframework.model.response.OperationStatusResponse;
import ru.tardyon.botframework.screen.ScreenActionCodec;
import ru.tardyon.botframework.screen.ScreenRegistry;
import ru.tardyon.botframework.screen.TypedV1ScreenActionCodec;

@QuarkusComponentTest(MaxBotProducer.class)
@TestConfigProperty(key = "max.bot.token", value = "test-token")
class MaxBotProducerOverrideWiringTest {
    @Inject
    MaxApiClientConfig config;

    @Inject
    OkHttpClient okHttpClient;

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
    Dispatcher dispatcher;

    @Inject
    ChatActionsFacade chatActionsFacade;

    @Test
    void userBeansOverrideDefaultBeans() {
        assertEquals(URI.create("https://override.max.ru"), config.baseUri());
        assertSame(CustomBeans.CUSTOM_OK_HTTP_CLIENT, okHttpClient);
        assertSame(CustomBeans.CUSTOM_CLIENT, maxBotClient);
        assertSame(CustomBeans.CUSTOM_STORAGE, fsmStorage);
        assertSame(CustomBeans.CUSTOM_SCENE_REGISTRY, sceneRegistry);
        assertSame(CustomBeans.CUSTOM_SCENE_STORAGE, sceneStorage);
        assertSame(CustomBeans.CUSTOM_SCREEN_REGISTRY, screenRegistry);
        assertSame(CustomBeans.CUSTOM_SCREEN_CODEC, screenActionCodec);
        assertSame(CustomBeans.CUSTOM_DISPATCHER, dispatcher);

        chatActionsFacade.typing(new ru.tardyon.botframework.model.ChatId("chat-1"));
        assertEquals("SendChatActionMethodRequest", CustomBeans.CUSTOM_CLIENT.lastOperation.get());
    }

    @Singleton
    static final class CustomBeans {
        static final RecordingMaxBotClient CUSTOM_CLIENT = new RecordingMaxBotClient();
        static final OkHttpClient CUSTOM_OK_HTTP_CLIENT = new OkHttpClient();
        static final MemoryStorage CUSTOM_STORAGE = new MemoryStorage();
        static final InMemorySceneRegistry CUSTOM_SCENE_REGISTRY = new InMemorySceneRegistry();
        static final MemorySceneStorage CUSTOM_SCENE_STORAGE = new MemorySceneStorage();
        static final ScreenRegistry CUSTOM_SCREEN_REGISTRY = new ru.tardyon.botframework.screen.InMemoryScreenRegistry();
        static final ScreenActionCodec CUSTOM_SCREEN_CODEC = new TypedV1ScreenActionCodec();
        static final Dispatcher CUSTOM_DISPATCHER = new Dispatcher();

        @Produces
        MaxApiClientConfig config() {
            return MaxApiClientConfig.builder()
                    .token("override-token")
                    .baseUrl("https://override.max.ru")
                    .build();
        }

        @Produces
        OkHttpClient okHttpClient() {
            return CUSTOM_OK_HTTP_CLIENT;
        }

        @Produces
        MaxBotClient maxBotClient() {
            return CUSTOM_CLIENT;
        }

        @Produces
        FSMStorage fsmStorage() {
            return CUSTOM_STORAGE;
        }

        @Produces
        SceneRegistry sceneRegistry() {
            return CUSTOM_SCENE_REGISTRY;
        }

        @Produces
        SceneStorage sceneStorage() {
            return CUSTOM_SCENE_STORAGE;
        }

        @Produces
        ScreenRegistry screenRegistry() {
            return CUSTOM_SCREEN_REGISTRY;
        }

        @Produces
        ScreenActionCodec screenActionCodec() {
            return CUSTOM_SCREEN_CODEC;
        }

        @Produces
        Dispatcher dispatcher() {
            return CUSTOM_DISPATCHER;
        }
    }

    static final class RecordingMaxBotClient implements MaxBotClient {
        final AtomicReference<String> lastOperation = new AtomicReference<>();

        @Override
        public <T> T execute(MaxRequest<T> request) {
            lastOperation.set(request.getClass().getSimpleName());
            if (request.responseType() == OperationStatusResponse.class) {
                return request.responseType().cast(new OperationStatusResponse(true));
            }
            if (request.responseType() == Boolean.class) {
                return request.responseType().cast(Boolean.TRUE);
            }
            return null;
        }
    }
}
