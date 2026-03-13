package ru.tardyon.botframework.upload;

import java.util.concurrent.CompletionStage;

/**
 * Gateway for upload preparation step backed by MAX API {@code POST /uploads}.
 */
@FunctionalInterface
public interface UploadPreparationGateway {
    CompletionStage<UploadPreparation> prepare(UploadPrepareCommand command);
}
