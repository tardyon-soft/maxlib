package ru.tardyon.botframework.client;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.List;
import ru.tardyon.botframework.client.method.AnswerCallbackMethodRequest;
import ru.tardyon.botframework.client.method.CreateSubscriptionMethodRequest;
import ru.tardyon.botframework.client.method.DeleteMessageMethodRequest;
import ru.tardyon.botframework.client.method.DeleteSubscriptionMethodRequest;
import ru.tardyon.botframework.client.method.EditMessageMethodRequest;
import ru.tardyon.botframework.client.method.GetMeRequest;
import ru.tardyon.botframework.client.method.GetMeApiMethodRequest;
import ru.tardyon.botframework.client.method.GetMessageApiMethodRequest;
import ru.tardyon.botframework.client.method.GetMessageMethodRequest;
import ru.tardyon.botframework.client.method.GetMessagesMethodRequest;
import ru.tardyon.botframework.client.method.GetSubscriptionsMethodRequest;
import ru.tardyon.botframework.client.method.GetUpdatesApiMethodRequest;
import ru.tardyon.botframework.client.method.GetUpdatesMethodRequest;
import ru.tardyon.botframework.client.method.GetMessagesApiMethodRequest;
import ru.tardyon.botframework.client.method.GetChatsApiMethodRequest;
import ru.tardyon.botframework.client.method.GetChatApiMethodRequest;
import ru.tardyon.botframework.client.method.GetVideoApiMethodRequest;
import ru.tardyon.botframework.client.method.GetChatMembersApiMethodRequest;
import ru.tardyon.botframework.client.method.AddChatMembersApiMethodRequest;
import ru.tardyon.botframework.client.method.RemoveChatMemberApiMethodRequest;
import ru.tardyon.botframework.client.method.GetChatAdminsApiMethodRequest;
import ru.tardyon.botframework.client.method.AddChatAdminsApiMethodRequest;
import ru.tardyon.botframework.client.method.RemoveChatAdminApiMethodRequest;
import ru.tardyon.botframework.client.method.GetMyChatMembershipApiMethodRequest;
import ru.tardyon.botframework.client.method.LeaveChatApiMethodRequest;
import ru.tardyon.botframework.client.method.GetChatPinApiMethodRequest;
import ru.tardyon.botframework.client.method.PinChatMessageApiMethodRequest;
import ru.tardyon.botframework.client.method.UnpinChatMessageApiMethodRequest;
import ru.tardyon.botframework.client.method.PatchChatApiMethodRequest;
import ru.tardyon.botframework.client.method.DeleteChatApiMethodRequest;
import ru.tardyon.botframework.client.method.SendMessageMethodRequest;
import ru.tardyon.botframework.client.method.SendChatActionMethodRequest;
import ru.tardyon.botframework.client.method.SendMessageApiMethodRequest;
import ru.tardyon.botframework.client.method.PrepareUploadApiMethodRequest;
import ru.tardyon.botframework.model.ChatAction;
import ru.tardyon.botframework.model.BotInfo;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.Message;
import ru.tardyon.botframework.model.MessageId;
import ru.tardyon.botframework.model.request.AnswerCallbackRequest;
import ru.tardyon.botframework.model.request.CreateSubscriptionRequest;
import ru.tardyon.botframework.model.request.DeleteSubscriptionRequest;
import ru.tardyon.botframework.model.request.EditMessageRequest;
import ru.tardyon.botframework.model.request.GetChatsApiRequest;
import ru.tardyon.botframework.model.request.GetMessagesApiRequest;
import ru.tardyon.botframework.model.request.GetUpdatesRequest;
import ru.tardyon.botframework.model.request.GetChatMembersApiRequest;
import ru.tardyon.botframework.model.request.AddChatMembersApiRequest;
import ru.tardyon.botframework.model.request.RemoveChatMemberApiRequest;
import ru.tardyon.botframework.model.request.AddChatAdminsApiRequest;
import ru.tardyon.botframework.model.request.PinChatMessageApiRequest;
import ru.tardyon.botframework.model.request.PrepareUploadApiRequest;
import ru.tardyon.botframework.model.request.SendChatActionRequest;
import ru.tardyon.botframework.model.request.SendMessageApiRequest;
import ru.tardyon.botframework.model.request.SendMessageRequest;
import ru.tardyon.botframework.model.request.UpdateChatApiRequest;
import ru.tardyon.botframework.model.response.GetUpdatesResponse;
import ru.tardyon.botframework.model.Subscription;
import ru.tardyon.botframework.model.transport.ApiGetUpdatesResponse;
import ru.tardyon.botframework.model.transport.ApiChat;
import ru.tardyon.botframework.model.transport.ApiChatsResponse;
import ru.tardyon.botframework.model.transport.ApiMessage;
import ru.tardyon.botframework.model.transport.ApiChatMembersResponse;
import ru.tardyon.botframework.model.transport.ApiChatPinResponse;
import ru.tardyon.botframework.model.transport.ApiUploadResponse;
import ru.tardyon.botframework.model.transport.ApiUser;
import ru.tardyon.botframework.model.transport.ApiVideoInfo;

