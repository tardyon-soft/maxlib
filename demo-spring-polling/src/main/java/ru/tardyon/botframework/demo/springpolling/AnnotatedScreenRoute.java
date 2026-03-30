package ru.tardyon.botframework.demo.springpolling;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.dispatcher.RuntimeContext;
import ru.tardyon.botframework.dispatcher.annotation.Command;
import ru.tardyon.botframework.dispatcher.annotation.Route;
import ru.tardyon.botframework.screen.ScreenRegistry;
import ru.tardyon.botframework.screen.Screens;

/**
 * Demo route that starts annotation-driven screens.
 */
@Route(value = "annotated-screen-route", autoRegister = true)
public final class AnnotatedScreenRoute {

    @Command("ascreen")
    public CompletionStage<Void> start(RuntimeContext context, ScreenRegistry screenRegistry) {
        return Screens.navigator(context, screenRegistry).start("annotated.home", Map.of());
    }

    @Command("cscreen")
    public CompletionStage<Void> startFacade(RuntimeContext context, ScreenRegistry screenRegistry) {
        return Screens.navigator(context, screenRegistry).start("facade.home", Map.of());
    }
}
