package ru.max.botframework.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UpdateHandlingResultTest {

    @Test
    void successResultHasNoError() {
        UpdateHandlingResult result = UpdateHandlingResult.success();

        assertTrue(result.isSuccess());
        assertEquals(UpdateHandlingStatus.SUCCESS, result.status());
        assertTrue(result.error().isEmpty());
    }

    @Test
    void failureResultStoresError() {
        RuntimeException error = new RuntimeException("boom");

        UpdateHandlingResult result = UpdateHandlingResult.failure(error);

        assertFalse(result.isSuccess());
        assertEquals(UpdateHandlingStatus.FAILURE, result.status());
        assertTrue(result.error().isPresent());
        assertSame(error, result.error().orElseThrow());
    }

    @Test
    void failureRequiresError() {
        assertThrows(NullPointerException.class, () -> UpdateHandlingResult.failure(null));
    }
}