/**
 * High-level entry point for executing typed MAX API requests.
 */
public interface MaxBotClient {

    <T> T execute(MaxRequest<T> request);

    default BotInfo getMe() {
        return execute(GetMeRequest.INSTANCE);
    }

    default CompletionStage<BotInfo> getMeAsync() {
        return executeAsync(GetMeRequest.INSTANCE);
    }

    default ApiUser getMeApi() {
        return execute(GetMeApiMethodRequest.INSTANCE);
    }

    default CompletionStage<ApiUser> getMeApiAsync() {
        return executeAsync(GetMeApiMethodRequest.INSTANCE);
    }

    default Message sendMessage(SendMessageRequest request) {
        return execute(new SendMessageMethodRequest(request)).message();
    }

    default CompletionStage<Message> sendMessageAsync(SendMessageRequest request) {
        return executeAsync(new SendMessageMethodRequest(request)).thenApply(response -> response.message());
    }

    default Message sendMessageApi(SendMessageApiRequest request) {
        return execute(new SendMessageApiMethodRequest(request)).message();
    }

    default CompletionStage<Message> sendMessageApiAsync(SendMessageApiRequest request) {
        return executeAsync(new SendMessageApiMethodRequest(request)).thenApply(response -> response.message());
    }

    default boolean editMessage(EditMessageRequest request) {
        return execute(new EditMessageMethodRequest(request)).success();
    }

    default CompletionStage<Boolean> editMessageAsync(EditMessageRequest request) {
        return executeAsync(new EditMessageMethodRequest(request)).thenApply(response -> response.success());
    }

    default boolean deleteMessage(MessageId messageId) {
        return execute(new DeleteMessageMethodRequest(messageId)).success();
    }

    default CompletionStage<Boolean> deleteMessageAsync(MessageId messageId) {
        return executeAsync(new DeleteMessageMethodRequest(messageId)).thenApply(response -> response.success());
    }

    default Message getMessage(MessageId messageId) {
        return execute(new GetMessageMethodRequest(messageId));
    }

    default CompletionStage<Message> getMessageAsync(MessageId messageId) {
        return executeAsync(new GetMessageMethodRequest(messageId));
    }

    default ApiMessage getMessageApi(MessageId messageId) {
        return execute(new GetMessageApiMethodRequest(messageId));
    }

    default CompletionStage<ApiMessage> getMessageApiAsync(MessageId messageId) {
        return executeAsync(new GetMessageApiMethodRequest(messageId));
    }

    default List<Message> getMessages(ChatId chatId) {
        return execute(new GetMessagesMethodRequest(chatId)).messages();
    }

    default List<Message> getMessages(List<MessageId> messageIds) {
        return execute(new GetMessagesMethodRequest(messageIds)).messages();
    }

    default List<Message> getMessagesApi(GetMessagesApiRequest request) {
        return execute(new GetMessagesApiMethodRequest(request)).messages();
    }

    default CompletionStage<List<Message>> getMessagesApiAsync(GetMessagesApiRequest request) {
        return executeAsync(new GetMessagesApiMethodRequest(request)).thenApply(response -> response.messages());
    }

    default ApiChatsResponse getChatsApi(GetChatsApiRequest request) {
        return execute(new GetChatsApiMethodRequest(request));
    }

    default CompletionStage<ApiChatsResponse> getChatsApiAsync(GetChatsApiRequest request) {
        return executeAsync(new GetChatsApiMethodRequest(request));
    }

