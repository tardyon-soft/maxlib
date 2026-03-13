package ru.tardyon.botframework.message;

import ru.tardyon.botframework.model.request.NewMessageAttachment;
import ru.tardyon.botframework.upload.UploadResult;

/**
 * High-level media attachment contract built from upload result model.
 */
public sealed interface MediaAttachment permits ImageAttachment, FileAttachment, VideoAttachment, AudioAttachment {

    UploadResult uploadResult();

    UploadKind kind();

    NewMessageAttachment toNewMessageAttachment();

    enum UploadKind {
        IMAGE,
        FILE,
        VIDEO,
        AUDIO
    }

    static ImageAttachment image(UploadResult uploadResult) {
        return ImageAttachment.from(uploadResult);
    }

    static FileAttachment file(UploadResult uploadResult) {
        return FileAttachment.from(uploadResult);
    }

    static VideoAttachment video(UploadResult uploadResult) {
        return VideoAttachment.from(uploadResult);
    }

    static AudioAttachment audio(UploadResult uploadResult) {
        return AudioAttachment.from(uploadResult);
    }
}
