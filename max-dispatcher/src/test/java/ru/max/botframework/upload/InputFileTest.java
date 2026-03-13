package ru.max.botframework.upload;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InputFileTest {

    @TempDir
    Path tempDir;

    @Test
    void createsFromPathWithKnownSizeAndReadableStream() throws Exception {
        Path file = tempDir.resolve("invoice.txt");
        Files.writeString(file, "invoice", StandardCharsets.UTF_8);

        InputFile input = InputFile.fromPath(file);

        assertEquals("invoice.txt", input.fileName());
        assertTrue(input.knownSize().isPresent());
        assertEquals(7L, input.knownSize().getAsLong());
        assertFalse(input.contentType().isPresent());
        assertEquals("invoice", new String(input.openStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void createsFromBytesWithKnownSizeAndDefensiveCopy() throws Exception {
        byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);
        InputFile input = InputFile.fromBytes(bytes, "hello.txt");
        bytes[0] = 'X';

        assertEquals("hello.txt", input.fileName());
        assertTrue(input.knownSize().isPresent());
        assertEquals(5L, input.knownSize().getAsLong());
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), input.openStream().readAllBytes());
    }

    @Test
    void createsFromStreamWithUnknownAndKnownSize() throws Exception {
        InputFile unknown = InputFile.fromStream(
                () -> new ByteArrayInputStream("stream".getBytes(StandardCharsets.UTF_8)),
                "stream.bin"
        );
        InputFile known = InputFile.fromStream(
                () -> new ByteArrayInputStream("stream".getBytes(StandardCharsets.UTF_8)),
                "stream.bin",
                6L
        );

        assertFalse(unknown.knownSize().isPresent());
        assertTrue(known.knownSize().isPresent());
        assertEquals(6L, known.knownSize().getAsLong());
        assertArrayEquals("stream".getBytes(StandardCharsets.UTF_8), unknown.openStream().readAllBytes());
    }

    @Test
    void supportsFileNameAndContentTypeOverrides() {
        InputFile input = InputFile.fromBytes("x".getBytes(StandardCharsets.UTF_8), "a.txt")
                .withFileName("b.txt")
                .withContentType("text/plain");

        assertEquals("b.txt", input.fileName());
        assertEquals("text/plain", input.contentType().orElseThrow());
    }

    @Test
    void validatesFactoryArguments() {
        assertThrows(IllegalArgumentException.class, () -> InputFile.fromBytes(new byte[]{1}, " "));
        assertThrows(IllegalArgumentException.class, () -> InputFile.fromStream(() -> new ByteArrayInputStream(new byte[0]), " ", 1L));
        assertThrows(IllegalArgumentException.class, () -> InputFile.fromStream(() -> new ByteArrayInputStream(new byte[0]), "file.bin", -1L));
        assertThrows(IllegalArgumentException.class, () -> InputFile.fromPath(tempDir.resolve("missing.bin")));
        assertThrows(IllegalArgumentException.class, () -> InputFile.fromBytes(new byte[]{1}, "a").withContentType(" "));
    }

    @Test
    void propagatesStreamSupplierIOException() {
        InputFile input = InputFile.fromStream(() -> {
            throw new IOException("boom");
        }, "f.bin");

        IOException exception = assertThrows(IOException.class, input::openStream);
        assertEquals("boom", exception.getMessage());
    }
}
