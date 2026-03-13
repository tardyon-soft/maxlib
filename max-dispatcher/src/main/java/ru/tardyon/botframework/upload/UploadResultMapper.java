package ru.tardyon.botframework.upload;

/**
 * Maps upload stage results into attachment-ready upload result model.
 */
@FunctionalInterface
public interface UploadResultMapper {
    UploadResult map(UploadPreparation preparation, UploadTransferReceipt receipt, UploadFinalizeResult finalizeResult);
}
