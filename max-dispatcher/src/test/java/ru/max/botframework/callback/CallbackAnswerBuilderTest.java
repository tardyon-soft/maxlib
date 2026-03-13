package ru.max.botframework.callback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import ru.max.botframework.model.CallbackId;

class CallbackAnswerBuilderTest {

    @Test
    void mapsBuilderToLowLevelRequest() {
        var request = CallbackAnswers.text("Done")
                .notify(false)
                .cacheSeconds(15)
                .toRequest(new CallbackId("cb-1"));

        assertEquals("cb-1", request.callbackId().value());
        assertEquals("Done", request.text());
        assertEquals(Boolean.FALSE, request.sendNotification());
        assertEquals(15, request.cacheSeconds());
    }

    @Test
    void validatesTextAndCacheSeconds() {
        assertThrows(IllegalArgumentException.class, () -> CallbackAnswers.text(" "));
        assertThrows(IllegalArgumentException.class, () -> CallbackAnswers.answer().cacheSeconds(-1));
    }
}
