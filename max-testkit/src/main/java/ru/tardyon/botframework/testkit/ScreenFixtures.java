package ru.tardyon.botframework.testkit;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.fsm.FSMContext;
import ru.tardyon.botframework.screen.FsmScreenStorage;
import ru.tardyon.botframework.screen.LegacyStringScreenActionCodec;
import ru.tardyon.botframework.screen.ScreenActionCodec;
import ru.tardyon.botframework.screen.ScreenSession;
import ru.tardyon.botframework.screen.ScreenStackEntry;

/**
 * Fixtures for screen callback payloads and persisted screen state.
 */
public final class ScreenFixtures {
    private ScreenFixtures() {
    }

    public static String actionPayload(String action) {
        return actionPayload(action, Map.of(), new LegacyStringScreenActionCodec());
    }

    public static String actionPayload(String action, Map<String, String> args) {
        return actionPayload(action, args, new LegacyStringScreenActionCodec());
    }

    public static String actionPayload(String action, Map<String, String> args, ScreenActionCodec codec) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(args, "args");
        Objects.requireNonNull(codec, "codec");
        return codec.encode(action, args);
    }

    public static String navBackPayload() {
        return "ui:nav:back";
    }

    public static ScreenSession activeSession(String screenId) {
        return activeSession(screenId, Map.of());
    }

    public static ScreenSession activeSession(String screenId, Map<String, Object> params) {
        Objects.requireNonNull(screenId, "screenId");
        Objects.requireNonNull(params, "params");
        Instant now = Instant.parse("2026-03-30T12:00:00Z");
        return new ScreenSession(
                "fixture-scope",
                List.of(new ScreenStackEntry(screenId, params, now)),
                "fixture-root-message",
                now
        );
    }

    public static CompletionStage<Void> seedState(FSMContext screenFsm, ScreenSession session) {
        Objects.requireNonNull(screenFsm, "screenFsm");
        Objects.requireNonNull(session, "session");
        return new FsmScreenStorage().set(screenFsm, session);
    }
}
