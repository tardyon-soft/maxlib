package ru.max.botframework.upload;

import java.util.concurrent.CompletionStage;

/**
 * Raw HTTP upload transfer gateway for prepared upload URL.
 */
@FunctionalInterface
public interface UploadTransferGateway {
    CompletionStage<UploadTransferReceipt> transfer(UploadPreparation preparation, InputFile inputFile);
}
