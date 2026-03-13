package ru.max.botframework.upload;

import java.util.Objects;
import java.util.concurrent.CompletionStage;

/**
 * Upload orchestration contract.
 *
 * <p>Public layer boundary for upload/media pipeline:
 * callers provide {@link InputFile} and receive normalized {@link UploadResult},
 * while transport details stay behind preparation/transfer/finalize gateways.</p>
 */
public interface UploadService {

    /**
     * Executes prepare -> transfer -> finalize orchestration with default request options.
     */
    default CompletionStage<UploadResult> upload(InputFile inputFile) {
        return upload(inputFile, UploadRequest.defaults());
    }

    /**
     * Executes prepare -> transfer -> finalize orchestration.
     */
    CompletionStage<UploadResult> upload(InputFile inputFile, UploadRequest request);

    static UploadService of(
            UploadPreparationGateway preparationGateway,
            UploadTransferGateway transferGateway,
            UploadFinalizeGateway finalizeGateway
    ) {
        return new DefaultUploadService(
                Objects.requireNonNull(preparationGateway, "preparationGateway"),
                Objects.requireNonNull(transferGateway, "transferGateway"),
                Objects.requireNonNull(finalizeGateway, "finalizeGateway"),
                new DefaultUploadResultMapper()
        );
    }
}
