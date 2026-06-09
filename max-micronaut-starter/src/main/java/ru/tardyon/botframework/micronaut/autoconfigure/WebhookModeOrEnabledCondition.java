package ru.tardyon.botframework.micronaut.autoconfigure;

import io.micronaut.context.BeanContext;
import io.micronaut.context.condition.Condition;
import io.micronaut.context.condition.ConditionContext;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.value.PropertyResolver;
import java.util.Locale;

public final class WebhookModeOrEnabledCondition implements Condition {
    @Override
    public boolean matches(@NonNull ConditionContext context) {
        BeanContext beanContext = context.getBeanContext();
        if (!(beanContext instanceof PropertyResolver propertyResolver)) {
            return false;
        }

        String mode = propertyResolver.getProperty("max.bot.mode", String.class)
                .orElse("POLLING")
                .toUpperCase(Locale.ROOT);
        boolean webhookEnabled = propertyResolver.getProperty("max.bot.webhook.enabled", Boolean.class)
                .orElse(false);
        return "WEBHOOK".equals(mode) || webhookEnabled;
    }
}
