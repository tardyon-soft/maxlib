package ru.tardyon.botframework.examples.sprint9;

import java.util.concurrent.CompletableFuture;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.message.Messages;

/**
 * Minimal Spring Boot bot in webhook mode.
 *
 * <p>Use with application-webhook.yml. Endpoint is auto-configured by starter
 * (default path: /webhook/max).</p>
 */
@SpringBootApplication
public class SpringWebhookBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringWebhookBotApplication.class, args);
    }

    @Bean
    Router webhookRouter() {
        Router router = new Router("webhook-main");

        router.message((message, ctx) -> {
            ctx.reply(Messages.text("Webhook received: " + message.text()));
            return CompletableFuture.completedFuture(null);
        });

        router.callback((callback, ctx) -> {
            ctx.answerCallback("OK");
            return CompletableFuture.completedFuture(null);
        });

        return router;
    }
}
