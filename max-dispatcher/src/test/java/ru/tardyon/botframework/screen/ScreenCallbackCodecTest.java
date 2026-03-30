package ru.tardyon.botframework.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ScreenCallbackCodecTest {

    @Test
    void typedCodecFallsBackToLegacyParsing() {
        ScreenCallbackCodec codec = new ScreenCallbackCodec(new TypedV1ScreenActionCodec());
        String legacyPayload = new LegacyStringScreenActionCodec().encode("open", Map.of("id", "7"));

        ScreenCallbackCodec.ParsedScreenCallback parsed = codec.parse(legacyPayload).orElseThrow();
        assertEquals(ScreenCallbackCodec.ParsedScreenCallback.Kind.ACTION, parsed.kind());
        assertEquals("open", parsed.action());
        assertEquals("7", parsed.args().get("id"));
    }

    @Test
    void navBackPayloadIsRecognized() {
        ScreenCallbackCodec codec = ScreenCallbackCodec.legacy();
        ScreenCallbackCodec.ParsedScreenCallback parsed = codec.parse(codec.navBack()).orElseThrow();

        assertEquals(ScreenCallbackCodec.ParsedScreenCallback.Kind.NAV_BACK, parsed.kind());
    }

    @Test
    void invalidUiPayloadIsMarkedUnknown() {
        ScreenCallbackCodec codec = new ScreenCallbackCodec(new TypedV1ScreenActionCodec());
        ScreenCallbackCodec.ParsedScreenCallback parsed = codec.parse("ui:v1:").orElseThrow();

        assertEquals(ScreenCallbackCodec.ParsedScreenCallback.Kind.UNKNOWN, parsed.kind());
        assertTrue(parsed.args().isEmpty());
    }
}

