package ru.tardyon.botframework.upload;

import java.util.Objects;

/**
 * Default mapper from staged upload responses to {@link UploadResult}.
 */
public final class DefaultUploadResultMapper implements UploadResultMapper {
    @Override
    public UploadResult map(
            UploadPreparation preparation,
            UploadTransferReceipt receipt,
            UploadFinalizeResult finalizeResult
    ) {
        Objects.requireNonNull(preparation, "preparation");
        Objects.requireNonNull(receipt, "receipt");
        Objects.requireNonNull(finalizeResult, "finalizeResult");
        return new UploadResult(
                new UploadRef(finalizeResult.uploadRef()),
                preparation.flowType(),
                receipt.bytesTransferred(),
                finalizeResult.contentType(),
                finalizeResult.mediaKind(),
                finalizeResult.attachmentPayload()
        );
    }
}
