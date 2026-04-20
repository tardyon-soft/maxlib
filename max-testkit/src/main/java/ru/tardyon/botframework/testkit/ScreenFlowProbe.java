package ru.tardyon.botframework.testkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import ru.tardyon.botframework.dispatcher.DispatchStatus;
import ru.tardyon.botframework.model.MessageAttachmentType;
import ru.tardyon.botframework.model.Update;
import ru.tardyon.botframework.model.request.EditMessageRequest;
import ru.tardyon.botframework.model.request.InlineKeyboardAttachment;
import ru.tardyon.botframework.model.request.InlineKeyboardButtonRequest;
import ru.tardyon.botframework.model.request.NewMessageAttachment;
import ru.tardyon.botframework.model.request.NewMessageBody;
import ru.tardyon.botframework.model.request.SendMessageRequest;
import ru.tardyon.botframework.model.transport.ApiAttachmentRequest;
import ru.tardyon.botframework.model.transport.ApiInlineKeyboardButton;
import ru.tardyon.botframework.model.transport.ApiInlineKeyboardPayload;
import ru.tardyon.botframework.model.transport.ApiOutgoingMessageBody;
import ru.tardyon.botframework.screen.ScreenSession;
import ru.tardyon.botframework.screen.ScreenStackEntry;

/**
 * Probe object for asserting one or many screen flow steps.
 */
public final class ScreenFlowProbe {
    private final ScreenTestKit kit;
    private final List<Step> steps;

    ScreenFlowProbe(ScreenTestKit kit, List<Step> steps) {
        this.kit = Objects.requireNonNull(kit, "kit");
        this.steps = List.copyOf(Objects.requireNonNull(steps, "steps"));
        if (this.steps.isEmpty()) {
            throw new IllegalArgumentException("steps must not be empty");
        }
    }

    public List<Step> steps() {
        return steps;
    }

    public Step lastStep() {
        return steps.get(steps.size() - 1);
    }

    public ScreenFlowProbe assertLastHandled() {
        DispatchStatus status = lastStep().probe().result().status();
        if (status != DispatchStatus.HANDLED) {
            throw new AssertionError("Expected HANDLED dispatch status, got: " + status);
        }
        return this;
    }

    public ScreenFlowProbe assertLastHasCall(String path) {
        Objects.requireNonNull(path, "path");
        if (!lastStep().probe().hasCall(path)) {
            throw new AssertionError("Expected API call to path: " + path);
        }
        return this;
    }

    public ScreenFlowProbe assertTopScreen(String expectedScreenId) {
        Objects.requireNonNull(expectedScreenId, "expectedScreenId");
        Optional<ScreenSession> sessionOpt = kit.screenSession(lastStep().update());
        if (sessionOpt.isEmpty()) {
            throw new AssertionError("Expected active screen session, but session is empty");
        }
        ScreenSession session = sessionOpt.orElseThrow();
        ScreenStackEntry top = session.top().orElseThrow(() -> new AssertionError("Expected non-empty screen stack"));
        if (!expectedScreenId.equals(top.screenId())) {
            throw new AssertionError("Expected top screen '" + expectedScreenId + "', got '" + top.screenId() + "'");
        }
        return this;
    }

    public ScreenFlowProbe assertTopParam(String key, Object expectedValue) {
        Objects.requireNonNull(key, "key");
        Optional<ScreenSession> sessionOpt = kit.screenSession(lastStep().update());
        if (sessionOpt.isEmpty()) {
            throw new AssertionError("Expected active screen session, but session is empty");
        }
        ScreenStackEntry top = sessionOpt.orElseThrow()
                .top()
                .orElseThrow(() -> new AssertionError("Expected non-empty screen stack"));
        Object actual = top.params().get(key);
        if (!Objects.equals(expectedValue, actual)) {
            throw new AssertionError("Expected param '" + key + "' to be '" + expectedValue + "', got '" + actual + "'");
        }
        return this;
    }

    public ScreenFlowProbe assertNoActiveScreen() {
        Optional<ScreenSession> sessionOpt = kit.screenSession(lastStep().update());
        if (sessionOpt.isPresent() && sessionOpt.orElseThrow().top().isPresent()) {
            throw new AssertionError("Expected no active screen, but screen session has stack entries");
        }
        return this;
    }

    public ScreenFlowProbe assertLastRenderedPayload(String callbackPayload) {
        Objects.requireNonNull(callbackPayload, "callbackPayload");
        List<String> payloads = extractKeyboardPayloads(lastStep().probe().sideEffects());
        if (!payloads.contains(callbackPayload)) {
            throw new AssertionError("Expected rendered callback payload '" + callbackPayload + "', got: " + payloads);
        }
        return this;
    }

    private static List<String> extractKeyboardPayloads(List<CapturedApiCall> calls) {
        ArrayList<String> payloads = new ArrayList<>();
        for (CapturedApiCall call : calls) {
            call.body().ifPresent(body -> collectFromBody(body, payloads));
        }
        return List.copyOf(payloads);
    }

    private static void collectFromBody(Object body, ArrayList<String> payloads) {
        NewMessageBody messageBody;
        if (body instanceof SendMessageRequest send) {
            messageBody = send.body();
        } else if (body instanceof EditMessageRequest edit) {
            messageBody = edit.body();
        } else {
            messageBody = null;
        }
        if (messageBody == null) {
            collectFromApiOutgoingBody(body, payloads);
            return;
        }
        for (NewMessageAttachment attachment : messageBody.attachments()) {
            if (attachment.type() != MessageAttachmentType.INLINE_KEYBOARD || attachment.inlineKeyboard() == null) {
                continue;
            }
            InlineKeyboardAttachment keyboard = attachment.inlineKeyboard();
            for (List<InlineKeyboardButtonRequest> row : keyboard.rows()) {
                for (InlineKeyboardButtonRequest button : row) {
                    if (button.callbackData() != null && !button.callbackData().isBlank()) {
                        payloads.add(button.callbackData());
                    }
                }
            }
        }
    }

    private static void collectFromApiOutgoingBody(Object body, ArrayList<String> payloads) {
        if (!(body instanceof ApiOutgoingMessageBody outgoingBody)) {
            return;
        }
        for (Object attachment : outgoingBody.attachments()) {
            if (!(attachment instanceof ApiAttachmentRequest apiAttachment)) {
                continue;
            }
            if (!MessageAttachmentType.INLINE_KEYBOARD.value().equals(apiAttachment.type())) {
                continue;
            }
            if (!(apiAttachment.payload() instanceof ApiInlineKeyboardPayload keyboardPayload)) {
                continue;
            }
            for (List<ApiInlineKeyboardButton> row : keyboardPayload.buttons()) {
                for (ApiInlineKeyboardButton button : row) {
                    if (button.payload() != null && !button.payload().isBlank()) {
                        payloads.add(button.payload());
                    }
                }
            }
        }
    }

    public record Step(Update update, DispatcherTestKit.DispatchProbe probe) {
        public Step {
            Objects.requireNonNull(update, "update");
            Objects.requireNonNull(probe, "probe");
        }
    }
}
