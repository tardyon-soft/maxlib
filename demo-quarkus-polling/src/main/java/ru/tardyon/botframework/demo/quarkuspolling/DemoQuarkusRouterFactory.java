package ru.tardyon.botframework.demo.quarkuspolling;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import ru.tardyon.botframework.demo.springpolling.ApiSmokeService;
import ru.tardyon.botframework.demo.springpolling.ApiSmokeRoute;
import ru.tardyon.botframework.demo.springpolling.BackgroundTaskMonitorService;
import ru.tardyon.botframework.demo.springpolling.DemoRouterSupport;
import ru.tardyon.botframework.client.MaxBotClient;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.screen.ScreenRegistry;

@ApplicationScoped
public class DemoQuarkusRouterFactory {
    @Produces
    @Singleton
    Router demoRouterMax(ScreenRegistry screenRegistry) {
        return DemoRouterSupport.buildRouter(screenRegistry);
    }

    @Produces
    @Singleton
    BackgroundTaskMonitorService backgroundTaskMonitorService() {
        return new BackgroundTaskMonitorService();
    }

    @Produces
    @Singleton
    ApiSmokeService apiSmokeService(
            MaxBotClient client,
            @ConfigProperty(name = "demo.smoke.destructive", defaultValue = "false") boolean destructive,
            @ConfigProperty(name = "demo.smoke.webhook-url") Optional<String> webhookUrl
    ) {
        return new ApiSmokeService(client, destructive, webhookUrl.orElse(""));
    }

    @Produces
    @Singleton
    ApiSmokeRoute apiSmokeRoute(ApiSmokeService smokeService) {
        return new ApiSmokeRoute(smokeService);
    }
}
