package ru.tardyon.botframework.demo.micronautpolling;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.runtime.Micronaut;
import jakarta.inject.Singleton;
import ru.tardyon.botframework.client.MaxBotClient;
import ru.tardyon.botframework.demo.springpolling.ApiSmokeRoute;
import ru.tardyon.botframework.demo.springpolling.ApiSmokeService;
import ru.tardyon.botframework.demo.springpolling.BackgroundTaskMonitorService;
import ru.tardyon.botframework.demo.springpolling.DemoRouterSupport;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.screen.ScreenRegistry;

@Factory
public final class DemoMicronautPollingApplication {
    public static void main(String[] args) {
        Micronaut.run(DemoMicronautPollingApplication.class, args);
    }

    @Singleton
    Router demoRouterMax(ScreenRegistry screenRegistry) {
        return DemoRouterSupport.buildRouter(screenRegistry);
    }

    @Singleton
    BackgroundTaskMonitorService backgroundTaskMonitorService() {
        return new BackgroundTaskMonitorService();
    }

    @Singleton
    ApiSmokeService apiSmokeService(
            MaxBotClient client,
            @Property(name = "demo.smoke.destructive", defaultValue = "false") boolean destructive,
            @Property(name = "demo.smoke.webhook-url", defaultValue = "") String webhookUrl
    ) {
        return new ApiSmokeService(client, destructive, webhookUrl);
    }

    @Singleton
    ApiSmokeRoute apiSmokeRoute(ApiSmokeService smokeService) {
        return new ApiSmokeRoute(smokeService);
    }
}
