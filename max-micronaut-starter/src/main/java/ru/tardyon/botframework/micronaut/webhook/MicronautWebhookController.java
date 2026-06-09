package ru.tardyon.botframework.micronaut.webhook;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.ingestion.WebhookReceiveResult;
import ru.tardyon.botframework.ingestion.WebhookReceiveStatus;
import ru.tardyon.botframework.micronaut.autoconfigure.WebhookModeOrEnabledCondition;

/**
 * Micronaut HTTP endpoint that bridges webhook requests to framework-agnostic webhook receiver.
 */
@Controller("${max.bot.webhook.path:/webhook/max}")
@Requires(condition = WebhookModeOrEnabledCondition.class)
@Requires(beans = MicronautWebhookAdapter.class)
public final class MicronautWebhookController {
    private final MicronautWebhookAdapter adapter;

    public MicronautWebhookController(MicronautWebhookAdapter adapter) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
    }

    @Post(consumes = MediaType.APPLICATION_JSON)
    public CompletionStage<HttpResponse<Void>> handle(
            @Body byte[] body,
            HttpHeaders headers
    ) {
        return adapter.receive(body, headers).thenApply(this::mapResponse);
    }

    private HttpResponse<Void> mapResponse(WebhookReceiveResult result) {
        WebhookReceiveStatus status = result.status();
        return switch (status) {
            case ACCEPTED -> HttpResponse.ok();
            case INVALID_SECRET -> HttpResponse.status(HttpStatus.FORBIDDEN);
            case BAD_PAYLOAD -> HttpResponse.badRequest();
            case OVERLOADED -> HttpResponse.status(HttpStatus.TOO_MANY_REQUESTS);
            case INTERNAL_ERROR -> HttpResponse.status(HttpStatus.INTERNAL_SERVER_ERROR);
        };
    }
}
