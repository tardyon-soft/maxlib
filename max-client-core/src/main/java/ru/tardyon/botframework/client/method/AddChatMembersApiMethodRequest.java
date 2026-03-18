package ru.tardyon.botframework.client.method;

import java.util.Objects;
import java.util.Optional;
import ru.tardyon.botframework.client.MaxRequest;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.model.request.AddChatMembersApiRequest;
import ru.tardyon.botframework.model.response.OperationStatusResponse;

/**
 * Docs-shaped request for POST /chats/{chatId}/members.
 */
public final class AddChatMembersApiMethodRequest implements MaxRequest<OperationStatusResponse> {
    private final long chatId;
    private final AddChatMembersApiRequest request;

    public AddChatMembersApiMethodRequest(long chatId, AddChatMembersApiRequest request) {
        this.chatId = chatId;
        this.request = Objects.requireNonNull(request, "request");
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.POST;
    }

    @Override
    public String path() {
        return "/chats/" + chatId + "/members";
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