    default ApiChat getChatApi(long chatId) {
        return execute(new GetChatApiMethodRequest(chatId));
    }

    default CompletionStage<ApiChat> getChatApiAsync(long chatId) {
        return executeAsync(new GetChatApiMethodRequest(chatId));
    }

    default ApiChatMembersResponse getChatMembersApi(long chatId, GetChatMembersApiRequest request) {
        return execute(new GetChatMembersApiMethodRequest(chatId, request));
    }

    default CompletionStage<ApiChatMembersResponse> getChatMembersApiAsync(long chatId, GetChatMembersApiRequest request) {
        return executeAsync(new GetChatMembersApiMethodRequest(chatId, request));
    }

    default boolean addChatMembersApi(long chatId, AddChatMembersApiRequest request) {
        return execute(new AddChatMembersApiMethodRequest(chatId, request)).success();
    }

    default CompletionStage<Boolean> addChatMembersApiAsync(long chatId, AddChatMembersApiRequest request) {
        return executeAsync(new AddChatMembersApiMethodRequest(chatId, request)).thenApply(response -> response.success());
    }

    default boolean removeChatMemberApi(long chatId, RemoveChatMemberApiRequest request) {
        return execute(new RemoveChatMemberApiMethodRequest(chatId, request)).success();
    }

    default CompletionStage<Boolean> removeChatMemberApiAsync(long chatId, RemoveChatMemberApiRequest request) {
        return executeAsync(new RemoveChatMemberApiMethodRequest(chatId, request)).thenApply(response -> response.success());
    }

    default ApiChatMembersResponse getChatAdminsApi(long chatId) {
        return execute(new GetChatAdminsApiMethodRequest(chatId));
    }

    default CompletionStage<ApiChatMembersResponse> getChatAdminsApiAsync(long chatId) {
        return executeAsync(new GetChatAdminsApiMethodRequest(chatId));
    }

    default boolean addChatAdminsApi(long chatId, AddChatAdminsApiRequest request) {
        return execute(new AddChatAdminsApiMethodRequest(chatId, request)).success();
    }

    default CompletionStage<Boolean> addChatAdminsApiAsync(long chatId, AddChatAdminsApiRequest request) {
        return executeAsync(new AddChatAdminsApiMethodRequest(chatId, request)).thenApply(response -> response.success());
    }

    default boolean removeChatAdminApi(long chatId, long userId) {
        return execute(new RemoveChatAdminApiMethodRequest(chatId, userId)).success();
    }

    default CompletionStage<Boolean> removeChatAdminApiAsync(long chatId, long userId) {
        return executeAsync(new RemoveChatAdminApiMethodRequest(chatId, userId)).thenApply(response -> response.success());
    }

    default ApiUser getMyChatMembershipApi(long chatId) {
        return execute(new GetMyChatMembershipApiMethodRequest(chatId));
    }

    default CompletionStage<ApiUser> getMyChatMembershipApiAsync(long chatId) {
        return executeAsync(new GetMyChatMembershipApiMethodRequest(chatId));
    }

    default boolean leaveChatApi(long chatId) {
        return execute(new LeaveChatApiMethodRequest(chatId)).success();
    }

    default CompletionStage<Boolean> leaveChatApiAsync(long chatId) {
        return executeAsync(new LeaveChatApiMethodRequest(chatId)).thenApply(response -> response.success());
    }

    default ApiChatPinResponse getChatPinApi(long chatId) {
        return execute(new GetChatPinApiMethodRequest(chatId));
    }

    default CompletionStage<ApiChatPinResponse> getChatPinApiAsync(long chatId) {
        return executeAsync(new GetChatPinApiMethodRequest(chatId));
    }

    default boolean pinChatMessageApi(long chatId, PinChatMessageApiRequest request) {
        return execute(new PinChatMessageApiMethodRequest(chatId, request)).success();
    }

    default CompletionStage<Boolean> pinChatMessageApiAsync(long chatId, PinChatMessageApiRequest request) {
        return executeAsync(new PinChatMessageApiMethodRequest(chatId, request)).thenApply(response -> response.success());
    }

    default boolean unpinChatMessageApi(long chatId) {
        return execute(new UnpinChatMessageApiMethodRequest(chatId)).success();
    }

    default CompletionStage<Boolean> unpinChatMessageApiAsync(long chatId) {
        return executeAsync(new UnpinChatMessageApiMethodRequest(chatId)).thenApply(response -> response.success());
    }

