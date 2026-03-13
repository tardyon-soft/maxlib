/**
 * Runtime dispatch layer:
 * dispatcher orchestration, router tree, observer registration, filter/middleware pipeline and dispatch outcomes.
 *
 * <p>Transport ingestion contracts live in {@code ru.tardyon.botframework.ingestion} and integrate with this package
 * via {@code UpdateConsumer}.</p>
 *
 * <p>Public SPI extension points for Sprint 5:
 * {@code HandlerInvoker}, {@code HandlerParameterResolver}, {@code ResolverRegistry},
 * {@code RuntimeDataKey}/{@code RuntimeDataContainer}.</p>
 *
 * <p>Runtime-internal details (for example resolver execution wrappers) are intentionally package-private.</p>
 */
package ru.tardyon.botframework.dispatcher;
