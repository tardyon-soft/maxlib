package ru.max.botframework.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UploadResultMapperTest {

    private final UploadResultMapper mapper = new DefaultUploadResultMapper();

    @Test
    void normalizesMultipartFinalizeResultIntoUnifiedUploadResult() {
        UploadResult result = mapper.map(
                new UploadPreparation(URI.create("https://upload.example.com/m-1"), UploadFlowType.MULTIPART, "m-1"),
                new UploadTransferReceipt(128L, null),
                new UploadFinalizeResult(
                        "ref-m-1",
                        128L,
                        "image/png",
                        UploadMediaKind.IMAGE,
                        Map.of("width", "800", "height", "600")
                )
        );

        assertEquals("ref-m-1", result.ref().value());
        assertEquals(UploadFlowType.MULTIPART, result.flowType());
        assertEquals(UploadMediaKind.IMAGE, result.mediaKind());
        assertEquals("800", result.attachmentPayloadValue("width").orElseThrow());
        assertEquals("600", result.attachmentPayloadValue("height").orElseThrow());
    }

    @Test
    void normalizesResumableFinalizeResultIntoUnifiedUploadResult() {
        UploadResult result = mapper.map(
                new UploadPreparation(URI.create("https://upload.example.com/r-1"), UploadFlowType.RESUMABLE, "r-1"),
                new UploadTransferReceipt(4096L, null),
                new UploadFinalizeResult(
                        "ref-r-1",
                        4096L,
                        "video/mp4",
                        UploadMediaKind.VIDEO,
                        Map.of("durationSeconds", "12", "previewRef", "preview-1")
                )
        );

        assertEquals("ref-r-1", result.ref().value());
        assertEquals(UploadFlowType.RESUMABLE, result.flowType());
        assertEquals(UploadMediaKind.VIDEO, result.mediaKind());
        assertEquals("12", result.attachmentPayloadValue("durationSeconds").orElseThrow());
        assertEquals("preview-1", result.attachmentPayloadValue("previewRef").orElseThrow());
    }

    @Test
    void returnsImmutableAttachmentPayload() {
        UploadResult result = mapper.map(
                new UploadPreparation(URI.create("https://upload.example.com/f-1"), UploadFlowType.MULTIPART, "f-1"),
                new UploadTransferReceipt(12L, null),
                new UploadFinalizeResult(
                        "ref-f-1",
                        12L,
                        "application/pdf",
                        UploadMediaKind.FILE,
                        Map.of("fileName", "contract.pdf")
                )
        );

        assertThrows(UnsupportedOperationException.class, () -> result.attachmentPayload().put("x", "y"));
    }

    @Test
    void usesUnknownMediaKindAndEmptyPayloadByDefault() {
        UploadResult result = mapper.map(
                new UploadPreparation(URI.create("https://upload.example.com/d-1"), UploadFlowType.MULTIPART, "d-1"),
                new UploadTransferReceipt(1L, null),
                new UploadFinalizeResult("ref-d-1", 1L, "application/octet-stream")
        );

        assertEquals(UploadMediaKind.UNKNOWN, result.mediaKind());
        assertEquals(0, result.attachmentPayload().size());
    }
}
