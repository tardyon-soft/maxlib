package ru.max.botframework.ingestion;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import ru.max.botframework.client.serialization.JacksonJsonCodec;

final class IngestionFixtures {
    private static final String BASE_PATH = "fixtures/ingestion/";
    private static final JacksonJsonCodec JSON = new JacksonJsonCodec();

    private IngestionFixtures() {
    }

    static String raw(String fixtureName) {
        try (InputStream stream = IngestionFixtures.class.getClassLoader().getResourceAsStream(BASE_PATH + fixtureName)) {
            if (stream == null) {
                throw new IllegalArgumentException("Fixture not found: " + fixtureName);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ioException) {
            throw new IllegalStateException("Unable to read fixture: " + fixtureName, ioException);
        }
    }

    static <T> T read(String fixtureName, Class<T> targetType) {
        return JSON.read(raw(fixtureName), targetType);
    }
}
