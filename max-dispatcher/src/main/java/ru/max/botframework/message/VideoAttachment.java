package ru.max.botframework.message;

import java.util.Objects;
import ru.max.botframework.model.MessageAttachmentType;
import ru.max.botframework.model.request.AttachmentInput;
import ru.max.botframework.model.request.NewMessageAttachment;
import ru.max.botframework.upload.UploadMediaKind;
import ru.max.botframework.upload.UploadResult;

/**
 * High-level video attachment.
 */
public record VideoAttachment(UploadResult uploadResult, String caption) implements MediaAttachment {

    public VideoAttachment {
        Objects.requireNonNull(uploadResult, "uploadResult");
        ImageAttachment.requireCompatible(uploadResult.mediaKind(), UploadMediaKind.VIDEO);
        if (caption != null && caption.isBlank()) {
            throw new IllegalArgumentException("caption must not be blank");
        }
    }

    public static VideoAttachment from(UploadResult uploadResult) {
        return new VideoAttachment(uploadResult, null);
    }

    public VideoAttachment caption(String value) {
        return new VideoAttachment(uploadResult, value);
    }

    @Override
    public UploadKind kind() {
        return UploadKind.VIDEO;
    }

    @Override
    public NewMessageAttachment toNewMessageAttachment() {
        String tokenAwareRef = uploadResult.mediaTokenOptional().orElse(uploadResult.ref().value());
        return NewMessageAttachment.media(
                MessageAttachmentType.VIDEO,
                new AttachmentInput(null, tokenAwareRef, null),
                caption,
                uploadResult.contentTypeOptional().orElse(null),
                uploadResult.bytesTransferred()
        );
    }
}
