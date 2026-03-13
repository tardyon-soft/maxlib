package ru.max.botframework.fsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.max.botframework.model.ChatId;
import ru.max.botframework.model.UserId;

class StateModelTest {

    @Test
    void validatesScopeSpecificStateKeyShape() {
        assertThrows(IllegalArgumentException.class, () -> new StateKey(StateScope.USER, null, null));
        assertThrows(IllegalArgumentException.class, () -> new StateKey(StateScope.USER, new UserId("u-1"), new ChatId("c-1")));

        assertThrows(IllegalArgumentException.class, () -> new StateKey(StateScope.CHAT, null, null));
        assertThrows(IllegalArgumentException.class, () -> new StateKey(StateScope.CHAT, new UserId("u-1"), new ChatId("c-1")));

        assertThrows(IllegalArgumentException.class, () -> new StateKey(StateScope.USER_IN_CHAT, null, new ChatId("c-1")));
        assertThrows(IllegalArgumentException.class, () -> new StateKey(StateScope.USER_IN_CHAT, new UserId("u-1"), null));
    }

    @Test
    void storesStatePayloadImmutablyAndSupportsTypedRead() {
        Map<String, Object> mutable = new HashMap<>();
        mutable.put("attempt", 1);
        StateData data = StateData.of(mutable);

        mutable.put("attempt", 2);

        assertEquals(1, data.get("attempt", Integer.class).orElseThrow());
        assertTrue(data.get("missing").isEmpty());
        assertTrue(data.get("attempt", String.class).isEmpty());

        assertThrows(UnsupportedOperationException.class, () -> data.values().put("x", "y"));
    }

    @Test
    void snapshotRepresentsCurrentStateAndPayload() {
        StateKey key = StateKey.userInChat(new UserId("u-1"), new ChatId("c-1"));
        StateSnapshot snapshot = StateSnapshot.empty(key)
                .withState("checkout.email")
                .withData(StateData.of(Map.of("email", "a@example.com")));

        assertEquals("checkout.email", snapshot.state().orElseThrow());
        assertEquals("a@example.com", snapshot.data().get("email", String.class).orElseThrow());

        StateSnapshot cleared = snapshot.withoutState();
        assertTrue(cleared.state().isEmpty());
    }
}
