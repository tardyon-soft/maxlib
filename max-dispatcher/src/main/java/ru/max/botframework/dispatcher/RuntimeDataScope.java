package ru.max.botframework.dispatcher;

/**
 * Runtime data source scope for request-scoped data container.
 */
public enum RuntimeDataScope {
    FRAMEWORK,
    FILTER,
    MIDDLEWARE,
    APPLICATION
}
