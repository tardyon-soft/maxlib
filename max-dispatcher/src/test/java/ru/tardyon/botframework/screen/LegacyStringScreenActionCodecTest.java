package ru.tardyon.botframework.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class LegacyStringScreenActionCodecTest {

    @Test
    void roundTripPreservesActionAndArgs() {
        LegacyStringScreenActionCodec codec = new LegacyStringScreenActionCodec();
        String payload = codec.encode("save", Map.of("id", "42", "name", "John Doe"));

        DecodedAction<Map<String, String>> decoded = codec.decode(payload).orElseThrow();
        assertEquals("save", decoded.action());
        assertEquals("42", decoded.payload().get("id"));
        assertEquals("John Doe", decoded.payload().get("name"));
    }

    @Test
    void invalidPayloadReturnsEmpty() {
        LegacyStringScreenActionCodec codec = new LegacyStringScreenActionCodec();

        assertTrue(codec.decode(null).isEmpty());
        assertTrue(codec.decode("").isEmpty());
        assertTrue(codec.decode("ui:v1:a=save").isEmpty());
        assertTrue(codec.decode("ui:act:").isEmpty());
    }
}

