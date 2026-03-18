package ru.tardyon.botframework.demo.springpolling;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.tardyon.botframework.client.MaxBotClient;
import ru.tardyon.botframework.client.error.MaxApiException;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.Message;
import ru.tardyon.botframework.model.MessageId;
import ru.tardyon.botframework.model.TextFormat;
import ru.tardyon.botframework.model.UpdateEventType;
import ru.tardyon.botframework.model.request.CreateSubscriptionRequest;
import ru.tardyon.botframework.model.request.DeleteSubscriptionRequest;
import ru.tardyon.botframework.model.request.GetChatMembersApiRequest;
import ru.tardyon.botframework.model.request.GetChatsApiRequest;
import ru.tardyon.botframework.model.request.GetMessagesApiRequest;
import ru.tardyon.botframework.model.request.GetUpdatesRequest;
import ru.tardyon.botframework.model.request.NewMessageBody;
import ru.tardyon.botframework.model.request.PinChatMessageApiRequest;
import ru.tardyon.botframework.model.request.PrepareUploadApiRequest;
import ru.tardyon.botframework.model.request.SendChatActionRequest;
import ru.tardyon.botframework.model.request.SendMessageApiRequest;
import ru.tardyon.botframework.model.request.UploadType;
import ru.tardyon.botframework.model.transport.ApiChat;
import ru.tardyon.botframework.model.transport.ApiChatsResponse;
import ru.tardyon.botframework.model.transport.ApiGetUpdatesResponse;
import ru.tardyon.botframework.model.transport.ApiUploadResponse;
import ru.tardyon.botframework.model.transport.ApiUser;

@Component
public final class ApiSmokeService {
    private final MaxBotClient client;
    private final boolean destructive;
    private final String webhookUrl;
    private final ConcurrentHashMap<String, SmokeState> stateByChat = new ConcurrentHashMap<>();

    public ApiSmokeService(
            MaxBotClient client,
            @Value("${demo.smoke.destructive:false}") boolean destructive,
            @Value("${demo.smoke.webhook-url:}") String webhookUrl
    ) {
        this.client = Objects.requireNonNull(client, "client");
        this.destructive = destructive;
        this.webhookUrl = webhookUrl == null ? "" : webhookUrl.trim();
    }

