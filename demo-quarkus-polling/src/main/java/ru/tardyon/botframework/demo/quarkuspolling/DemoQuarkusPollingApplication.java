package ru.tardyon.botframework.demo.quarkuspolling;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public final class DemoQuarkusPollingApplication {
    public static void main(String... args) {
        Quarkus.run(Application.class, args);
    }

    public static final class Application implements QuarkusApplication {
        @Override
        public int run(String... args) {
            Quarkus.waitForExit();
            return 0;
        }
    }
}
