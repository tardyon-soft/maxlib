package ru.tardyon.botframework.quarkus.properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.fsm.StateScope;

class MaxBotPropertiesValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void failsWhenTokenIsMissing() {
        MaxBotProperties properties = new MaxBotProperties();

        Set<ConstraintViolation<MaxBotProperties>> violations = validator.validate(properties);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("token")
                && v.getConstraintDescriptor().getAnnotation() instanceof NotBlank));
    }

    @Test
    void failsWhenTokenIsBlank() {
        MaxBotProperties properties = new MaxBotProperties();
        properties.setToken(" ");

        Set<ConstraintViolation<MaxBotProperties>> violations = validator.validate(properties);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("token")
                && v.getConstraintDescriptor().getAnnotation() instanceof NotBlank));
    }

    @Test
    void failsWhenNumericConstraintsAreInvalid() {
        MaxBotProperties properties = validProperties();
        properties.getPolling().setLimit(0);
        properties.getWebhook().setMaxInFlight(0);

        Set<ConstraintViolation<MaxBotProperties>> violations = validator.validate(properties);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("polling.limit")
                && v.getConstraintDescriptor().getAnnotation() instanceof Positive));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("webhook.maxInFlight")
                && v.getConstraintDescriptor().getAnnotation() instanceof Positive));
    }

    @Test
    void failsWhenNestedObjectsAreInvalid() {
        MaxBotProperties properties = validProperties();
        properties.getStorage().setType(null);
        properties.getStorage().setStateScope(null);
        properties.getScreen().setNamespace(" ");
        properties.getScreen().getCallback().getCodec().setMode(null);

        Set<ConstraintViolation<MaxBotProperties>> violations = validator.validate(properties);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("storage.type")
                && v.getConstraintDescriptor().getAnnotation() instanceof NotNull));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("storage.stateScope")
                && v.getConstraintDescriptor().getAnnotation() instanceof NotNull));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("screen.namespace")
                && v.getConstraintDescriptor().getAnnotation() instanceof NotBlank));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("screen.callback.codec.mode")
                && v.getConstraintDescriptor().getAnnotation() instanceof NotNull));
    }

    private static MaxBotProperties validProperties() {
        MaxBotProperties properties = new MaxBotProperties();
        properties.setToken("test-token");
        properties.setBaseUrl("https://platform-api.max.ru");
        properties.setMode(MaxBotMode.POLLING);
        properties.getPolling().setTimeout(Duration.ofSeconds(30));
        properties.getStorage().setType(MaxBotStorageType.MEMORY);
        properties.getStorage().setStateScope(StateScope.USER_IN_CHAT);
        properties.getStorage().getRedis().setKeyPrefix("max:bot:fsm");
        properties.getScreen().setNamespace("max.screen");
        return properties;
    }
}
