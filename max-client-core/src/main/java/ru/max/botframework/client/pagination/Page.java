package ru.max.botframework.client.pagination;

import java.util.List;

/**
 * Generic immutable page abstraction for list responses.
 *
 * @param <T> item type
 */
public interface Page<T> {

    List<T> items();

    boolean hasNext();
}
