package ru.max.botframework.spring.properties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.max.botframework.model.UpdateType;

/**
 * Spring Boot configuration properties for MAX bot runtime starter.
 */
@ConfigurationProperties(prefix = "max.bot")
public class MaxBotProperties {
    private String token;
    private String baseUrl = "https://api.max.ru";
    private MaxBotMode mode = MaxBotMode.POLLING;
    private final Polling polling = new Polling();
    private final Webhook webhook = new Webhook();

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

    public static final class Polling {
        private boolean enabled = true;
        private Integer limit;
        private Duration timeout;
        private final List<UpdateType> types = new ArrayList<>();

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

        public List<UpdateType> getTypes() {
            return types;
        }
    }

    public static final class Webhook {
        private boolean enabled;
        private String path = "/webhook/max";
        private String secret;
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
}
