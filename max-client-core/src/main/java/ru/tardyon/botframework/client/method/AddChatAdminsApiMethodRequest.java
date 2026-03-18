package ru.tardyon.botframework.client.method;

import java.util.Objects;
import java.util.Optional;
import ru.tardyon.botframework.client.MaxRequest;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.model.request.AddChatAdminsApiRequest;
import ru.tardyon.botframework.model.response.OperationStatusResponse;

/**
 * Docs-shaped request for POST /chats/{chatId}/members/admins.
 */
public final class AddChatAdminsApiMethodRequest implements MaxRequest<OperationStatusResponse> {
    private final long chatId;
    private final AddChatAdminsApiRequest request;

    public AddChatAdminsApiMethodRequest(long chatId, AddChatAdminsApiRequest request) {
        this.chatId = chatId;
        this.request = Objects.requireNonNull(request, "request");
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.POST;
    }

    @Override
    public String path() {
        return "/chats/" + chatId + "/members/admins";
    }

    @Override
    public Class<OperationStatusResponse> responseType() {
        return OperationStatusResponse.class;
    }

    @Override
    public Optional<Object> body() {
        return Optional.of(request);
    }
}
