package ru.tardyon.botframework.dispatcher.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import ru.tardyon.botframework.dispatcher.Filter;

/**
 * Declares additional filters for route class or handler method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface UseFilters {
    Class<? extends Filter<?>>[] value();
}
