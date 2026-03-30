package ru.tardyon.botframework.screen;

import java.util.Map;
import java.util.Optional;

/**
 * Codec for mapping logical screen action + args to callback payload and back.
 */
public interface ScreenActionCodec {

    ScreenActionCodecMode mode();

    String encode(String action, Map<String, String> args);

    Optional<DecodedAction<Map<String, String>>> decode(String payload);
}

