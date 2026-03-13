package ru.tardyon.botframework.examples.sprint9;

import java.util.concurrent.CompletableFuture;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import ru.tardyon.botframework.dispatcher.BuiltInFilters;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.message.Messages;

/**
 * Minimal Spring Boot bot in polling mode.
 *
 * <p>Use with application-polling.yml.</p>
 */
@SpringBootApplication
public class SpringPollingBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringPollingBotApplication.class, args);
    }

    @Bean
    Router pollingRouter() {
        Router router = new Router("polling-main");

        router.message(BuiltInFilters.command("start"), (message, ctx) -> {
            ctx.reply(Messages.text("Привет из polling mode"));
            return CompletableFuture.completedFuture(null);
        });

        router.message((message, ctx) -> {
            ctx.reply(Messages.text("Echo: " + message.text()));
            return CompletableFuture.completedFuture(null);
        });

        return router;
    }
}
