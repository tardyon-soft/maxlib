package ru.tardyon.botframework.quarkus.webhook;

import io.quarkus.runtime.Startup;
import jakarta.inject.Singleton;
import jakarta.inject.Inject;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.concurrent.CompletionStage;
import org.eclipse.microprofile.config.Config;
import ru.tardyon.botframework.ingestion.WebhookReceiveResult;
import ru.tardyon.botframework.ingestion.WebhookReceiveStatus;

/**
 * Quarkus HTTP endpoint that bridges webhook requests to framework-agnostic webhook receiver.
 */
@Path("${max.bot.webhook.path:/webhook/max}")
@Singleton
@Startup
public class QuarkusWebhookResource {
    @Inject
    Instance<QuarkusWebhookAdapter> adapterProvider;

    @Inject
    Config config;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public CompletionStage<Response> handle(byte[] body, HttpHeaders headers) {
        if (!webhookEnabled() || adapterProvider == null || !adapterProvider.isResolvable()) {
            return java.util.concurrent.CompletableFuture.completedFuture(Response.status(Response.Status.NOT_FOUND).build());
        }
        return adapterProvider.get().receive(body, headers).thenApply(this::mapResponse);
    }

    private Response mapResponse(WebhookReceiveResult result) {
        WebhookReceiveStatus status = result.status();
        return switch (status) {
            case ACCEPTED -> Response.ok().build();
            case INVALID_SECRET -> Response.status(Response.Status.FORBIDDEN).build();
            case BAD_PAYLOAD -> Response.status(Response.Status.BAD_REQUEST).build();
            case OVERLOADED -> Response.status(Response.Status.TOO_MANY_REQUESTS).build();
            case INTERNAL_ERROR -> Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        };
    }

    private boolean webhookEnabled() {
        String mode = config.getOptionalValue("max.bot.mode", String.class).orElse("POLLING");
        boolean enabled = config.getOptionalValue("max.bot.webhook.enabled", Boolean.class).orElse(false);
        return "WEBHOOK".equalsIgnoreCase(mode) || enabled;
    }
}