    default ApiChat patchChatApi(long chatId, UpdateChatApiRequest request) {
        return execute(new PatchChatApiMethodRequest(chatId, request));
    }

    default CompletionStage<ApiChat> patchChatApiAsync(long chatId, UpdateChatApiRequest request) {
        return executeAsync(new PatchChatApiMethodRequest(chatId, request));
    }

    default boolean deleteChatApi(long chatId) {
        return execute(new DeleteChatApiMethodRequest(chatId)).success();
    }

    default CompletionStage<Boolean> deleteChatApiAsync(long chatId) {
        return executeAsync(new DeleteChatApiMethodRequest(chatId)).thenApply(response -> response.success());
    }

    default ApiUploadResponse prepareUploadApi(PrepareUploadApiRequest request) {
        return execute(new PrepareUploadApiMethodRequest(request));
    }

    default CompletionStage<ApiUploadResponse> prepareUploadApiAsync(PrepareUploadApiRequest request) {
        return executeAsync(new PrepareUploadApiMethodRequest(request));
    }

    default ApiVideoInfo getVideoApi(String videoToken) {
        return execute(new GetVideoApiMethodRequest(videoToken));
    }

    default CompletionStage<ApiVideoInfo> getVideoApiAsync(String videoToken) {
        return executeAsync(new GetVideoApiMethodRequest(videoToken));
    }

    default boolean answerCallback(AnswerCallbackRequest request) {
        return execute(new AnswerCallbackMethodRequest(request)).success();
    }

    default CompletionStage<Boolean> answerCallbackAsync(AnswerCallbackRequest request) {
        return executeAsync(new AnswerCallbackMethodRequest(request)).thenApply(response -> response.success());
    }

    default boolean sendChatAction(ChatId chatId, SendChatActionRequest request) {
        return execute(new SendChatActionMethodRequest(chatId, request)).success();
    }

    default boolean sendChatAction(ChatId chatId, ChatAction action) {
        return sendChatAction(chatId, new SendChatActionRequest(action));
    }

    default CompletionStage<Boolean> sendChatActionAsync(ChatId chatId, SendChatActionRequest request) {
        return executeAsync(new SendChatActionMethodRequest(chatId, request)).thenApply(response -> response.success());
    }

    default CompletionStage<Boolean> sendChatActionAsync(ChatId chatId, ChatAction action) {
        return sendChatActionAsync(chatId, new SendChatActionRequest(action));
    }

    default GetUpdatesResponse getUpdates(GetUpdatesRequest request) {
        return execute(new GetUpdatesMethodRequest(request));
    }

    default CompletionStage<GetUpdatesResponse> getUpdatesAsync(GetUpdatesRequest request) {
        return executeAsync(new GetUpdatesMethodRequest(request));
    }

    /**
     * Transport-shape getUpdates response as documented by MAX API.
     */
    default ApiGetUpdatesResponse getUpdatesApi(GetUpdatesRequest request) {
        return execute(new GetUpdatesApiMethodRequest(request));
    }

    default CompletionStage<ApiGetUpdatesResponse> getUpdatesApiAsync(GetUpdatesRequest request) {
        return executeAsync(new GetUpdatesApiMethodRequest(request));
    }

    default List<Subscription> getSubscriptions() {
        return execute(GetSubscriptionsMethodRequest.INSTANCE).subscriptions();
    }

    default boolean createSubscription(CreateSubscriptionRequest request) {
        return execute(new CreateSubscriptionMethodRequest(request)).success();
    }

    default CompletionStage<Boolean> createSubscriptionAsync(CreateSubscriptionRequest request) {
        return executeAsync(new CreateSubscriptionMethodRequest(request)).thenApply(response -> response.success());
    }

    default boolean deleteSubscription(DeleteSubscriptionRequest request) {
        return execute(new DeleteSubscriptionMethodRequest(request)).success();
    }

    default CompletionStage<Boolean> deleteSubscriptionAsync(DeleteSubscriptionRequest request) {
        return executeAsync(new DeleteSubscriptionMethodRequest(request)).thenApply(response -> response.success());
    }

    default <T> CompletionStage<T> executeAsync(MaxRequest<T> request) {
        return CompletableFuture.supplyAsync(() -> execute(request));
    }
}
