package ru.tardyon.botframework.spring.webhook;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import ru.tardyon.botframework.ingestion.WebhookReceiveResult;
import ru.tardyon.botframework.ingestion.WebhookReceiveStatus;

/**
 * Spring MVC endpoint that bridges webhook HTTP requests to framework-agnostic webhook receiver.
 */
@RestController
public final class SpringWebhookController {
    private static final Logger log = LoggerFactory.getLogger(SpringWebhookController.class);
    private final SpringWebhookAdapter adapter;

    public SpringWebhookController(SpringWebhookAdapter adapter) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
    }

    @PostMapping(
            path = "${max.bot.webhook.path:/webhook/max}",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public CompletionStage<ResponseEntity<Void>> handle(
            @RequestBody byte[] body,
            @RequestHeader org.springframework.http.HttpHeaders headers
    ) {
        log.debug("Webhook HTTP request received: bodyBytes={}, headerCount={}", body.length, headers.size());
        return adapter.receive(body, headers).thenApply(this::mapResponse);
    }

    private ResponseEntity<Void> mapResponse(WebhookReceiveResult result) {
        WebhookReceiveStatus status = result.status();
        log.debug("Webhook HTTP response mapped: status={}", status);
        return switch (status) {
            case ACCEPTED -> ResponseEntity.ok().build();
            case INVALID_SECRET -> ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            case BAD_PAYLOAD -> ResponseEntity.badRequest().build();
            case OVERLOADED -> ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
            case INTERNAL_ERROR -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        };
    }
}
