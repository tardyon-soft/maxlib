package ru.max.botframework.testkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.max.botframework.model.ChatType;
import ru.max.botframework.model.UpdateType;

class UpdateFixturesTest {

    @Test
    void messageBuilderCreatesTypedMessageUpdate() {
        var update = UpdateFixtures.message()
                .userId("u-42")
                .chatId("c-42")
                .text("hello")
                .chatType(ChatType.GROUP)
                .build();

        assertEquals(UpdateType.MESSAGE, update.type());
        assertNotNull(update.message());
        assertEquals("u-42", update.message().from().id().value());
        assertEquals("c-42", update.message().chat().id().value());
        assertEquals(ChatType.GROUP, update.message().chat().type());
        assertEquals("hello", update.message().text());
    }

    @Test
    void callbackBuilderCreatesTypedCallbackUpdate() {
        var update = UpdateFixtures.callback()
                .userId("u-7")
                .chatId("c-7")
                .data("order:pay")
                .sourceMessageText("source")
                .build();

        assertEquals(UpdateType.CALLBACK, update.type());
        assertNotNull(update.callback());
        assertEquals("order:pay", update.callback().data());
        assertEquals("u-7", update.callback().from().id().value());
        assertEquals("c-7", update.callback().message().chat().id().value());
    }

    @Test
    void statefulMessagesKeepsSameScopeAcrossFlow() {
        List<ru.max.botframework.model.Update> flow = UpdateFixtures.statefulMessages("u-1", "c-1", "one", "two", "three");

        assertEquals(3, flow.size());
        assertEquals("u-1", flow.getFirst().message().from().id().value());
        assertEquals("u-1", flow.get(1).message().from().id().value());
        assertEquals("c-1", flow.get(2).message().chat().id().value());
        assertEquals("three", flow.get(2).message().text());
    }
}