    public String runAll(Message sourceMessage) {
        String chatKey = sourceMessage.chat().id().value();
        SmokeState state = stateByChat.computeIfAbsent(chatKey, ignored -> new SmokeState());
        Long chatId = resolveChatId(sourceMessage, state);

        ArrayList<StepResult> steps = new ArrayList<>();
        steps.add(run("GET /me", () -> {
            ApiUser me = client.getMeApi();
            state.myUserId = me.userId();
            return "user_id=" + me.userId();
        }));

        steps.add(run("GET /chats", () -> {
            ApiChatsResponse chats = client.getChatsApi(new GetChatsApiRequest(null, 20));
            if (chats.chats().isEmpty()) {
                return "chats=0";
            }
            ApiChat first = chats.chats().getFirst();
            state.lastChatId = first.chatId();
            return "chats=" + chats.chats().size() + ", first_chat_id=" + first.chatId();
        }));

        if (chatId == null) {
            steps.add(StepResult.skip("GET /chats/{chatId}", "No numeric chat id"));
        } else {
            long targetChatId = chatId;
            steps.add(run("GET /chats/{chatId}", () -> {
                ApiChat chat = client.getChatApi(targetChatId);
                state.lastChatType = chat.type();
                return "chat_id=" + chat.chatId() + ", type=" + chat.type();
            }));
        }

        if (chatId == null) {
            steps.add(StepResult.skip("POST /messages", "No numeric chat id"));
        } else {
            long targetChatId = chatId;
            steps.add(run("POST /messages", () -> {
                Message sent = client.sendMessageApi(new SendMessageApiRequest(
                        null,
                        targetChatId,
                        false,
                        new NewMessageBody("QA smoke probe " + Instant.now(), TextFormat.PLAIN, List.of()),
                        false,
                        null
                ));
                state.lastMessageId = sent.messageId().value();
                return "message_id=" + state.lastMessageId;
            }));
        }

        if (chatId == null || state.lastMessageId == null || isUnknownId(state.lastMessageId)) {
            steps.add(StepResult.skip("PUT /messages", "No sent message for edit"));
        } else {
            long targetChatId = chatId;
            steps.add(run("PUT /messages", () -> {
                boolean ok = client.editMessage(new ru.tardyon.botframework.model.request.EditMessageRequest(
                        new ChatId(Long.toString(targetChatId)),
                        new MessageId(state.lastMessageId),
                        new NewMessageBody("QA smoke edited " + Instant.now(), TextFormat.PLAIN, List.of()),
                        false
                ));
                return "edited=" + ok;
            }));
        }

        if (state.lastMessageId == null || isUnknownId(state.lastMessageId)) {
            steps.add(StepResult.skip("GET /messages/{messageId}", "No sent message id"));
        } else {
            String messageId = state.lastMessageId;
            steps.add(run("GET /messages/{messageId}", () ->
                    "message_id=" + client.getMessageApi(new MessageId(messageId)).messageId()));
        }

        if (chatId == null) {
            steps.add(StepResult.skip("GET /messages", "No numeric chat id"));
        } else {
            long targetChatId = chatId;
            steps.add(run("GET /messages", () ->
                    "messages=" + client.getMessagesApi(new GetMessagesApiRequest(targetChatId, null, null, 10)).size()));
        }

        if (!destructive || state.lastMessageId == null || isUnknownId(state.lastMessageId)) {
            steps.add(StepResult.skip("DELETE /messages", "Destructive disabled or no message"));
        } else {
            String messageId = state.lastMessageId;
            steps.add(run("DELETE /messages", () -> {
                boolean deleted = client.deleteMessage(new MessageId(messageId));
                return "deleted=" + deleted;
            }));
        }

        if (chatId == null) {
            steps.add(StepResult.skip("POST /chats/{chatId}/actions", "No numeric chat id"));
        } else {
            long targetChatId = chatId;
            steps.add(run("POST /chats/{chatId}/actions", () -> {
                boolean ok = client.sendChatAction(new ChatId(Long.toString(targetChatId)), new SendChatActionRequest(
                        ru.tardyon.botframework.model.ChatAction.TYPING
                ));
                return "typing_on=" + ok;
            }));
        }

        if (chatId == null) {
            steps.add(StepResult.skip("GET /chats/{chatId}/members", "No numeric chat id"));
            steps.add(StepResult.skip("GET /chats/{chatId}/members/admins", "No numeric chat id"));
            steps.add(StepResult.skip("GET /chats/{chatId}/members/me", "No numeric chat id"));
        } else if (!isGroupChat(state.lastChatType)) {
            steps.add(StepResult.skip("GET /chats/{chatId}/members", "Supported only for group chat"));
            steps.add(StepResult.skip("GET /chats/{chatId}/members/admins", "Supported only for group chat"));
            steps.add(StepResult.skip("GET /chats/{chatId}/members/me", "Supported only for group chat"));
        } else {
            long targetChatId = chatId;
            steps.add(run("GET /chats/{chatId}/members", () ->
                    "members=" + client.getChatMembersApi(targetChatId, GetChatMembersApiRequest.defaults()).members().size()));
            steps.add(run("GET /chats/{chatId}/members/admins", () ->
                    "admins=" + client.getChatAdminsApi(targetChatId).admins().size()));
            steps.add(run("GET /chats/{chatId}/members/me", () -> {
                ApiUser me = client.getMyChatMembershipApi(targetChatId);
                return "user_id=" + me.userId();
            }));
        }

        if (!destructive) {
            steps.add(StepResult.skip("POST/DELETE /chats/{chatId}/members*", "Destructive disabled"));
            steps.add(StepResult.skip("POST/DELETE /chats/{chatId}/members/admins*", "Destructive disabled"));
            steps.add(StepResult.skip("DELETE /chats/{chatId}/members/me", "Destructive disabled"));
            steps.add(StepResult.skip("PATCH/DELETE /chats/{chatId}", "Destructive disabled"));
        }

        if (chatId == null || state.lastMessageId == null || isUnknownId(state.lastMessageId)) {
            steps.add(StepResult.skip("GET/PUT/DELETE /chats/{chatId}/pin", "No numeric chat id or message id"));
        } else if (!isGroupChat(state.lastChatType)) {
            steps.add(StepResult.skip("GET/PUT/DELETE /chats/{chatId}/pin", "Supported only for group chat"));
        } else {
            long targetChatId = chatId;
            String messageId = state.lastMessageId;
            steps.add(run("PUT /chats/{chatId}/pin", () -> {
                boolean ok = client.pinChatMessageApi(targetChatId, new PinChatMessageApiRequest(messageId, false));
                return "pinned=" + ok;
            }));
            steps.add(run("GET /chats/{chatId}/pin", () -> {
                var pin = client.getChatPinApi(targetChatId);
                String pinned = pin.message() == null ? "null" : pin.message().messageId();
                return "pinned_message_id=" + pinned;
            }));
            steps.add(run("DELETE /chats/{chatId}/pin", () -> "unpinned=" + client.unpinChatMessageApi(targetChatId)));
        }

        steps.add(run("GET /updates", () -> {
            ApiGetUpdatesResponse updates = client.getUpdatesApi(new GetUpdatesRequest(
                    null,
                    0,
                    1,
                    List.of(UpdateEventType.MESSAGE_CREATED, UpdateEventType.MESSAGE_CALLBACK)
            ));
            return "updates=" + updates.updates().size() + ", marker=" + updates.marker();
        }));

        steps.add(run("GET /subscriptions", () -> "subscriptions=" + client.getSubscriptions().size()));
        if (webhookUrl.isBlank()) {
            steps.add(StepResult.skip("POST/DELETE /subscriptions", "demo.smoke.webhook-url is empty"));
        } else {
            steps.add(run("POST /subscriptions", () -> {
                boolean ok = client.createSubscription(new CreateSubscriptionRequest(
                        webhookUrl,
                        List.of(UpdateEventType.MESSAGE_CREATED),
                        "smoke-secret"
                ));
                return "created=" + ok;
            }));
            steps.add(run("DELETE /subscriptions", () -> {
                boolean ok = client.deleteSubscription(new DeleteSubscriptionRequest(webhookUrl));
                return "deleted=" + ok;
            }));
        }

        steps.add(run("POST /uploads", () -> {
            ApiUploadResponse upload = client.prepareUploadApi(new PrepareUploadApiRequest(UploadType.VIDEO));
            state.lastUploadToken = upload.token();
            return "token=" + shorten(upload.token()) + ", url=" + shorten(upload.url());
        }));

        if (state.lastVideoToken == null || state.lastVideoToken.isBlank()) {
            steps.add(StepResult.skip("GET /videos/{videoToken}", "Set token via /qa_set_video <token>"));
        } else {
            String token = state.lastVideoToken;
            steps.add(run("GET /videos/{videoToken}", () ->
                    "urls=" + client.getVideoApi(token).urls().size() + ", token=" + shorten(token)));
        }

        steps.add(StepResult.skip("POST /answers", "Run /qa_callback and click button"));
        return toReport(steps, chatId);
    }

