package ru.max.botframework.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class UploadResultTokenTest {

    @Test
    void resolvesVideoTokenFromPayload() {
        UploadResult result = new UploadResult(
                new UploadRef("ref-video"),
                UploadFlowType.RESUMABLE,
                1,
                "video/mp4",
                UploadMediaKind.VIDEO,
                Map.of(UploadPayloadKeys.VIDEO_TOKEN, "video-token")
        );

        assertEquals("video-token", result.mediaTokenOptional().orElseThrow());
    }

    @Test
    void resolvesAudioTokenFromGenericTokenFallback() {
        UploadResult result = new UploadResult(
                new UploadRef("ref-audio"),
                UploadFlowType.RESUMABLE,
                1,
                "audio/mpeg",
                UploadMediaKind.AUDIO,
                Map.of(UploadPayloadKeys.TOKEN, "audio-token")
        );

        assertEquals("audio-token", result.mediaTokenOptional().orElseThrow());
    }

    @Test
    void noMediaTokenForImageKind() {
        UploadResult result = new UploadResult(
                new UploadRef("ref-image"),
                UploadFlowType.MULTIPART,
                1,
                "image/png",
                UploadMediaKind.IMAGE,
                Map.of(UploadPayloadKeys.TOKEN, "ignored")
        );

        assertTrue(result.mediaTokenOptional().isEmpty());
    }
}
