package ru.max.botframework.spring.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import ru.max.botframework.fsm.StateScope;
import ru.max.botframework.model.UpdateEventType;

/**
 * Spring Boot configuration properties for MAX bot runtime starter.
 */
@ConfigurationProperties(prefix = "max.bot")
@Validated
public class MaxBotProperties {
    @NotBlank
    private String token;

    @NotBlank
    private String baseUrl = "https://api.max.ru";

    @NotNull
    private MaxBotMode mode = MaxBotMode.POLLING;

    @Valid
    private final Polling polling = new Polling();

    @Valid
    private final Webhook webhook = new Webhook();

    @Valid
    private final Storage storage = new Storage();

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public MaxBotMode getMode() {
        return mode;
    }

    public void setMode(MaxBotMode mode) {
        this.mode = mode;
    }

    public Polling getPolling() {
        return polling;
    }

    public Webhook getWebhook() {
        return webhook;
    }

    public Storage getStorage() {
        return storage;
    }

    public static final class Polling {
        private boolean enabled = true;
        @Positive
        private Integer limit = 100;
        @NotNull
        private Duration timeout = Duration.ofSeconds(30);
        private final List<UpdateEventType> types = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Integer getLimit() {
            return limit;
        }

        public void setLimit(Integer limit) {
            this.limit = limit;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public List<UpdateEventType> getTypes() {
            return types;
        }
    }

    public static final class Webhook {
        private boolean enabled;

        @NotBlank
        private String path = "/webhook/max";
        private String secret;

        @Positive
        private Integer maxInFlight;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public Integer getMaxInFlight() {
            return maxInFlight;
        }

        public void setMaxInFlight(Integer maxInFlight) {
            this.maxInFlight = maxInFlight;
        }
    }

    public static final class Storage {
        @NotNull
        private MaxBotStorageType type = MaxBotStorageType.MEMORY;

        @NotNull
        private StateScope stateScope = StateScope.USER_IN_CHAT;

        public MaxBotStorageType getType() {
            return type;
        }

        public void setType(MaxBotStorageType type) {
            this.type = type;
        }

        public StateScope getStateScope() {
            return stateScope;
        }

        public void setStateScope(StateScope stateScope) {
            this.stateScope = stateScope;
        }
    }
}
