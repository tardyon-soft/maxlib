package ru.tardyon.botframework.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class TypedV1ScreenActionCodecTest {

    @Test
    void roundTripPreservesActionAndArgs() {
        TypedV1ScreenActionCodec codec = new TypedV1ScreenActionCodec();
        String payload = codec.encode("save", Map.of("id", "42", "name", "John Doe"));

        DecodedAction<Map<String, String>> decoded = codec.decode(payload).orElseThrow();
        assertEquals("save", decoded.action());
        assertEquals("42", decoded.payload().get("id"));
        assertEquals("John Doe", decoded.payload().get("name"));
    }

    @Test
    void invalidPayloadReturnsEmpty() {
        TypedV1ScreenActionCodec codec = new TypedV1ScreenActionCodec();

        assertTrue(codec.decode(null).isEmpty());
        assertTrue(codec.decode("").isEmpty());
        assertTrue(codec.decode("ui:act:save?id=1").isEmpty());
        assertTrue(codec.decode("ui:v1:arg.id=1").isEmpty());
    }

    @Test
    void throwsOnPayloadSizeOverflow() {
        TypedV1ScreenActionCodec codec = new TypedV1ScreenActionCodec(32);

        assertThrows(IllegalArgumentException.class, () ->
                codec.encode("save", Map.of("huge", "x".repeat(128))));
    }
}

