package ru.max.botframework.dispatcher;

/**
 * Runtime dispatch-layer error categories.
 */
public enum RuntimeDispatchErrorType {
    /** Handler execution failed after successful matching. */
    HANDLER_FAILURE,
    /** Filter evaluation failed (exception or explicit failed result). */
    FILTER_FAILURE,
    /** Outer middleware failed before/after dispatch traversal. */
    OUTER_MIDDLEWARE_FAILURE,
    /** Inner middleware failed around matched handler execution. */
    INNER_MIDDLEWARE_FAILURE,
    /** Runtime context enrichment merge failed because of conflict/type mismatch. */
    ENRICHMENT_FAILURE,
    /** Update-to-event mapping failed in resolver. */
    EVENT_MAPPING_FAILURE,
    /** Observer invocation failed outside of handler body. */
    OBSERVER_EXECUTION_FAILURE
}
