package ru.max.botframework.upload;

import java.util.Objects;
import java.util.concurrent.CompletionStage;

/**
 * Default staged upload orchestration implementation.
 */
public final class DefaultUploadService implements UploadService {
    private final UploadPreparationGateway preparationGateway;
    private final UploadTransferGateway transferGateway;
    private final UploadFinalizeGateway finalizeGateway;
    private final UploadResultMapper resultMapper;

    public DefaultUploadService(
            UploadPreparationGateway preparationGateway,
            UploadTransferGateway transferGateway,
            UploadFinalizeGateway finalizeGateway,
            UploadResultMapper resultMapper
    ) {
        this.preparationGateway = Objects.requireNonNull(preparationGateway, "preparationGateway");
        this.transferGateway = Objects.requireNonNull(transferGateway, "transferGateway");
        this.finalizeGateway = Objects.requireNonNull(finalizeGateway, "finalizeGateway");
        this.resultMapper = Objects.requireNonNull(resultMapper, "resultMapper");
    }

    @Override
    public CompletionStage<UploadResult> upload(InputFile inputFile, UploadRequest request) {
        Objects.requireNonNull(inputFile, "inputFile");
        Objects.requireNonNull(request, "request");

        UploadPrepareCommand command = new UploadPrepareCommand(
                inputFile.fileName(),
                inputFile.contentType().orElse(null),
                inputFile.knownSize().isPresent() ? inputFile.knownSize().getAsLong() : null,
                request.preferredFlowType(),
                request.mediaTypeHint()
        );

        return preparationGateway.prepare(command)
                .thenCompose(preparation -> transferGateway.transfer(preparation, inputFile)
                        .thenCompose(receipt -> finalizeGateway.finalizeUpload(preparation, receipt)
                                .thenApply(finalize -> resultMapper.map(preparation, receipt, finalize))));
    }
}
