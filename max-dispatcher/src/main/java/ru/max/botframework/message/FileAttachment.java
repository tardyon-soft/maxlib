package ru.max.botframework.message;

import java.util.Objects;
import ru.max.botframework.model.MessageAttachmentType;
import ru.max.botframework.model.request.AttachmentInput;
import ru.max.botframework.model.request.NewMessageAttachment;
import ru.max.botframework.upload.UploadMediaKind;
import ru.max.botframework.upload.UploadResult;

/**
 * High-level file attachment.
 */
public record FileAttachment(UploadResult uploadResult, String caption) implements MediaAttachment {

    public FileAttachment {
        Objects.requireNonNull(uploadResult, "uploadResult");
        ImageAttachment.requireCompatible(uploadResult.mediaKind(), UploadMediaKind.FILE);
        if (caption != null && caption.isBlank()) {
            throw new IllegalArgumentException("caption must not be blank");
        }
    }

    public static FileAttachment from(UploadResult uploadResult) {
        return new FileAttachment(uploadResult, null);
    }

    public FileAttachment caption(String value) {
        return new FileAttachment(uploadResult, value);
    }

    @Override
    public UploadKind kind() {
        return UploadKind.FILE;
    }

    @Override
    public NewMessageAttachment toNewMessageAttachment() {
        return NewMessageAttachment.media(
                MessageAttachmentType.FILE,
                new AttachmentInput(null, uploadResult.ref().value(), null),
                caption,
                uploadResult.contentTypeOptional().orElse(null),
                uploadResult.bytesTransferred()
        );
    }
}
