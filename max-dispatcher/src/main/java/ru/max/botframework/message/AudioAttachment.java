package ru.max.botframework.message;

import java.util.Objects;
import ru.max.botframework.model.MessageAttachmentType;
import ru.max.botframework.model.request.AttachmentInput;
import ru.max.botframework.model.request.NewMessageAttachment;
import ru.max.botframework.upload.UploadMediaKind;
import ru.max.botframework.upload.UploadResult;

/**
 * High-level audio attachment.
 */
public record AudioAttachment(UploadResult uploadResult, String caption) implements MediaAttachment {

    public AudioAttachment {
        Objects.requireNonNull(uploadResult, "uploadResult");
        ImageAttachment.requireCompatible(uploadResult.mediaKind(), UploadMediaKind.AUDIO);
        if (caption != null && caption.isBlank()) {
            throw new IllegalArgumentException("caption must not be blank");
        }
    }

    public static AudioAttachment from(UploadResult uploadResult) {
        return new AudioAttachment(uploadResult, null);
    }

    public AudioAttachment caption(String value) {
        return new AudioAttachment(uploadResult, value);
    }

    @Override
    public UploadKind kind() {
        return UploadKind.AUDIO;
    }

    @Override
    public NewMessageAttachment toNewMessageAttachment() {
        String tokenAwareRef = uploadResult.mediaTokenOptional().orElse(uploadResult.ref().value());
        return NewMessageAttachment.media(
                MessageAttachmentType.AUDIO,
                new AttachmentInput(null, tokenAwareRef, null),
                caption,
                uploadResult.contentTypeOptional().orElse(null),
                uploadResult.bytesTransferred()
        );
    }
}
