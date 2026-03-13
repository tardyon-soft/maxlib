package ru.max.botframework.model.request;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.max.botframework.model.CallbackId;
import ru.max.botframework.model.ChatId;
import ru.max.botframework.model.FileId;
import ru.max.botframework.model.MessageId;
import ru.max.botframework.model.MessageAttachmentType;
import ru.max.botframework.model.TextFormat;

class RequestModelSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void shouldSerializeAndDeserializeSendMessageRequest() throws Exception {
        SendMessageRequest source = new SendMessageRequest(
                new ChatId("chat-1"),
                new NewMessageBody(
                        "hello",
                        TextFormat.MARKDOWN,
                        List.of(new NewMessageAttachment(
                                MessageAttachmentType.DOCUMENT,
                                new AttachmentInput(new FileId("file-1"), null, null),
                                null,
                                "spec",
                                "application/pdf",
                                1024L
                        ))
                ),
                false,
                new MessageId("m-10")
        );

        String json = objectMapper.writeValueAsString(source);
        SendMessageRequest restored = objectMapper.readValue(json, SendMessageRequest.class);

        assertThat(json).contains("\"chatId\":\"chat-1\"");
        assertThat(json).contains("\"format\":\"markdown\"");
        assertThat(restored).isEqualTo(source);
    }

    @Test
    void shouldSerializeAndDeserializeEditMessageRequest() throws Exception {
        EditMessageRequest source = new EditMessageRequest(
                new ChatId("chat-1"),
                new MessageId("m-1"),
                new NewMessageBody(
                        "edited",
                        TextFormat.PLAIN,
                        List.of()
                ),
                true
        );

        String json = objectMapper.writeValueAsString(source);
        EditMessageRequest restored = objectMapper.readValue(json, EditMessageRequest.class);

        assertThat(json).contains("\"messageId\":\"m-1\"");
        assertThat(restored).isEqualTo(source);
    }

    @Test
    void shouldSerializeAndDeserializeAnswerCallbackRequest() throws Exception {
        AnswerCallbackRequest source = new AnswerCallbackRequest(new CallbackId("cb-1"), "OK", false, 5);

        String json = objectMapper.writeValueAsString(source);
        AnswerCallbackRequest restored = objectMapper.readValue(json, AnswerCallbackRequest.class);

        assertThat(json).contains("\"callbackId\":\"cb-1\"");
        assertThat(restored).isEqualTo(source);
    }

    @Test
    void shouldSerializeAndDeserializeInlineKeyboardAttachment() throws Exception {
        SendMessageRequest source = new SendMessageRequest(
                new ChatId("chat-1"),
                new NewMessageBody(
                        "choose",
                        TextFormat.PLAIN,
                        List.of(NewMessageAttachment.inlineKeyboard(new InlineKeyboardAttachment(List.of(
                                List.of(new InlineKeyboardButtonRequest("Pay", "pay:1", null)),
                                List.of(new InlineKeyboardButtonRequest("Site", null, "https://example.com"))
                        ))))
                ),
                true,
                null
        );

        String json = objectMapper.writeValueAsString(source);
        SendMessageRequest restored = objectMapper.readValue(json, SendMessageRequest.class);

        assertThat(json).contains("\"type\":\"inline_keyboard\"");
        assertThat(json).contains("\"inlineKeyboard\"");
        assertThat(restored).isEqualTo(source);
    }
}
