package ru.tardyon.botframework.ingestion;

import java.util.Objects;
import java.util.Optional;

/**
 * Minimal ingestion-level handling outcome.
 */
public final class UpdateHandlingResult {
    private static final UpdateHandlingResult SUCCESS = new UpdateHandlingResult(UpdateHandlingStatus.SUCCESS, null);

    private final UpdateHandlingStatus status;
    private final Throwable error;

    private UpdateHandlingResult(UpdateHandlingStatus status, Throwable error) {
        this.status = Objects.requireNonNull(status, "status");
        this.error = error;
    }

    public static UpdateHandlingResult success() {
        return SUCCESS;
    }

    public static UpdateHandlingResult failure(Throwable error) {
        return new UpdateHandlingResult(UpdateHandlingStatus.FAILURE, Objects.requireNonNull(error, "error"));
    }

    public UpdateHandlingStatus status() {
        return status;
    }

    public boolean isSuccess() {
        return status == UpdateHandlingStatus.SUCCESS;
    }

    public Optional<Throwable> error() {
        return Optional.ofNullable(error);
    }
}
