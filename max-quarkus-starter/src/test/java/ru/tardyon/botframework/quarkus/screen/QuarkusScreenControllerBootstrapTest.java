package ru.tardyon.botframework.quarkus.screen;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import java.util.Optional;
import java.util.Set;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.quarkus.screen.annotation.ScreenController;
import ru.tardyon.botframework.quarkus.screen.annotation.ScreenView;
import ru.tardyon.botframework.screen.InMemoryScreenRegistry;

class QuarkusScreenControllerBootstrapTest {

    @Test
    void invalidControllerBeanFailsDuringStartupRegistration() {
        QuarkusScreenAutoRegistrationBootstrap bootstrap = new QuarkusScreenAutoRegistrationBootstrap();
        bootstrap.registrar = new ru.tardyon.botframework.screen.AnnotatedScreenRegistrar();
        bootstrap.controllerRegistrar = new QuarkusScreenControllerRegistrar();
        bootstrap.beanLookup = unresolvedInstance();
        bootstrap.beanManager = beanManager(new InvalidController());
        bootstrap.config = config(false);

        assertThrows(IllegalStateException.class, () -> bootstrap.registerScreens(new InMemoryScreenRegistry()));
    }

    @ScreenController
    static final class InvalidController {
        @ScreenView(screen = "invalid")
        public String invalid() {
            return "bad";
        }
    }

    @SuppressWarnings("unchecked")
    private static Instance<Object> unresolvedInstance() {
        Instance<Object> provider = mock(Instance.class);
        Instance<Object> selected = mock(Instance.class);
        when(provider.select(org.mockito.ArgumentMatchers.any(Class.class))).thenReturn(selected);
        when(selected.isResolvable()).thenReturn(false);
        return provider;
    }

    @SuppressWarnings("unchecked")
    private static BeanManager beanManager(InvalidController controller) {
        BeanManager beanManager = mock(BeanManager.class);
        @SuppressWarnings("rawtypes")
        Bean bean = mock(Bean.class);
        @SuppressWarnings("rawtypes")
        CreationalContext creationalContext = mock(CreationalContext.class);
        when(beanManager.getBeans(Object.class, Any.Literal.INSTANCE)).thenReturn(Set.of(bean));
        when(bean.getTypes()).thenReturn(Set.of(InvalidController.class));
        when(beanManager.createCreationalContext((Bean) bean)).thenReturn(creationalContext);
        when(beanManager.getReference((Bean) bean, InvalidController.class, creationalContext)).thenReturn(controller);
        return beanManager;
    }

    private static Config config(boolean routeComponentScanEnabled) {
        Config config = mock(Config.class);
        when(config.getOptionalValue("max.bot.route-component-scan.enabled", String.class))
                .thenReturn(Optional.of(Boolean.toString(routeComponentScanEnabled)));
        return config;
    }
}
