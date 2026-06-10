package ru.tardyon.botframework.quarkus.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.client.MaxBotClient;
import ru.tardyon.botframework.client.MaxRequest;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.message.MediaMessagingFacade;
import ru.tardyon.botframework.screen.Screens;
import ru.tardyon.botframework.upload.InputFile;
import ru.tardyon.botframework.upload.UploadRequest;
import ru.tardyon.botframework.upload.UploadResult;
import ru.tardyon.botframework.upload.UploadService;

@QuarkusComponentTest(MaxBotProducer.class)
@TestConfigProperty(key = "max.bot.token", value = "test-token")
class MaxBotProducerRouterAndMediaWiringTest {
    @Inject
    Dispatcher dispatcher;

    @Inject
    MediaMessagingFacade mediaMessagingFacade;

    @Inject
    MaxBotClient maxBotClient;

    @Test
    void registersRoutersInOrderAndCreatesMediaFacadeWhenUploadServiceExists() {
        assertEquals(List.of("a", "b"), dispatcher.routers().stream().map(Router::name).toList());
        assertNotNull(mediaMessagingFacade);
        assertInstanceOf(MaxBotClient.class, maxBotClient);
        assertTrue(dispatcher.outerMiddlewares().size() >= 1);
        assertEquals("max.screen", dispatcher.applicationData().get(Screens.SCREEN_FSM_NAMESPACE_KEY));
    }

    @Singleton
    static final class RouterBeans {
        @Produces
        @jakarta.annotation.Priority(1)
        Router routerA() {
            return new Router("a");
        }

        @Produces
        @jakarta.annotation.Priority(2)
        Router routerB() {
            return new Router("b");
        }
    }

    @Singleton
    static final class UploadServiceBeans {
        @Produces
        UploadService uploadService() {
            return new UploadService() {
                @Override
                public CompletionStage<UploadResult> upload(InputFile inputFile, UploadRequest request) {
                    return CompletableFuture.completedFuture(null);
                }
            };
        }
    }
}
