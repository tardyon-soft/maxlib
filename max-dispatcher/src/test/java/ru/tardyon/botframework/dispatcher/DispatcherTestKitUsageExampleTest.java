package ru.tardyon.botframework.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.message.Messages;
import ru.tardyon.botframework.testkit.DispatcherTestKit;
import ru.tardyon.botframework.testkit.TestUpdates;

class DispatcherTestKitUsageExampleTest {

    @Test
    void testkitCanDriveRuntimeAndAssertSideEffects() {
        Router router = new Router("example");
        router.message((message, context) -> {
            context.reply(Messages.text("pong"));
            return CompletableFuture.completedFuture(null);
        });

        DispatcherTestKit kit = DispatcherTestKit.withRouter(router);
        DispatcherTestKit.DispatchProbe probe = kit.feedAndCapture(TestUpdates.message("ping"));

        assertEquals(DispatchStatus.HANDLED, probe.result().status());
        assertEquals(1, probe.callsTo("/messages").size());
    }
}
