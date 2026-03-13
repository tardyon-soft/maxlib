package ru.tardyon.botframework.message;

import java.util.Objects;
import ru.tardyon.botframework.model.MessageAttachmentType;
import ru.tardyon.botframework.model.request.AttachmentInput;
import ru.tardyon.botframework.model.request.NewMessageAttachment;
import ru.tardyon.botframework.upload.UploadMediaKind;
import ru.tardyon.botframework.upload.UploadResult;

/**
 * High-level image attachment.
 */
public record ImageAttachment(UploadResult uploadResult, String caption) implements MediaAttachment {

    public ImageAttachment {
        Objects.requireNonNull(uploadResult, "uploadResult");
        requireCompatible(uploadResult.mediaKind(), UploadMediaKind.IMAGE);
        if (caption != null && caption.isBlank()) {
            throw new IllegalArgumentException("caption must not be blank");
        }
    }

    public static ImageAttachment from(UploadResult uploadResult) {
        return new ImageAttachment(uploadResult, null);
    }

    public ImageAttachment caption(String value) {
        return new ImageAttachment(uploadResult, value);
    }

    @Override
    public UploadKind kind() {
        return UploadKind.IMAGE;
    }

    @Override
    public NewMessageAttachment toNewMessageAttachment() {
        return NewMessageAttachment.media(
                MessageAttachmentType.PHOTO,
                new AttachmentInput(null, uploadResult.ref().value(), null),
                caption,
                uploadResult.contentTypeOptional().orElse(null),
                uploadResult.bytesTransferred()
        );
    }

    static void requireCompatible(UploadMediaKind actual, UploadMediaKind expected) {
        if (actual == UploadMediaKind.UNKNOWN || actual == expected) {
            return;
        }
        throw new IllegalArgumentException("UploadResult mediaKind " + actual + " is incompatible with " + expected + " attachment");
    }
}
