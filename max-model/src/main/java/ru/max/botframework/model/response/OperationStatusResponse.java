package ru.max.botframework.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Generic success status response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OperationStatusResponse(boolean success) {
}
