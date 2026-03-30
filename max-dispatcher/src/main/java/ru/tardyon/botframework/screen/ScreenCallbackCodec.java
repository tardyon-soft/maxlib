package ru.tardyon.botframework.screen;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

final class ScreenCallbackCodec {
    private static final String NAV_BACK = "ui:nav:back";
    private static final String NAV_HOME = "ui:nav:home";
    private static final String NAV_REFRESH = "ui:nav:refresh";

    private final ScreenActionCodec primaryActionCodec;
    private final ScreenActionCodec fallbackActionCodec;

    ScreenCallbackCodec(ScreenActionCodec primaryActionCodec) {
        this(primaryActionCodec, new LegacyStringScreenActionCodec());
    }

    ScreenCallbackCodec(ScreenActionCodec primaryActionCodec, ScreenActionCodec fallbackActionCodec) {
        this.primaryActionCodec = Objects.requireNonNull(primaryActionCodec, "primaryActionCodec");
        this.fallbackActionCodec = Objects.requireNonNull(fallbackActionCodec, "fallbackActionCodec");
    }

    static ScreenCallbackCodec legacy() {
        return new ScreenCallbackCodec(new LegacyStringScreenActionCodec());
    }

    String navBack() {
        return NAV_BACK;
    }

    String action(String action, Map<String, String> args) {
        return primaryActionCodec.encode(action, args);
    }

    Optional<ParsedScreenCallback> parse(String data) {
        if (data == null || data.isBlank() || !data.startsWith("ui:")) {
            return Optional.empty();
        }
        if (NAV_BACK.equals(data)) {
            return Optional.of(new ParsedScreenCallback(ParsedScreenCallback.Kind.NAV_BACK, null, Map.of()));
        }
        if (NAV_HOME.equals(data)) {
            return Optional.of(new ParsedScreenCallback(ParsedScreenCallback.Kind.NAV_HOME, null, Map.of()));
        }
        if (NAV_REFRESH.equals(data)) {
            return Optional.of(new ParsedScreenCallback(ParsedScreenCallback.Kind.NAV_REFRESH, null, Map.of()));
        }

        Optional<DecodedAction<Map<String, String>>> decoded = primaryActionCodec.decode(data);
        if (decoded.isEmpty() && primaryActionCodec.mode() != fallbackActionCodec.mode()) {
            decoded = fallbackActionCodec.decode(data);
        }
        if (decoded.isPresent()) {
            DecodedAction<Map<String, String>> action = decoded.orElseThrow();
            return Optional.of(new ParsedScreenCallback(
                    ParsedScreenCallback.Kind.ACTION,
                    action.action(),
                    action.payload()
            ));
        }
        return Optional.of(new ParsedScreenCallback(ParsedScreenCallback.Kind.UNKNOWN, null, Map.of()));
    }

    record ParsedScreenCallback(Kind kind, String action, Map<String, String> args) {
        enum Kind {
            ACTION,
            NAV_BACK,
            NAV_HOME,
            NAV_REFRESH,
            UNKNOWN
        }
    }
}
