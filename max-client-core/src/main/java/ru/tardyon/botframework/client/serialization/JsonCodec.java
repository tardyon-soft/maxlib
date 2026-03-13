package ru.tardyon.botframework.client.serialization;

/**
 * Serialization contract used by MAX client-core request/response mapping.
 */
public interface JsonCodec {
    <T> T read(String source, Class<T> targetType);

    String write(Object value);
}
