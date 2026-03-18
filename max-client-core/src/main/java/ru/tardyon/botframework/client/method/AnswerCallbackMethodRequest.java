package ru.tardyon.botframework.client.method;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import ru.tardyon.botframework.client.MaxRequest;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.model.TextFormat;
import ru.tardyon.botframework.model.request.AnswerCallbackRequest;
import ru.tardyon.botframework.model.request.NewMessageBody;
import ru.tardyon.botframework.model.response.OperationStatusResponse;

/**
 * Domain-level request for POST /answers callback operation.
 */
public final class AnswerCallbackMethodRequest implements MaxRequest<OperationStatusResponse> {
    private final AnswerCallbackRequest request;

    public AnswerCallbackMethodRequest(AnswerCallbackRequest request) {
        this.request = Objects.requireNonNull(request, "request");
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.POST;
    }

    @Override
    public String path() {
        return "/answers";
    }

    @Override
    public Class<OperationStatusResponse> responseType() {
        return OperationStatusResponse.class;
    }

    @Override
    public Map<String, String> queryParameters() {
        return Map.of("callback_id", request.callbackId().value());
    }

    @Override
    public Optional<Object> body() {
        String text = request.text();
        if (text == null || text.isBlank()) {
            return Optional.of(new AnswerBody(null, null));
        }

        if (Boolean.TRUE.equals(request.sendNotification())) {
            return Optional.of(new AnswerBody(null, text));
        }

        return Optional.of(new AnswerBody(new NewMessageBody(text, TextFormat.PLAIN, List.of()), null));
    }

    private record AnswerBody(
            @JsonProperty("message") NewMessageBody message,
            @JsonProperty("notification") String notification
    ) {
    }
}
