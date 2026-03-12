package ru.max.botframework.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.max.botframework.model.Chat;
import ru.max.botframework.model.ChatId;
import ru.max.botframework.model.ChatType;
import ru.max.botframework.model.Message;
import ru.max.botframework.model.MessageId;
import ru.max.botframework.model.Update;
import ru.max.botframework.model.UpdateId;
import ru.max.botframework.model.UpdateType;
import ru.max.botframework.model.User;
import ru.max.botframework.model.UserId;

class ApplicationDataParameterResolverTest {

    @Test
    void resolvesApplicationDataByUniqueType() throws Exception {
        RuntimeContext context = new RuntimeContext(sampleUpdate());
        context.putData(RuntimeDataKey.application("service.value", String.class), "orders");
        HandlerParameterDescriptor parameter = descriptor(String.class);

        HandlerParameterResolution result = new ApplicationDataParameterResolver()
                .resolve(parameter, new HandlerInvocationContext(context.update().message(), context));

        assertTrue(result.supported());
        assertEquals("orders", result.value());
    }

    @Test
    void returnsUnsupportedWhenNoApplicationDataFound() throws Exception {
        RuntimeContext context = new RuntimeContext(sampleUpdate());
        HandlerParameterDescriptor parameter = descriptor(String.class);

        HandlerParameterResolution result = new ApplicationDataParameterResolver()
                .resolve(parameter, new HandlerInvocationContext(context.update().message(), context));

        assertFalse(result.supported());
    }

    @Test
    void throwsWhenResolutionIsAmbiguous() throws Exception {
        RuntimeContext context = new RuntimeContext(sampleUpdate());
        context.putData(RuntimeDataKey.application("service.a", String.class), "a");
        context.putData(RuntimeDataKey.application("service.b", String.class), "b");
        HandlerParameterDescriptor parameter = descriptor(String.class);

        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> new ApplicationDataParameterResolver()
                        .resolve(parameter, new HandlerInvocationContext(context.update().message(), context))
        );

        assertTrue(thrown.getMessage().contains("ambiguous application data"));
    }

    private static HandlerParameterDescriptor descriptor(Class<?> type) throws Exception {
        Method method = Target.class.getDeclaredMethod("handler", type);
        return new HandlerParameterDescriptor(0, method.getParameters()[0]);
    }

    private static final class Target {
        @SuppressWarnings("unused")
        public void handler(String value) {
        }
    }

    private static Update sampleUpdate() {
        return new Update(
                new UpdateId("u-app-res-1"),
                UpdateType.MESSAGE,
                new Message(
                        new MessageId("m-app-res-1"),
                        new Chat(new ChatId("c-app-res-1"), ChatType.PRIVATE, "chat", null, null),
                        new User(new UserId("u-app-res-1"), "demo", "Demo", "User", "Demo User", false, "en"),
                        "hello",
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
