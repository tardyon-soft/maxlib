package ru.tardyon.botframework.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.fsm.MemoryStorage;
import ru.tardyon.botframework.fsm.StateKey;
import ru.tardyon.botframework.fsm.StateKeyStrategies;
import ru.tardyon.botframework.fsm.StateScope;
import ru.tardyon.botframework.model.Callback;
import ru.tardyon.botframework.model.CallbackId;
import ru.tardyon.botframework.model.Chat;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.ChatType;
import ru.tardyon.botframework.model.FileId;
import ru.tardyon.botframework.model.Message;
import ru.tardyon.botframework.model.MessageAttachment;
import ru.tardyon.botframework.model.MessageAttachmentType;
import ru.tardyon.botframework.model.MessageId;
import ru.tardyon.botframework.model.User;
import ru.tardyon.botframework.model.UserId;

class BuiltInFiltersTest {

    @Test
    void commandFilterMatchesAndExtractsArgs() {
        FilterResult result = BuiltInFilters.command("start")
                .test(message("/start hello world", ChatType.PRIVATE, user("u-1"), List.of()))
                .toCompletableFuture()
                .join();

        assertEquals(FilterStatus.MATCHED, result.status());
        assertEquals("start", result.enrichment().get(BuiltInFilters.COMMAND_KEY));
        assertEquals("hello world", result.enrichment().get(BuiltInFilters.COMMAND_ARGS_KEY));
    }

    @Test
    void textEqualsFilterMatchesExactText() {
        FilterResult result = BuiltInFilters.textEquals("ping")
                .test(message("ping", ChatType.PRIVATE, user("u-1"), List.of()))
                .toCompletableFuture()
                .join();

        assertEquals(FilterStatus.MATCHED, result.status());
    }

    @Test
    void textStartsWithFilterMatchesAndExtractsSuffix() {
        FilterResult result = BuiltInFilters.textStartsWith("pay:")
                .test(message("pay:42", ChatType.PRIVATE, user("u-1"), List.of()))
                .toCompletableFuture()
                .join();

        assertEquals(FilterStatus.MATCHED, result.status());
        assertEquals("42", result.enrichment().get(BuiltInFilters.TEXT_SUFFIX_KEY));
    }

    @Test
    void chatTypeFilterMatchesExpectedType() {
        FilterResult result = BuiltInFilters.chatType(ChatType.GROUP)
                .test(message("hi", ChatType.GROUP, user("u-1"), List.of()))
                .toCompletableFuture()
                .join();

        assertEquals(FilterStatus.MATCHED, result.status());
        assertEquals("group", result.enrichment().get(BuiltInFilters.CHAT_TYPE_KEY));
    }

    @Test
    void fromUserFilterMatchesMessageSender() {
        FilterResult result = BuiltInFilters.fromUser(new UserId("u-1"))
                .test(message("hi", ChatType.PRIVATE, user("u-1"), List.of()))
                .toCompletableFuture()
                .join();

        assertEquals(FilterStatus.MATCHED, result.status());
        assertEquals("u-1", result.enrichment().get(BuiltInFilters.USER_ID_KEY));
    }

    @Test
    void fromUserFilterMatchesCallbackSender() {
        Callback callback = new Callback(
                new CallbackId("cb-1"),
                "action:1",
                user("u-7"),
                message("source", ChatType.PRIVATE, user("u-7"), List.of()),
                Instant.parse("2026-03-12T00:00:01Z")
        );

        FilterResult result = BuiltInFilters.fromCallbackUser(new UserId("u-7"))
                .test(callback)
                .toCompletableFuture()
                .join();

        assertEquals(FilterStatus.MATCHED, result.status());
        assertEquals("u-7", result.enrichment().get(BuiltInFilters.USER_ID_KEY));
    }

    @Test
    void callbackDataEqualsMatchesExactValue() {
        Callback callback = new Callback(
                new CallbackId("cb-2"),
                "menu:pay",
                user("u-8"),
                message("source", ChatType.PRIVATE, user("u-8"), List.of()),
                Instant.parse("2026-03-12T00:00:02Z")
        );

        FilterResult result = BuiltInFilters.callbackDataEquals("menu:pay")
                .test(callback)
                .toCompletableFuture()
                .join();

        assertEquals(FilterStatus.MATCHED, result.status());
        assertEquals("menu:pay", result.enrichment().get(BuiltInFilters.CALLBACK_DATA_KEY));
    }

    @Test
    void callbackDataStartsWithMatchesAndExtractsSuffix() {
        Callback callback = new Callback(
                new CallbackId("cb-3"),
                "menu:pay:42",
                user("u-9"),
                message("source", ChatType.PRIVATE, user("u-9"), List.of()),
                Instant.parse("2026-03-12T00:00:03Z")
        );

        FilterResult result = BuiltInFilters.callbackDataStartsWith("menu:")
                .test(callback)
                .toCompletableFuture()
                .join();

        assertEquals(FilterStatus.MATCHED, result.status());
        assertEquals("menu:pay:42", result.enrichment().get(BuiltInFilters.CALLBACK_DATA_KEY));
        assertEquals("pay:42", result.enrichment().get(BuiltInFilters.CALLBACK_SUFFIX_KEY));
    }

