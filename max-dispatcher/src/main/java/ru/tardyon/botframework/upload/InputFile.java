package ru.tardyon.botframework.upload;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Unified media input abstraction for upload pipeline.
 *
 * <p>{@code InputFile} represents source data and metadata without binding to specific upload mode
 * (multipart/resumable).</p>
 */
public sealed interface InputFile permits InputFile.PathInputFile, InputFile.BytesInputFile, InputFile.StreamInputFile {

    /**
     * Creates input file from local filesystem path.
     */
    static InputFile fromPath(Path path) {
        Objects.requireNonNull(path, "path");
        Path normalized = path.toAbsolutePath().normalize();
        if (!Files.exists(normalized) || !Files.isRegularFile(normalized)) {
            throw new IllegalArgumentException("path must point to existing regular file");
        }
        String fileName = normalized.getFileName() == null ? normalized.toString() : normalized.getFileName().toString();
        long size;
        try {
            size = Files.size(normalized);
        } catch (IOException e) {
            throw new IllegalArgumentException("failed to resolve file size", e);
        }
        return new PathInputFile(normalized, fileName, null, size);
    }

    /**
     * Creates in-memory input file.
     */
    static InputFile fromBytes(byte[] bytes, String fileName) {
        Objects.requireNonNull(bytes, "bytes");
        requireFileName(fileName);
        return new BytesInputFile(bytes.clone(), fileName, null);
    }

    /**
     * Creates stream-based input file with unknown size.
     */
    static InputFile fromStream(StreamSupplier streamSupplier, String fileName) {
        return fromStream(streamSupplier, fileName, null);
    }

    /**
     * Creates stream-based input file with known size.
     */
    static InputFile fromStream(StreamSupplier streamSupplier, String fileName, Long knownSize) {
        Objects.requireNonNull(streamSupplier, "streamSupplier");
        requireFileName(fileName);
        if (knownSize != null && knownSize < 0) {
            throw new IllegalArgumentException("knownSize must be non-negative");
        }
        return new StreamInputFile(streamSupplier, fileName, null, knownSize);
    }

    String fileName();

    Optional<String> contentType();

    OptionalLong knownSize();

    /**
     * Opens new stream for reading source data.
     */
    InputStream openStream() throws IOException;

    /**
     * Returns copy with overridden file name.
     */
    InputFile withFileName(String fileName);

    /**
     * Returns copy with explicit content type.
     */
    InputFile withContentType(String contentType);

    @FunctionalInterface
    interface StreamSupplier {
        InputStream openStream() throws IOException;
    }

    record PathInputFile(
            Path path,
            String fileName,
            String contentType,
            long size
    ) implements InputFile {
        PathInputFile {
            Objects.requireNonNull(path, "path");
            requireFileName(fileName);
            requireOptionalContentType(contentType);
            if (size < 0) {
                throw new IllegalArgumentException("size must be non-negative");
            }
        }

        @Override
        public Optional<String> contentType() {
            return Optional.ofNullable(contentType);
        }

        @Override
        public OptionalLong knownSize() {
            return OptionalLong.of(size);
        }

        @Override
        public InputStream openStream() throws IOException {
            return Files.newInputStream(path);
        }

        @Override
        public InputFile withFileName(String fileName) {
            return new PathInputFile(path, fileName, contentType, size);
        }

        @Override
        public InputFile withContentType(String contentType) {
            return new PathInputFile(path, fileName, contentType, size);
        }
    }

    record BytesInputFile(
            byte[] bytes,
            String fileName,
            String contentType
    ) implements InputFile {
        BytesInputFile {
            Objects.requireNonNull(bytes, "bytes");
            requireFileName(fileName);
            requireOptionalContentType(contentType);
            bytes = bytes.clone();
        }

        @Override
        public Optional<String> contentType() {
            return Optional.ofNullable(contentType);
        }

        @Override
        public OptionalLong knownSize() {
            return OptionalLong.of(bytes.length);
        }

        @Override
        public InputStream openStream() {
            return new ByteArrayInputStream(bytes.clone());
        }

        @Override
        public InputFile withFileName(String fileName) {
            return new BytesInputFile(bytes, fileName, contentType);
        }

        @Override
        public InputFile withContentType(String contentType) {
            return new BytesInputFile(bytes, fileName, contentType);
        }
    }

    record StreamInputFile(
            StreamSupplier streamSupplier,
            String fileName,
            String contentType,
            Long knownSizeValue
    ) implements InputFile {
        StreamInputFile {
            Objects.requireNonNull(streamSupplier, "streamSupplier");
            requireFileName(fileName);
            requireOptionalContentType(contentType);
            if (knownSizeValue != null && knownSizeValue < 0) {
                throw new IllegalArgumentException("knownSizeValue must be non-negative");
            }
        }

        @Override
        public Optional<String> contentType() {
            return Optional.ofNullable(contentType);
        }

        @Override
        public OptionalLong knownSize() {
            return knownSizeValue == null ? OptionalLong.empty() : OptionalLong.of(knownSizeValue);
        }

        @Override
        public InputStream openStream() throws IOException {
            return streamSupplier.openStream();
        }

        @Override
        public InputFile withFileName(String fileName) {
            return new StreamInputFile(streamSupplier, fileName, contentType, knownSizeValue);
        }

        @Override
        public InputFile withContentType(String contentType) {
            return new StreamInputFile(streamSupplier, fileName, contentType, knownSizeValue);
        }
    }

    private static void requireFileName(String fileName) {
        Objects.requireNonNull(fileName, "fileName");
        if (fileName.isBlank()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
    }

    private static void requireOptionalContentType(String contentType) {
        if (contentType == null) {
            return;
        }
        if (contentType.isBlank()) {
            throw new IllegalArgumentException("contentType must not be blank");
        }
    }
}
