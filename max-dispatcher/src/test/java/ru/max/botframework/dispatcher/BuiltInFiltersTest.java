package ru.max.botframework.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.max.botframework.model.Callback;
import ru.max.botframework.model.CallbackId;
import ru.max.botframework.model.Chat;
import ru.max.botframework.model.ChatId;
import ru.max.botframework.model.ChatType;
import ru.max.botframework.model.FileId;
import ru.max.botframework.model.Message;
import ru.max.botframework.model.MessageAttachment;
import ru.max.botframework.model.MessageAttachmentType;
import ru.max.botframework.model.MessageId;
import ru.max.botframework.model.User;
import ru.max.botframework.model.UserId;

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
    void stateFilterPlaceholderReturnsFailed() {
        FilterResult result = BuiltInFilters.state("checkout.waiting_email")
                .test(message("mail@example.com", ChatType.PRIVATE, user("u-1"), List.of()))
                .toCompletableFuture()
                .join();

        assertEquals(FilterStatus.FAILED, result.status());
        Throwable error = result.errorOpt().orElseThrow();
        assertTrue(error instanceof UnsupportedOperationException);
    }

    @Test
    void builtInFiltersReturnNotMatchedForNegativeCase() {
        FilterResult result = BuiltInFilters.command("start")
                .test(message("/help", ChatType.PRIVATE, user("u-1"), List.of()))
                .toCompletableFuture()
                .join();

        assertEquals(FilterStatus.NOT_MATCHED, result.status());
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
}