    @Test
    void hasAttachmentFilterMatchesWhenAttachmentsPresent() {
        MessageAttachment attachment = new MessageAttachment(
                MessageAttachmentType.PHOTO,
                new FileId("f-1"),
                null,
                "image/jpeg",
                10L
        );
        FilterResult result = BuiltInFilters.hasAttachment()
                .test(message("photo", ChatType.PRIVATE, user("u-1"), List.of(attachment)))
                .toCompletableFuture()
                .join();

        assertEquals(FilterStatus.MATCHED, result.status());
    }

    @Test
    void stateFilterMatchesWhenCurrentStateEqualsExpected() {
        Message event = message("mail@example.com", ChatType.PRIVATE, user("u-1"), List.of());
        RuntimeContext context = runtimeContext(event);
        context.fsm().setState("checkout.waiting_email").toCompletableFuture().join();

        FilterResult result = BuiltInFilters.state("checkout.waiting_email")
                .test(event, context)
                .toCompletableFuture()
                .join();

        assertEquals(FilterStatus.MATCHED, result.status());
        assertEquals("checkout.waiting_email", result.enrichment().get(BuiltInFilters.STATE_KEY));
    }

    @Test
    void stateFilterReturnsNotMatchedWhenStateDiffers() {
        Message event = message("mail@example.com", ChatType.PRIVATE, user("u-1"), List.of());
        RuntimeContext context = runtimeContext(event);
        context.fsm().setState("checkout.waiting_name").toCompletableFuture().join();

        FilterResult result = BuiltInFilters.state("checkout.waiting_email")
                .test(event, context)
                .toCompletableFuture()
                .join();

        assertEquals(FilterStatus.NOT_MATCHED, result.status());
        assertTrue(result.enrichment().isEmpty());
    }

    @Test
    void stateFilterReturnsNotMatchedWhenStateIsMissing() {
        Message event = message("mail@example.com", ChatType.PRIVATE, user("u-1"), List.of());
        RuntimeContext context = runtimeContext(event);

        FilterResult result = BuiltInFilters.state("checkout.waiting_email")
                .test(event, context)
                .toCompletableFuture()
                .join();

        assertEquals(FilterStatus.NOT_MATCHED, result.status());
        assertFalse(result.isMatched());
    }

    @Test
    void stateInFilterMatchesAnyConfiguredState() {
        Message event = message("mail@example.com", ChatType.PRIVATE, user("u-1"), List.of());
        RuntimeContext context = runtimeContext(event);
        context.fsm().setState("checkout.confirm").toCompletableFuture().join();

        FilterResult result = BuiltInFilters.stateIn(Set.of("checkout.email", "checkout.confirm"))
                .test(event, context)
                .toCompletableFuture()
                .join();

        assertEquals(FilterStatus.MATCHED, result.status());
        assertEquals("checkout.confirm", result.enrichment().get(BuiltInFilters.STATE_KEY));
    }

    @Test
    void builtInFiltersReturnNotMatchedForNegativeCase() {
        FilterResult result = BuiltInFilters.command("start")
                .test(message("/help", ChatType.PRIVATE, user("u-1"), List.of()))
                .toCompletableFuture()
                .join();

        assertEquals(FilterStatus.NOT_MATCHED, result.status());
    }

    @Test
    void commandFilterHasHigherPriorityThanDefaultFilters() {
        assertTrue(BuiltInFilters.command("start").priority() > Filter.any().priority());
    }

    private static Message message(String text, ChatType chatType, User from, List<MessageAttachment> attachments) {
        return new Message(
                new MessageId("m-1"),
                new Chat(new ChatId("c-1"), chatType, "chat", null, null),
                from,
                text,
                Instant.parse("2026-03-12T00:00:00Z"),
                null,
                List.of(),
                attachments
        );
    }

    private static User user(String id) {
        return new User(new UserId(id), "demo", "Demo", "User", "Demo User", false, "en");
    }

    private static RuntimeContext runtimeContext(Message message) {
        RuntimeContext context = new RuntimeContext(new ru.tardyon.botframework.model.Update(
                new ru.tardyon.botframework.model.UpdateId("u-state-1"),
                ru.tardyon.botframework.model.UpdateType.MESSAGE,
                message,
                null,
                null,
                Instant.parse("2026-03-12T00:00:00Z")
        ));
        MemoryStorage storage = new MemoryStorage();
        StateKey key = StateKey.userInChat(message.from().id(), message.chat().id());
        storage.clear(key).toCompletableFuture().join();
        FSMRuntimeSupport.bootstrap(context, storage, StateKeyStrategies.forScope(StateScope.USER_IN_CHAT));
        return context;
    }
}
