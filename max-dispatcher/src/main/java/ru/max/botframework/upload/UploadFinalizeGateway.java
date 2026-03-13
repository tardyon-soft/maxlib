package ru.max.botframework.upload;

import java.util.concurrent.CompletionStage;

/**
 * Gateway for upload finalization stage.
 */
@FunctionalInterface
public interface UploadFinalizeGateway {
    CompletionStage<UploadFinalizeResult> finalizeUpload(UploadPreparation preparation, UploadTransferReceipt receipt);
}
