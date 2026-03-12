package ru.max.botframework.client.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import okhttp3.mockwebserver.MockResponse;

/**
 * Shared JSON fixture helper for client SDK tests.
 */
public final class JsonFixtures {
    private JsonFixtures() {
    }

    public static String read(String fixtureName) {
        String path = "/fixtures/" + fixtureName;
        try (InputStream stream = JsonFixtures.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalArgumentException("Fixture not found: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read fixture: " + path, e);
        }
    }

    public static MockResponse jsonResponse(String fixtureName) {
        return jsonResponse(200, fixtureName);
    }

    public static MockResponse jsonResponse(int statusCode, String fixtureName) {
        return new MockResponse()
                .setResponseCode(statusCode)
                .setHeader("Content-Type", "application/json")
                .setBody(read(Objects.requireNonNull(fixtureName, "fixtureName")));
    }
}
