package ru.tardyon.botframework.dispatcher;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.message.MediaMessagingFacade;
import ru.tardyon.botframework.model.Chat;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.ChatType;
import ru.tardyon.botframework.model.Message;
import ru.tardyon.botframework.model.MessageId;
import ru.tardyon.botframework.model.Update;
import ru.tardyon.botframework.model.UpdateId;
import ru.tardyon.botframework.model.UpdateType;
import ru.tardyon.botframework.model.User;
import ru.tardyon.botframework.model.UserId;

class RuntimeMessagingFacadeParameterResolverTest {

    @Test
    void resolvesMediaMessagingFacadeWhenPresentInRuntimeData() throws Exception {
        RuntimeContext context = new RuntimeContext(sampleUpdate("hello"));
        MediaMessagingFacade mediaFacade = org.mockito.Mockito.mock(MediaMessagingFacade.class);
        context.putData(RuntimeMessagingSupport.MEDIA_MESSAGING_FACADE_KEY, mediaFacade);
        HandlerParameterDescriptor parameter = descriptor("expectsMedia", MediaMessagingFacade.class);

        HandlerParameterResolution result = new RuntimeMessagingFacadeParameterResolver()
                .resolve(parameter, new HandlerInvocationContext(context.update().message(), context));

        assertTrue(result.supported());
        assertSame(mediaFacade, result.value());
    }

    @Test
    void returnsUnsupportedForMediaFacadeWhenNotPresent() throws Exception {
        RuntimeContext context = new RuntimeContext(sampleUpdate("hello"));
        HandlerParameterDescriptor parameter = descriptor("expectsMedia", MediaMessagingFacade.class);

        HandlerParameterResolution result = new RuntimeMessagingFacadeParameterResolver()
                .resolve(parameter, new HandlerInvocationContext(context.update().message(), context));

        assertFalse(result.supported());
    }

    private static HandlerParameterDescriptor descriptor(String methodName, Class<?> type) throws Exception {
        Method method = ParameterTarget.class.getDeclaredMethod(methodName, type);
        return new HandlerParameterDescriptor(0, method.getParameters()[0]);
    }

    private static final class ParameterTarget {
        @SuppressWarnings("unused")
        public void expectsMedia(MediaMessagingFacade media) {
        }
    }

    private static Update sampleUpdate(String text) {
        return new Update(
                new UpdateId("u-media-resolver"),
                UpdateType.MESSAGE,
                new Message(
                        new MessageId("m-media-resolver"),
                        new Chat(new ChatId("c-media-resolver"), ChatType.PRIVATE, "chat", null, null),
                        new User(new UserId("u-media-resolver"), "demo", "Demo", "User", "Demo User", false, "en"),
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