    public void setVideoToken(Message sourceMessage, String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        stateByChat.computeIfAbsent(sourceMessage.chat().id().value(), ignored -> new SmokeState()).lastVideoToken = token.trim();
    }

    private static String shorten(String value) {
        if (value == null) {
            return "null";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 16) {
            return trimmed;
        }
        return trimmed.substring(0, 12) + "...";
    }

    private Long resolveChatId(Message sourceMessage, SmokeState state) {
        Long fromMessage = parseLongOrNull(sourceMessage.chat().id().value());
        if (fromMessage != null) {
            state.lastChatId = fromMessage;
            return fromMessage;
        }
        if (state.lastChatId != null) {
            return state.lastChatId;
        }
        ApiChatsResponse chats = client.getChatsApi(GetChatsApiRequest.defaults());
        if (chats.chats().isEmpty() || chats.chats().getFirst().chatId() == null) {
            return null;
        }
        state.lastChatId = chats.chats().getFirst().chatId();
        return state.lastChatId;
    }

    private static Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static boolean isUnknownId(String value) {
        return value == null || value.isBlank() || value.endsWith("-unknown");
    }

    private static boolean isGroupChat(String chatType) {
        if (chatType == null || chatType.isBlank()) {
            return false;
        }
        return "chat".equalsIgnoreCase(chatType) || "group".equalsIgnoreCase(chatType);
    }

