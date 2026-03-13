package ru.max.botframework.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.max.botframework.model.MessageAttachmentType;
import ru.max.botframework.upload.UploadFlowType;
import ru.max.botframework.upload.UploadMediaKind;
import ru.max.botframework.upload.UploadRef;
import ru.max.botframework.upload.UploadResult;

class MediaAttachmentTest {

    @Test
    void mapsImageAttachmentToLowLevelPhotoAttachment() {
        UploadResult upload = new UploadResult(
                new UploadRef("ref-image"),
                UploadFlowType.MULTIPART,
                100L,
                "image/png",
                UploadMediaKind.IMAGE,
                Map.of("width", "800")
        );

        var lowLevel = ImageAttachment.from(upload)
                .caption("photo")
                .toNewMessageAttachment();

        assertEquals(MessageAttachmentType.PHOTO, lowLevel.type());
        assertEquals("ref-image", lowLevel.input().uploadRef());
        assertEquals("photo", lowLevel.caption());
        assertEquals("image/png", lowLevel.mimeType());
        assertEquals(100L, lowLevel.size());
    }

    @Test
    void mapsFileAttachmentToLowLevelFileAttachment() {
        UploadResult upload = new UploadResult(
                new UploadRef("ref-file"),
                UploadFlowType.RESUMABLE,
                256L,
                "application/pdf",
                UploadMediaKind.FILE,
                Map.of("name", "doc.pdf")
        );

        var lowLevel = FileAttachment.from(upload)
                .caption("doc")
                .toNewMessageAttachment();

        assertEquals(MessageAttachmentType.FILE, lowLevel.type());
        assertEquals("ref-file", lowLevel.input().uploadRef());
        assertEquals("doc", lowLevel.caption());
        assertEquals("application/pdf", lowLevel.mimeType());
    }

    @Test
    void mapsVideoAndAudioAttachments() {
        UploadResult video = new UploadResult(
                new UploadRef("ref-video"),
                UploadFlowType.RESUMABLE,
                1000L,
                "video/mp4",
                UploadMediaKind.VIDEO,
                Map.of("duration", "12")
        );
        UploadResult audio = new UploadResult(
                new UploadRef("ref-audio"),
                UploadFlowType.MULTIPART,
                500L,
                "audio/mpeg",
                UploadMediaKind.AUDIO,
                Map.of()
        );

        assertEquals(MessageAttachmentType.VIDEO, VideoAttachment.from(video).toNewMessageAttachment().type());
        assertEquals(MessageAttachmentType.AUDIO, AudioAttachment.from(audio).toNewMessageAttachment().type());
    }

    @Test
    void supportsUnknownUploadMediaKind() {
        UploadResult upload = new UploadResult(
                new UploadRef("ref-unknown"),
                UploadFlowType.MULTIPART,
                10L,
                null,
                UploadMediaKind.UNKNOWN,
                Map.of()
        );

        assertEquals(MessageAttachmentType.PHOTO, ImageAttachment.from(upload).toNewMessageAttachment().type());
        assertEquals(MessageAttachmentType.FILE, FileAttachment.from(upload).toNewMessageAttachment().type());
    }

    @Test
    void rejectsIncompatibleUploadMediaKind() {
        UploadResult upload = new UploadResult(
                new UploadRef("ref-wrong"),
                UploadFlowType.MULTIPART,
                10L,
                "video/mp4",
                UploadMediaKind.VIDEO,
                Map.of()
        );

        assertThrows(IllegalArgumentException.class, () -> ImageAttachment.from(upload));
    }

    @Test
    void messageBuilderAcceptsHighLevelMediaAttachment() {
        UploadResult upload = new UploadResult(
                new UploadRef("ref-bld"),
                UploadFlowType.MULTIPART,
                20L,
                "image/jpeg",
                UploadMediaKind.IMAGE,
                Map.of()
        );

        var body = Messages.text("with image")
                .attachment(MediaAttachment.image(upload).caption("preview"))
                .toNewMessageBody();

        assertEquals(1, body.attachments().size());
        assertEquals(MessageAttachmentType.PHOTO, body.attachments().getFirst().type());
        assertEquals("ref-bld", body.attachments().getFirst().input().uploadRef());
    }
}
