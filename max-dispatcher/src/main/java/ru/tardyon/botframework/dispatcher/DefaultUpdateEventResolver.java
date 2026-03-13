package ru.tardyon.botframework.dispatcher;

import java.util.Objects;
import ru.tardyon.botframework.model.Update;
import ru.tardyon.botframework.model.UpdateType;

/**
 * Default resolver for Sprint 3 observer mapping.
 *
 * <p>Strategy:
 * 1) Prefer explicit {@link UpdateType} when matching payload exists.
 * 2) Fallback to payload presence (message/callback) for unknown/misaligned type.
 * 3) Otherwise mark as unsupported.
 * </p>
 */
public final class DefaultUpdateEventResolver implements UpdateEventResolver {

    @Override
    public UpdateEventResolution resolve(Update update) {
        Update source = Objects.requireNonNull(update, "update");

        if (source.type() == UpdateType.MESSAGE && source.message() != null) {
            return UpdateEventResolution.message();
        }
        if (source.type() == UpdateType.CALLBACK && source.callback() != null) {
            return UpdateEventResolution.callback();
        }

        if (source.message() != null) {
            return UpdateEventResolution.message();
        }
        if (source.callback() != null) {
            return UpdateEventResolution.callback();
        }

        return UpdateEventResolution.unsupported();
    }
}