    private static StepResult run(String name, Supplier<String> action) {
        Instant started = Instant.now();
        try {
            String detail = action.get();
            long ms = Duration.between(started, Instant.now()).toMillis();
            return StepResult.pass(name, detail + " (" + ms + "ms)");
        } catch (Exception exception) {
            Throwable root = unwrap(exception);
            String detail = root.getClass().getSimpleName() + ": " + safeMessage(root.getMessage());
            if (root instanceof MaxApiException apiException) {
                detail = detail + ", status=" + apiException.statusCode();
            }
            return StepResult.fail(name, detail);
        }
    }

    private static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof java.util.concurrent.CompletionException completion && completion.getCause() != null) {
            return completion.getCause();
        }
        return throwable;
    }

    private static String safeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "no-message";
        }
        String compact = message.replace('\n', ' ').replace('\r', ' ').trim();
        return compact.length() > 180 ? compact.substring(0, 180) + "..." : compact;
    }

    private static String toReport(List<StepResult> steps, Long chatId) {
        long pass = steps.stream().filter(step -> step.status == Status.PASS).count();
        long fail = steps.stream().filter(step -> step.status == Status.FAIL).count();
        long skip = steps.stream().filter(step -> step.status == Status.SKIP).count();

        StringBuilder report = new StringBuilder();
        report.append("MAX API smoke report\n");
        report.append("chatId=").append(chatId == null ? "n/a" : chatId).append('\n');
        report.append("summary: pass=").append(pass).append(", fail=").append(fail).append(", skip=").append(skip).append("\n\n");

        for (StepResult step : steps) {
            report.append("- [").append(step.status.name().toLowerCase(Locale.ROOT)).append("] ")
                    .append(step.name).append(" -> ").append(step.detail).append('\n');
        }
        report.append("\nHints:\n");
        report.append("1) /qa_callback -> click button to validate POST /answers.\n");
        report.append("2) /qa_set_video <token> then /qa_run_all for GET /videos/{token}.\n");
        report.append("3) Set demo.smoke.destructive=true only in isolated test chats.");
        return report.toString();
    }

    private static final class SmokeState {
        private Long lastChatId;
        private String lastChatType;
        private Long myUserId;
        private String lastMessageId;
        private String lastUploadToken;
        private String lastVideoToken;
    }

    private enum Status {
        PASS,
        FAIL,
        SKIP
    }

    private static final class StepResult {
        private final String name;
        private final Status status;
        private final String detail;

        private StepResult(String name, Status status, String detail) {
            this.name = name;
            this.status = status;
            this.detail = detail;
        }

        private static StepResult pass(String name, String detail) {
            return new StepResult(name, Status.PASS, detail);
        }

        private static StepResult fail(String name, String detail) {
            return new StepResult(name, Status.FAIL, detail);
        }

        private static StepResult skip(String name, String detail) {
            return new StepResult(name, Status.SKIP, detail);
        }
    }
}
