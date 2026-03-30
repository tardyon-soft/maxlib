package ru.tardyon.botframework.spring.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import ru.tardyon.botframework.fsm.StateScope;
import ru.tardyon.botframework.model.UpdateEventType;
import ru.tardyon.botframework.screen.ScreenActionCodecMode;

/**
 * Spring Boot configuration properties for MAX bot runtime starter.
 */
@ConfigurationProperties(prefix = "max.bot")
@Validated
public class MaxBotProperties {
    @NotBlank
    private String token;

    @NotBlank
    private String baseUrl = "https://platform-api.max.ru";

    @NotNull
    private MaxBotMode mode = MaxBotMode.POLLING;

    @Valid
    private final Polling polling = new Polling();

    @Valid
    private final Webhook webhook = new Webhook();

    @Valid
    private final Storage storage = new Storage();
    @Valid
    private final Screen screen = new Screen();

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

    public Screen getScreen() {
        return screen;
    }

    /**
     * Polling-specific properties (`max.bot.polling.*`).
     */
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

    /**
     * Webhook-specific properties (`max.bot.webhook.*`).
     */
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

    /**
     * Runtime storage and FSM scope properties (`max.bot.storage.*`).
     */
    public static final class Storage {
        @NotNull
        private MaxBotStorageType type = MaxBotStorageType.MEMORY;

        @NotNull
        private StateScope stateScope = StateScope.USER_IN_CHAT;
        @Valid
        private final Redis redis = new Redis();

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

        public Redis getRedis() {
            return redis;
        }
    }

    /**
     * Redis-backed FSM storage properties (`max.bot.storage.redis.*`).
     */
    public static final class Redis {
        /**
         * Key prefix for all FSM entries.
         */
        @NotBlank
        private String keyPrefix = "max:bot:fsm";

        /**
         * Optional TTL for FSM keys. If null, keys do not expire.
         */
        private Duration ttl;

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }
    }

    /**
     * Screen runtime properties (`max.bot.screen.*`).
     */
    public static final class Screen {
        @Valid
        private final Callback callback = new Callback();

        public Callback getCallback() {
            return callback;
        }
    }

    /**
     * Screen callback properties (`max.bot.screen.callback.*`).
     */
    public static final class Callback {
        @Valid
        private final Codec codec = new Codec();

        public Codec getCodec() {
            return codec;
        }
    }

    /**
     * Screen callback codec properties (`max.bot.screen.callback.codec.*`).
     */
    public static final class Codec {
        @NotNull
        private ScreenActionCodecMode mode = ScreenActionCodecMode.LEGACY_STRING;

        public ScreenActionCodecMode getMode() {
            return mode;
        }

        public void setMode(ScreenActionCodecMode mode) {
            this.mode = mode;
        }
    }
}
