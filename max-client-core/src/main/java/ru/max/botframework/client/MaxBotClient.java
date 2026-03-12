package ru.max.botframework.client;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.List;
import ru.max.botframework.client.method.AnswerCallbackMethodRequest;
import ru.max.botframework.client.method.CreateSubscriptionMethodRequest;
import ru.max.botframework.client.method.DeleteMessageMethodRequest;
import ru.max.botframework.client.method.DeleteSubscriptionMethodRequest;
import ru.max.botframework.client.method.EditMessageMethodRequest;
import ru.max.botframework.client.method.GetMeRequest;
import ru.max.botframework.client.method.GetMessageMethodRequest;
import ru.max.botframework.client.method.GetMessagesMethodRequest;
import ru.max.botframework.client.method.GetSubscriptionsMethodRequest;
import ru.max.botframework.client.method.GetUpdatesMethodRequest;
import ru.max.botframework.client.method.SendMessageMethodRequest;
import ru.max.botframework.model.BotInfo;
import ru.max.botframework.model.ChatId;
import ru.max.botframework.model.Message;
import ru.max.botframework.model.MessageId;
import ru.max.botframework.model.request.AnswerCallbackRequest;
import ru.max.botframework.model.request.CreateSubscriptionRequest;
import ru.max.botframework.model.request.DeleteSubscriptionRequest;
import ru.max.botframework.model.request.EditMessageRequest;
import ru.max.botframework.model.request.GetUpdatesRequest;
import ru.max.botframework.model.request.SendMessageRequest;
import ru.max.botframework.model.response.GetUpdatesResponse;
import ru.max.botframework.model.Subscription;

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

    default Message sendMessage(SendMessageRequest request) {
        return execute(new SendMessageMethodRequest(request)).message();
    }

    default CompletionStage<Message> sendMessageAsync(SendMessageRequest request) {
        return executeAsync(new SendMessageMethodRequest(request)).thenApply(response -> response.message());
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

    default List<Message> getMessages(ChatId chatId) {
        return execute(new GetMessagesMethodRequest(chatId)).messages();
    }

    default List<Message> getMessages(List<MessageId> messageIds) {
        return execute(new GetMessagesMethodRequest(messageIds)).messages();
    }

    default boolean answerCallback(AnswerCallbackRequest request) {
        return execute(new AnswerCallbackMethodRequest(request)).success();
    }

    default CompletionStage<Boolean> answerCallbackAsync(AnswerCallbackRequest request) {
        return executeAsync(new AnswerCallbackMethodRequest(request)).thenApply(response -> response.success());
    }

    default GetUpdatesResponse getUpdates(GetUpdatesRequest request) {
        return execute(new GetUpdatesMethodRequest(request));
    }

    default CompletionStage<GetUpdatesResponse> getUpdatesAsync(GetUpdatesRequest request) {
        return executeAsync(new GetUpdatesMethodRequest(request));
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
