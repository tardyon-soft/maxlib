package ru.tardyon.botframework.upload;

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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InputFileTest {

    @TempDir
    Path tempDir;

    @Test
    void createsFromPathWithKnownSizeAndReadableStream() throws Exception {
        Path file = tempDir.resolve("invoice.txt");
        Files.writeString(file, "invoice", StandardCharsets.UTF_8);

        InputFile input = new InputFile.PathInputFile(
                file.toAbsolutePath().normalize(),
                "invoice.txt",
                Optional.empty(),
                7L
        );

        assertEquals("invoice.txt", input.fileName());
        assertTrue(input.knownSize().isPresent());
        assertEquals(7L, input.knownSize().getAsLong());
        assertFalse(input.contentType().isPresent());
        assertEquals("invoice", new String(input.openStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void createsFromBytesWithKnownSizeAndDefensiveCopy() throws Exception {
        byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);
        InputFile input = new InputFile.BytesInputFile(bytes, "hello.txt", Optional.empty());
        bytes[0] = 'X';

        assertEquals("hello.txt", input.fileName());
        assertTrue(input.knownSize().isPresent());
        assertEquals(5L, input.knownSize().getAsLong());
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), input.openStream().readAllBytes());
    }

    @Test
    void createsFromStreamWithUnknownAndKnownSize() throws Exception {
        InputFile unknown = new InputFile.StreamInputFile(
                () -> new ByteArrayInputStream("stream".getBytes(StandardCharsets.UTF_8)),
                "stream.bin",
                Optional.empty(),
                null
        );
        InputFile known = new InputFile.StreamInputFile(
                () -> new ByteArrayInputStream("stream".getBytes(StandardCharsets.UTF_8)),
                "stream.bin",
                Optional.empty(),
                6L
        );

        assertFalse(unknown.knownSize().isPresent());
        assertTrue(known.knownSize().isPresent());
        assertEquals(6L, known.knownSize().getAsLong());
        assertArrayEquals("stream".getBytes(StandardCharsets.UTF_8), unknown.openStream().readAllBytes());
    }

    @Test
    void supportsFileNameAndContentTypeOverrides() {
        InputFile input = new InputFile.BytesInputFile("x".getBytes(StandardCharsets.UTF_8), "a.txt", Optional.empty())
                .withFileName("b.txt")
                .withContentType("text/plain");

        assertEquals("b.txt", input.fileName());
        assertEquals("text/plain", input.contentType().orElseThrow());
    }

    @Test
    void validatesFactoryArguments() {
        assertThrows(IllegalArgumentException.class, () -> new InputFile.BytesInputFile(new byte[]{1}, " ", Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new InputFile.StreamInputFile(
                () -> new ByteArrayInputStream(new byte[0]), " ", Optional.empty(), 1L
        ));
        assertThrows(IllegalArgumentException.class, () -> new InputFile.StreamInputFile(
                () -> new ByteArrayInputStream(new byte[0]), "file.bin", Optional.empty(), -1L
        ));
        assertThrows(NullPointerException.class, () -> InputFile.fromBytes(new byte[]{1}, "a.bin"));
        assertThrows(IllegalArgumentException.class, () -> InputFile.fromPath(tempDir.resolve("missing.bin")));
        assertThrows(
                IllegalArgumentException.class,
                () -> new InputFile.BytesInputFile(new byte[]{1}, "a", Optional.empty()).withContentType(" ")
        );
    }

    @Test
    void propagatesStreamSupplierIOException() {
        InputFile input = new InputFile.StreamInputFile(() -> {
            throw new IOException("boom");
        }, "f.bin", Optional.empty(), null);

        IOException exception = assertThrows(IOException.class, input::openStream);
        assertEquals("boom", exception.getMessage());
    }
}
