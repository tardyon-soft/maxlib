package ru.tardyon.botframework.demo.springpolling;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.dispatcher.Router;

@SpringBootTest(
        classes = DemoSpringPollingApplication.class,
        properties = {
                "max.bot.token=test-token",
                "max.bot.mode=POLLING",
                "max.bot.polling.enabled=false"
        }
)
class DemoSpringPollingApplicationSmokeTest {

    @Autowired
    private Dispatcher dispatcher;

    @Autowired
    private Router demoRouter;

    @Test
    void contextLoads() {
        assertNotNull(dispatcher);
        assertNotNull(demoRouter);
    }
}
