package ru.max.botframework.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.max.botframework.model.Callback;
import ru.max.botframework.model.CallbackId;
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

class BuiltInParameterResolversTest {

    @Test
    void updateResolverResolvesCurrentUpdate() throws Exception {
        Update update = updateWithMessage("hello");
        RuntimeContext ctx = new RuntimeContext(update);
        HandlerParameterDescriptor parameter = descriptor("expectsUpdate", Update.class);

        HandlerParameterResolution result = new UpdateParameterResolver()
                .resolve(parameter, new HandlerInvocationContext(update.message(), ctx));

        assertTrue(result.supported());
        assertSame(update, result.value());
    }

    @Test
    void messageResolverResolvesMessageEvent() throws Exception {
        Update update = updateWithMessage("hello");
        RuntimeContext ctx = new RuntimeContext(update);
        HandlerParameterDescriptor parameter = descriptor("expectsMessage", Message.class);

        HandlerParameterResolution result = new MessageParameterResolver()
                .resolve(parameter, new HandlerInvocationContext(update.message(), ctx));

        assertTrue(result.supported());
        Message message = (Message) result.value();
        assertEquals("hello", message.text());
    }

    @Test
    void messageResolverResolvesFromCallbackUpdate() throws Exception {
        Update update = updateWithCallback("pay:1");
        RuntimeContext ctx = new RuntimeContext(update);
        HandlerParameterDescriptor parameter = descriptor("expectsMessage", Message.class);

        HandlerParameterResolution result = new MessageParameterResolver()
                .resolve(parameter, new HandlerInvocationContext(update, ctx));

        assertTrue(result.supported());
        Message message = (Message) result.value();
        assertEquals("source", message.text());
    }

    @Test
    void callbackResolverResolvesFromUpdate() throws Exception {
        Update update = updateWithCallback("pay:9");
        RuntimeContext ctx = new RuntimeContext(update);
        HandlerParameterDescriptor parameter = descriptor("expectsCallback", Callback.class);

        HandlerParameterResolution result = new CallbackParameterResolver()
                .resolve(parameter, new HandlerInvocationContext(update, ctx));

        assertTrue(result.supported());
        Callback callback = (Callback) result.value();
        assertEquals("pay:9", callback.data());
    }

    @Test
    void userResolverResolvesFromMessageAndCallback() throws Exception {
        HandlerParameterDescriptor parameter = descriptor("expectsUser", User.class);

        Update messageUpdate = updateWithMessage("hello");
        HandlerParameterResolution fromMessage = new UserParameterResolver()
                .resolve(parameter, new HandlerInvocationContext(messageUpdate.message(), new RuntimeContext(messageUpdate)));

        Update callbackUpdate = updateWithCallback("pay:3");
        HandlerParameterResolution fromCallback = new UserParameterResolver()
                .resolve(parameter, new HandlerInvocationContext(callbackUpdate.callback(), new RuntimeContext(callbackUpdate)));

        assertTrue(fromMessage.supported());
        assertTrue(fromCallback.supported());
        assertEquals("u-1", ((User) fromMessage.value()).id().value());
        assertEquals("u-1", ((User) fromCallback.value()).id().value());
    }

    @Test
    void chatResolverResolvesFromCallbackSourceMessage() throws Exception {
        HandlerParameterDescriptor parameter = descriptor("expectsChat", Chat.class);
        Update callbackUpdate = updateWithCallback("pay:7");

        HandlerParameterResolution result = new ChatParameterResolver()
                .resolve(parameter, new HandlerInvocationContext(callbackUpdate.callback(), new RuntimeContext(callbackUpdate)));

        assertTrue(result.supported());
        Chat chat = (Chat) result.value();
        assertEquals("c-1", chat.id().value());
    }

    @Test
    void runtimeContextResolverReturnsCurrentContext() throws Exception {
        HandlerParameterDescriptor parameter = descriptor("expectsContext", RuntimeContext.class);
        Update update = updateWithMessage("hello");
        RuntimeContext context = new RuntimeContext(update);

        HandlerParameterResolution result = new RuntimeContextParameterResolver()
                .resolve(parameter, new HandlerInvocationContext(update.message(), context));

        assertTrue(result.supported());
        assertSame(context, result.value());
    }

    @Test
    void callbackResolverReturnsUnsupportedWhenNotApplicable() throws Exception {
        HandlerParameterDescriptor parameter = descriptor("expectsCallback", Callback.class);
        Update messageUpdate = updateWithMessage("hello");

        HandlerParameterResolution result = new CallbackParameterResolver()
                .resolve(parameter, new HandlerInvocationContext(messageUpdate.message(), new RuntimeContext(messageUpdate)));

        assertFalse(result.supported());
    }

    private static HandlerParameterDescriptor descriptor(String methodName, Class<?> type) throws Exception {
        Method method = ParameterTarget.class.getDeclaredMethod(methodName, type);
        return new HandlerParameterDescriptor(0, method.getParameters()[0]);
    }

    private static final class ParameterTarget {
        public void expectsUpdate(Update update) {
        }

        public void expectsMessage(Message message) {
        }

        public void expectsCallback(Callback callback) {
        }

        public void expectsUser(User user) {
        }

        public void expectsChat(Chat chat) {
        }

        public void expectsContext(RuntimeContext context) {
        }
    }

    private static Update updateWithMessage(String text) {
        return new Update(
                new UpdateId("u-1"),
                UpdateType.MESSAGE,
                message(text),
                null,
                null,
                Instant.parse("2026-03-12T00:00:00Z")
        );
    }

    private static Update updateWithCallback(String data) {
        return new Update(
                new UpdateId("u-2"),
                UpdateType.CALLBACK,
                null,
                new Callback(
                        new CallbackId("cb-1"),
                        data,
                        user(),
                        message("source"),
                        Instant.parse("2026-03-12T00:00:01Z")
                ),
                null,
                Instant.parse("2026-03-12T00:00:01Z")
        );
    }

    private static Message message(String text) {
        return new Message(
                new MessageId("m-1"),
                new Chat(new ChatId("c-1"), ChatType.PRIVATE, "chat", null, null),
                user(),
                text,
                Instant.parse("2026-03-12T00:00:00Z"),
                null,
                List.of(),
                List.of()
        );
    }

    private static User user() {
        return new User(new UserId("u-1"), "demo", "Demo", "User", "Demo User", false, "en");
    }
}
