package ru.tardyon.botframework.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.client.error.MaxNotFoundException;
import ru.tardyon.botframework.client.error.MaxRateLimitException;
import ru.tardyon.botframework.client.error.MaxServiceUnavailableException;
import ru.tardyon.botframework.client.error.MaxTransportException;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.client.http.MaxHttpClient;
import ru.tardyon.botframework.client.http.MaxHttpResponse;
import ru.tardyon.botframework.client.test.MockHttpClientTestContext;
import ru.tardyon.botframework.model.BotInfo;
import ru.tardyon.botframework.model.ChatAction;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.Message;
import ru.tardyon.botframework.model.MessageId;
import ru.tardyon.botframework.model.TextFormat;
import ru.tardyon.botframework.model.CallbackId;
import ru.tardyon.botframework.model.request.AnswerCallbackRequest;
import ru.tardyon.botframework.model.request.CreateSubscriptionRequest;
import ru.tardyon.botframework.model.request.DeleteSubscriptionRequest;
import ru.tardyon.botframework.model.request.EditMessageRequest;
import ru.tardyon.botframework.model.request.GetMessagesApiRequest;
import ru.tardyon.botframework.model.request.GetChatsApiRequest;
import ru.tardyon.botframework.model.request.GetChatMembersApiRequest;
import ru.tardyon.botframework.model.request.AddChatMembersApiRequest;
import ru.tardyon.botframework.model.request.RemoveChatMemberApiRequest;
import ru.tardyon.botframework.model.request.ChatAdminGrantApi;
import ru.tardyon.botframework.model.request.AddChatAdminsApiRequest;
import ru.tardyon.botframework.model.request.PinChatMessageApiRequest;
import ru.tardyon.botframework.model.request.GetUpdatesRequest;
import ru.tardyon.botframework.model.request.InlineKeyboardAttachment;
import ru.tardyon.botframework.model.request.InlineKeyboardButtonRequest;
import ru.tardyon.botframework.model.request.NewMessageBody;
import ru.tardyon.botframework.model.request.NewMessageAttachment;
import ru.tardyon.botframework.model.request.PrepareUploadApiRequest;
import ru.tardyon.botframework.model.request.SendMessageRequest;
import ru.tardyon.botframework.model.request.SendMessageApiRequest;
import ru.tardyon.botframework.model.request.UploadType;
import ru.tardyon.botframework.model.request.UpdateChatApiRequest;
import ru.tardyon.botframework.model.response.GetUpdatesResponse;
import ru.tardyon.botframework.model.UpdateEventType;
import ru.tardyon.botframework.model.Subscription;
import ru.tardyon.botframework.model.transport.ApiMessage;
import ru.tardyon.botframework.model.transport.ApiChat;
import ru.tardyon.botframework.model.transport.ApiChatsResponse;
import ru.tardyon.botframework.model.transport.ApiChatMembersResponse;
import ru.tardyon.botframework.model.transport.ApiChatPinResponse;
import ru.tardyon.botframework.model.transport.ApiUploadResponse;
import ru.tardyon.botframework.model.transport.ApiUser;
import ru.tardyon.botframework.model.transport.ApiVideoInfo;

class DefaultMaxBotClientTest {

    private MockHttpClientTestContext http;
    private DefaultMaxBotClient client;

    @BeforeEach
    void setUp() {
        http = MockHttpClientTestContext.start();
        client = http.createClient(RetryPolicy.none());
    }

    @AfterEach
    void tearDown() {
        http.close();
    }

    @Test
    void shouldExecuteGetWithoutBodyAndDeserializeJsonResponse() throws Exception {
        http.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"ok\":true,\"message\":\"pong\"}"));

        EchoResponse response = client.execute(new EchoRequest(HttpMethod.GET, null));

        RecordedRequest recorded = http.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("GET");
        assertThat(recorded.getPath()).isEqualTo("/v1/ping?limit=10");
        assertThat(recorded.getHeader("Authorization")).isEqualTo("test-token");
        assertThat(recorded.getHeaders().values("User-Agent")).contains("max-client-core-test/1.0");
        assertThat(recorded.getBodySize()).isZero();

        assertThat(response.ok()).isTrue();
        assertThat(response.message()).isEqualTo("pong");
    }

    @Test
    void shouldSerializeJsonBodyAndSupportMutatingHttpMethods() throws Exception {
        for (HttpMethod method : new HttpMethod[]{HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE}) {
            http.enqueue(new MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"ok\":true,\"message\":\"" + method + "\"}"));

            EchoResponse response = client.execute(new EchoRequest(method, new Payload("hello")));

            RecordedRequest recorded = http.takeRequest();
            assertThat(recorded.getMethod()).isEqualTo(method.name());
            assertThat(recorded.getPath()).isEqualTo("/v1/ping?limit=10");
            assertThat(recorded.getHeader("Content-Type")).startsWith("application/json");
            assertThat(recorded.getHeader("Authorization")).isEqualTo("test-token");
            assertThat(recorded.getHeaders().values("User-Agent")).contains("max-client-core-test/1.0");
            assertThat(recorded.getBody().readUtf8()).contains("\"value\":\"hello\"");
            assertThat(response.message()).isEqualTo(method.name());
        }
    }

    @Test
    void shouldMap429ToRateLimitException() {
        http.enqueue(new MockResponse()
                .setResponseCode(429)
                .setHeader("Retry-After", "7")
                .setBody("{\"error_code\":\"RATE_LIMIT\",\"message\":\"Slow down\",\"details\":{\"scope\":\"global\"}}"));

        assertThatThrownBy(() -> client.execute(new EchoRequest(HttpMethod.GET, null)))
                .isInstanceOf(MaxRateLimitException.class)
                .satisfies(ex -> {
                    MaxRateLimitException exception = (MaxRateLimitException) ex;
                    assertThat(exception.statusCode()).isEqualTo(429);
                    assertThat(exception.retryAfterSeconds()).isEqualTo(7L);
                    assertThat(exception.responseBody()).contains("RATE_LIMIT");
                    assertThat(exception.errorPayload().errorCode()).isEqualTo("RATE_LIMIT");
                    assertThat(exception.errorPayload().message()).isEqualTo("Slow down");
                });
    }

    @Test
    void shouldMap404ToNotFoundException() {
        http.enqueue(new MockResponse().setResponseCode(404).setBody("{\"error\":\"not_found\"}"));

        assertThatThrownBy(() -> client.execute(new EchoRequest(HttpMethod.GET, null)))
                .isInstanceOf(MaxNotFoundException.class);
    }

    @Test
    void shouldCallGetMeAndReturnTypedBotInfo() throws Exception {
        http.enqueueJsonFixture("bot-info-response.json");

        BotInfo botInfo = client.getMe();

        RecordedRequest recorded = http.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("GET");
        assertThat(recorded.getPath()).isEqualTo("/me");
        assertThat(recorded.getHeader("Authorization")).isEqualTo("test-token");

        assertThat(botInfo.id().value()).isEqualTo("b-1");
        assertThat(botInfo.username()).isEqualTo("max_helper_bot");
        assertThat(botInfo.displayName()).isEqualTo("MAX Helper Bot");
    }

    @Test
    void shouldCallGetMeApiAndReturnTransportUser() throws Exception {
        http.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "user_id": 101,
                          "first_name": "MAX",
                          "last_name": "Bot",
                          "username": "max_helper_bot",
                          "is_bot": true
                        }
                        """));

        ApiUser me = client.getMeApi();

        RecordedRequest recorded = http.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("GET");
        assertThat(recorded.getPath()).isEqualTo("/me");
        assertThat(me.userId()).isEqualTo(101L);
        assertThat(me.firstName()).isEqualTo("MAX");
        assertThat(me.username()).isEqualTo("max_helper_bot");
        assertThat(me.isBot()).isTrue();
    }

    @Test
    void shouldSendMessageViaDomainMethod() throws Exception {
        http.enqueueJsonFixture("message-envelope-response.json");

        Message message = client.sendMessage(new SendMessageRequest(
                new ChatId("c-100"),
                new NewMessageBody("hello", TextFormat.PLAIN, List.of()),
                false,
                null
        ));

        RecordedRequest recorded = http.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getPath()).isEqualTo("/messages?chat_id=c-100");
        String body = recorded.getBody().readUtf8();
        assertThat(body).contains("\"text\":\"hello\"");
        assertThat(body).contains("\"notify\":false");
        assertThat(message.messageId().value()).isEqualTo("m-101");
    }

    @Test
    void shouldSendMessageViaDomainMethodWhenApiReturnsRawTransportMessage() throws Exception {
        http.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "mid": "m-raw-1",
                          "timestamp": 1735689600,
                          "recipient": {"chat_id": 247923392, "chat_type": "dialog"},
                          "sender": {"user_id": 1001, "first_name": "Alice", "is_bot": false},
                          "body": {"text": "hello raw"}
                        }
                        """));

        Message message = client.sendMessage(new SendMessageRequest(
                new ChatId("247923392"),
                new NewMessageBody("hello raw", TextFormat.PLAIN, List.of()),
                false,
                null
        ));

        RecordedRequest recorded = http.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getPath()).isEqualTo("/messages?chat_id=247923392");
        assertThat(message.messageId().value()).isEqualTo("m-raw-1");
        assertThat(message.text()).isEqualTo("hello raw");
    }

    @Test
    void shouldSendMessageViaDocsShapedMethod() throws Exception {
        http.enqueueJsonFixture("message-envelope-response.json");

        Message message = client.sendMessageApi(new SendMessageApiRequest(
                777L,
                null,
                false,
                new NewMessageBody("hello docs", TextFormat.PLAIN, List.of()),
                false,
                new MessageId("m-100")
        ));

        RecordedRequest recorded = http.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getPath()).contains("/messages?");
        assertThat(recorded.getPath()).contains("user_id=777");
        assertThat(recorded.getPath()).contains("disable_link_preview=false");
        String body = recorded.getBody().readUtf8();
        assertThat(body).contains("\"text\":\"hello docs\"");
        assertThat(body).contains("\"notify\":false");
        assertThat(body).contains("\"link\":{\"type\":\"reply\",\"message\":\"m-100\"}");
        assertThat(message.messageId().value()).isEqualTo("m-101");
    }

    @Test
    void shouldSerializeInlineKeyboardAsPayloadAttachment() throws Exception {
        http.enqueueJsonFixture("message-envelope-response.json");

        NewMessageAttachment inlineKeyboard = NewMessageAttachment.inlineKeyboard(
                new InlineKeyboardAttachment(List.of(
                        List.of(new InlineKeyboardButtonRequest("Pay", "menu:pay", null, null, null, null, null))
                ))
        );
        Message message = client.sendMessage(new SendMessageRequest(
                new ChatId("c-100"),
                new NewMessageBody("menu", TextFormat.PLAIN, List.of(inlineKeyboard)),
                false,
                null
        ));

        RecordedRequest recorded = http.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("POST");
        String body = recorded.getBody().readUtf8();
        assertThat(body).contains("\"type\":\"inline_keyboard\"");
        assertThat(body).contains("\"payload\":{\"buttons\":[[{\"type\":\"callback\",\"text\":\"Pay\",\"payload\":\"menu:pay\"");
        assertThat(message.messageId().value()).isEqualTo("m-101");
    }

    @Test
    void shouldEditMessageViaDomainMethod() {
        http.enqueueJsonFixture("operation-success-response.json");

        boolean success = client.editMessage(new EditMessageRequest(
                new ChatId("c-100"),
                new MessageId("m-101"),
                new NewMessageBody("edited", TextFormat.MARKDOWN, List.of()),
                true
        ));

        RecordedRequest recorded = http.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("PUT");
        assertThat(recorded.getPath()).isEqualTo("/messages?message_id=m-101");
        assertThat(recorded.getBody().readUtf8()).contains("\"text\":\"edited\"");
        assertThat(success).isTrue();
    }

    @Test
    void shouldDeleteMessageViaDomainMethod() {
        http.enqueueJsonFixture("operation-success-response.json");

        boolean success = client.deleteMessage(new MessageId("m-102"));

        RecordedRequest recorded = http.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("DELETE");
        assertThat(recorded.getPath()).isEqualTo("/messages?message_id=m-102");
        assertThat(success).isTrue();
    }

    @Test
    void shouldGetMessageViaDomainMethod() {
        http.enqueueJsonFixture("message-single-response.json");

        Message message = client.getMessage(new MessageId("m-200"));

        RecordedRequest recorded = http.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("GET");
        assertThat(recorded.getPath()).isEqualTo("/messages/m-200");
        assertThat(message.text()).isEqualTo("single");
    }

    @Test
    void shouldGetMessageViaApiMethod() {
        http.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "message_id": 200,
                          "sender": {
                            "user_id": 10,
                            "first_name": "Alice",
                            "username": "alice",
                            "is_bot": false
                          },
                          "recipient": {
                            "chat_id": 100,
                            "chat_type": "chat"
                          },
                          "timestamp": 1700000000,
                          "body": {
                            "text": "single"
                          }
                        }
                        """));

        ApiMessage message = client.getMessageApi(new MessageId("m-200"));

        RecordedRequest recorded = http.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("GET");
        assertThat(recorded.getPath()).isEqualTo("/messages/m-200");
        assertThat(message.messageId()).isEqualTo("200");
        assertThat(message.body()).isNotNull();
        assertThat(message.body().text()).isEqualTo("single");
    }

    @Test
    void shouldGetMessagesByChatIdViaDomainMethod() {
        http.enqueueJsonFixture("messages-list-response.json");

        List<Message> messages = client.getMessages(new ChatId("c-100"));

        RecordedRequest recorded = http.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("GET");
        assertThat(recorded.getPath()).isEqualTo("/messages?chat_id=c-100");
        assertThat(messages).hasSize(2);
        assertThat(messages.getFirst().messageId().value()).isEqualTo("m-1");
    }

    @Test
    void shouldGetMessagesByIdsViaDomainMethod() {
        http.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"messages":[]}
                        """));

        List<Message> messages = client.getMessages(List.of(new MessageId("m-1"), new MessageId("m-2")));

        RecordedRequest recorded = http.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("GET");
        assertThat(recorded.getPath()).isEqualTo("/messages?message_ids=m-1%2Cm-2");
        assertThat(messages).isEmpty();
    }

    @Test
    void shouldGetMessagesViaDocsShapedMethod() {
        http.enqueueJsonFixture("messages-list-response.json");

        List<Message> messages = client.getMessagesApi(new GetMessagesApiRequest(
                123L,
                1000L,
                2000L,
                25
        ));

        RecordedRequest recorded = http.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("GET");
        assertThat(recorded.getPath()).contains("/messages?");
        assertThat(recorded.getPath()).contains("chat_id=123");
        assertThat(recorded.getPath()).contains("from=1000");
        assertThat(recorded.getPath()).contains("to=2000");
        assertThat(recorded.getPath()).contains("count=25");
        assertThat(messages).hasSize(2);
    }

    @Test
    void shouldGetChatsViaDocsShapedMethod() {
        http.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "chats": [
                            {
                              "chat_id": 100,
                              "type": "chat",
                              "status": "active",
                              "title": "Team Alpha",
                              "last_event_time": 1700000000,
                              "participants_count": 10,
                              "is_public": false
                            }
                          ],
                          "marker": 200
                        }
                        """));

        ApiChatsResponse response = client.getChatsApi(new GetChatsApiRequest(150L, 20));

        RecordedRequest recorded = http.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("GET");
        assertThat(recorded.getPath()).contains("/chats?");
        assertThat(recorded.getPath()).contains("marker=150");
        assertThat(recorded.getPath()).contains("count=20");
        assertThat(response.marker()).isEqualTo(200L);
        assertThat(response.chats()).hasSize(1);
        assertThat(response.chats().getFirst().chatId()).isEqualTo(100L);
    }

    @Test
    void shouldGetChatViaDocsShapedMethod() {
        http.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "chat_id": 100,
                          "type": "chat",
                          "status": "active",
                          "title": "Team Alpha",
                          "last_event_time": 1700000000,
                          "participants_count": 10,
                          "is_public": false
                        }
                        """));

        ApiChat chat = client.getChatApi(100L);

        RecordedRequest recorded = http.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("GET");
        assertThat(recorded.getPath()).isEqualTo("/chats/100");
        assertThat(chat.chatId()).isEqualTo(100L);
        assertThat(chat.type()).isEqualTo("chat");
        assertThat(chat.title()).isEqualTo("Team Alpha");
    }

    @Test
    void shouldPrepareUploadViaDocsShapedMethod() {
        http.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "url": "https://upload.max.ru/session-1",
                          "token": "upload-token-1"
                        }
                        """));

        ApiUploadResponse response = client.prepareUploadApi(new PrepareUploadApiRequest(UploadType.VIDEO));

        RecordedRequest recorded = http.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getPath()).isEqualTo("/uploads?type=video");
        assertThat(recorded.getBody().readUtf8()).isEmpty();
        assertThat(response.url()).isEqualTo("https://upload.max.ru/session-1");
        assertThat(response.token()).isEqualTo("upload-token-1");
    }

    @Test
    void shouldGetVideoViaDocsShapedMethod() {
        http.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "token": "video-token-1",
                          "urls": {
                            "mp4_720": "https://cdn.max.ru/video/1-720.mp4"
                          },
                          "width": 1280,
                          "height": 720,
                          "duration": 63
                        }
                        """));

        ApiVideoInfo video = client.getVideoApi("video-token-1");

        RecordedRequest recorded = http.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("GET");
        assertThat(recorded.getPath()).isEqualTo("/videos/video-token-1");
        assertThat(video.token()).isEqualTo("video-token-1");
        assertThat(video.urls()).containsKey("mp4_720");
        assertThat(video.duration()).isEqualTo(63);
    }

    @Test
    void shouldGetChatMembersViaDocsShapedMethod() {
        http.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "members": [
                            { "user_id": 11, "chat_id": 100, "status": "member" }
                          ],
                          "marker": 555
                        }
                        """));

        ApiChatMembersResponse response = client.getChatMembersApi(100L, new GetChatMembersApiRequest(List.of(11L, 12L), 500L, 20));

        RecordedRequest recorded = http.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("GET");
        assertThat(recorded.getPath()).contains("/chats/100/members?");
        assertThat(recorded.getPath()).contains("user_ids=11%2C12");
        assertThat(recorded.getPath()).contains("marker=500");
        assertThat(recorded.getPath()).contains("count=20");
        assertThat(response.members()).hasSize(1);
        assertThat(response.marker()).isEqualTo(555L);
    }

    @Test
    void shouldManageChatMembersViaDocsShapedMethods() {
        http.enqueueJsonFixture("operation-success-response.json");
        http.enqueueJsonFixture("operation-success-response.json");

        boolean added = client.addChatMembersApi(100L, new AddChatMembersApiRequest(List.of(11L, 12L)));
        boolean removed = client.removeChatMemberApi(100L, new RemoveChatMemberApiRequest(11L, true));

        RecordedRequest add = http.takeRequest();
        assertThat(add.getMethod()).isEqualTo("POST");
        assertThat(add.getPath()).isEqualTo("/chats/100/members");
        assertThat(add.getBody().readUtf8()).contains("\"user_ids\":[11,12]");

        RecordedRequest remove = http.takeRequest();
        assertThat(remove.getMethod()).isEqualTo("DELETE");
        assertThat(remove.getPath()).contains("/chats/100/members?");
        assertThat(remove.getPath()).contains("user_id=11");
        assertThat(remove.getPath()).contains("block=true");

        assertThat(added).isTrue();
        assertThat(removed).isTrue();
    }

    @Test
    void shouldManageChatAdminsViaDocsShapedMethods() {
        http.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "admins": [
                            { "user_id": 7, "chat_id": 100, "status": "admin" }
                          ]
                        }
                        """));
        http.enqueueJsonFixture("operation-success-response.json");
        http.enqueueJsonFixture("operation-success-response.json");

        ApiChatMembersResponse admins = client.getChatAdminsApi(100L);
        boolean added = client.addChatAdminsApi(
                100L,
                new AddChatAdminsApiRequest(List.of(new ChatAdminGrantApi(7L, List.of("write", "pin"), "moderator")))
        );
        boolean removed = client.removeChatAdminApi(100L, 7L);

        RecordedRequest get = http.takeRequest();
        assertThat(get.getMethod()).isEqualTo("GET");
        assertThat(get.getPath()).isEqualTo("/chats/100/members/admins");

        RecordedRequest add = http.takeRequest();
        assertThat(add.getMethod()).isEqualTo("POST");
        assertThat(add.getPath()).isEqualTo("/chats/100/members/admins");
        assertThat(add.getBody().readUtf8()).contains("\"user_id\":7");

        RecordedRequest remove = http.takeRequest();
        assertThat(remove.getMethod()).isEqualTo("DELETE");
        assertThat(remove.getPath()).isEqualTo("/chats/100/members/admins/7");

        assertThat(admins.admins()).hasSize(1);
        assertThat(added).isTrue();
        assertThat(removed).isTrue();
    }

    @Test
    void shouldManageOwnMembershipViaDocsShapedMethods() {
        http.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "user_id": 1,
                          "first_name": "Bot",
                          "username": "max_helper_bot",
                          "is_bot": true
                        }
                        """));
        http.enqueueJsonFixture("operation-success-response.json");

        ApiUser me = client.getMyChatMembershipApi(100L);
        boolean left = client.leaveChatApi(100L);

        RecordedRequest get = http.takeRequest();
        assertThat(get.getMethod()).isEqualTo("GET");
        assertThat(get.getPath()).isEqualTo("/chats/100/members/me");

        RecordedRequest leave = http.takeRequest();
        assertThat(leave.getMethod()).isEqualTo("DELETE");
        assertThat(leave.getPath()).isEqualTo("/chats/100/members/me");

        assertThat(me.userId()).isEqualTo(1L);
        assertThat(left).isTrue();
    }

    @Test
    void shouldManageChatPinViaDocsShapedMethods() {
        http.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "message": {
                            "message_id": 200,
                            "timestamp": 1700000000
                          }
                        }
                        """));
        http.enqueueJsonFixture("operation-success-response.json");
        http.enqueueJsonFixture("operation-success-response.json");

        ApiChatPinResponse pin = client.getChatPinApi(100L);
        boolean pinned = client.pinChatMessageApi(100L, new PinChatMessageApiRequest("200", false));
        boolean unpinned = client.unpinChatMessageApi(100L);

        RecordedRequest get = http.takeRequest();
        assertThat(get.getMethod()).isEqualTo("GET");
        assertThat(get.getPath()).isEqualTo("/chats/100/pin");

        RecordedRequest put = http.takeRequest();
        assertThat(put.getMethod()).isEqualTo("PUT");
        assertThat(put.getPath()).isEqualTo("/chats/100/pin");
        assertThat(put.getBody().readUtf8()).contains("\"message_id\":\"200\"");

        RecordedRequest delete = http.takeRequest();
        assertThat(delete.getMethod()).isEqualTo("DELETE");
        assertThat(delete.getPath()).isEqualTo("/chats/100/pin");

        assertThat(pin.message()).isNotNull();
        assertThat(pinned).isTrue();
        assertThat(unpinned).isTrue();
    }

    @Test
    void shouldPatchAndDeleteChatViaDocsShapedMethods() {
        http.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "chat_id": 100,
                          "type": "chat",
                          "title": "New Title"
                        }
                        """));
        http.enqueueJsonFixture("operation-success-response.json");

        ApiChat patched = client.patchChatApi(100L, new UpdateChatApiRequest(Map.of(), "New Title", true, false));
        boolean deleted = client.deleteChatApi(100L);

        RecordedRequest patch = http.takeRequest();
        assertThat(patch.getMethod()).isEqualTo("PATCH");
        assertThat(patch.getPath()).isEqualTo("/chats/100");
        assertThat(patch.getBody().readUtf8()).contains("\"title\":\"New Title\"");

        RecordedRequest delete = http.takeRequest();
        assertThat(delete.getMethod()).isEqualTo("DELETE");
        assertThat(delete.getPath()).isEqualTo("/chats/100");

        assertThat(patched.chatId()).isEqualTo(100L);
        assertThat(deleted).isTrue();
    }

    @Test
    void shouldAnswerCallbackViaDomainMethod() {
        http.enqueueJsonFixture("operation-success-response.json");

        boolean success = client.answerCallback(new AnswerCallbackRequest(
                new CallbackId("cb-1"),
                "OK",
                false,
                5
        ));

        RecordedRequest recorded = http.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getPath()).isEqualTo("/answers?callback_id=cb-1");
        String body = recorded.getBody().readUtf8();
        assertThat(body).contains("\"message\":{");
        assertThat(body).contains("\"text\":\"OK\"");
        assertThat(body).contains("\"format\":\"plain\"");
        assertThat(body).contains("\"attachments\":[]");
        assertThat(body).doesNotContain("\"notification\":");
        assertThat(success).isTrue();
    }

    @Test
    void shouldAnswerCallbackWithNotificationTextWhenNotificationModeEnabled() {
        http.enqueueJsonFixture("operation-success-response.json");

        boolean success = client.answerCallback(new AnswerCallbackRequest(
                new CallbackId("cb-2"),
                "Done",
                true,
                0
        ));

        RecordedRequest recorded = http.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getPath()).isEqualTo("/answers?callback_id=cb-2");
        String body = recorded.getBody().readUtf8();
        assertThat(body).contains("\"notification\":\"Done\"");
        assertThat(body).doesNotContain("\"message\":");
        assertThat(success).isTrue();
    }

    @Test
    void shouldSendChatActionViaDomainMethod() {
        http.enqueueJsonFixture("operation-success-response.json");

        boolean success = client.sendChatAction(new ChatId("chat-1"), ChatAction.TYPING);

        RecordedRequest recorded = http.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getPath()).isEqualTo("/chats/chat-1/actions");
        String body = recorded.getBody().readUtf8();
        assertThat(body).contains("\"action\":\"typing_on\"");
        assertThat(success).isTrue();
    }

    @Test
    void shouldGetUpdatesViaDomainMethodWithPollingParams() {
        http.enqueueJsonFixture("updates-response.json");

        GetUpdatesResponse response = client.getUpdates(new GetUpdatesRequest(
                100L,
                30,
                50,
                List.of(UpdateEventType.MESSAGE_CREATED, UpdateEventType.MESSAGE_CALLBACK)
        ));

        RecordedRequest recorded = http.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("GET");
        assertThat(recorded.getPath()).contains("/updates?");
        assertThat(recorded.getPath()).contains("marker=100");
        assertThat(recorded.getPath()).contains("timeout=30");
        assertThat(recorded.getPath()).contains("limit=50");
        assertThat(recorded.getPath()).contains("types=message_created%2Cmessage_callback");
        assertThat(response.marker()).isEqualTo(101L);
        assertThat(response.updates()).hasSize(1);
        assertThat(response.updates().getFirst().updateId().value()).isEqualTo("upd-1");
    }

    @Test
    void shouldGetWebhookSubscriptions() {
        http.enqueueJsonFixture("subscriptions-response.json");

        List<Subscription> subscriptions = client.getSubscriptions();

        RecordedRequest recorded = http.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("GET");
        assertThat(recorded.getPath()).isEqualTo("/subscriptions");
        assertThat(subscriptions).hasSize(1);
        assertThat(subscriptions.getFirst().url()).isEqualTo("https://example.com/webhook");
        assertThat(subscriptions.getFirst().updateTypes())
                .containsExactly(UpdateEventType.MESSAGE_CREATED, UpdateEventType.MESSAGE_CALLBACK);
    }

    @Test
    void shouldCreateWebhookSubscription() {
        http.enqueueJsonFixture("operation-success-response.json");

        boolean success = client.createSubscription(new CreateSubscriptionRequest(
                "https://example.com/webhook",
                List.of(UpdateEventType.MESSAGE_CREATED),
                "secret-1"
        ));

        RecordedRequest recorded = http.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getPath()).isEqualTo("/subscriptions");
        String body = recorded.getBody().readUtf8();
        assertThat(body).contains("\"url\":\"https://example.com/webhook\"");
        assertThat(body).contains("\"update_types\":[\"message_created\"]");
        assertThat(body).contains("\"secret\":\"secret-1\"");
        assertThat(success).isTrue();
    }

    @Test
    void shouldDeleteWebhookSubscription() {
        http.enqueueJsonFixture("operation-success-response.json");

        boolean success = client.deleteSubscription(new DeleteSubscriptionRequest("https://example.com/webhook"));

        RecordedRequest recorded = http.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("DELETE");
        assertThat(recorded.getPath()).isEqualTo("/subscriptions?url=https%3A%2F%2Fexample.com%2Fwebhook");
        assertThat(success).isTrue();
    }

    @Test
    void shouldRetrySafeGetForTransientStatusWhenPolicyAllows() throws Exception {
        MaxApiClientConfig config = http.buildConfig(RetryPolicy.fixed(2, Duration.ZERO));
        client = http.createClient(config, http.createTransport(config));

        http.enqueue(new MockResponse().setResponseCode(503).setBody("{\"error\":\"service_unavailable\"}"));
        http.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"ok\":true,\"message\":\"recovered\"}"));

        EchoResponse response = client.execute(new EchoRequest(HttpMethod.GET, null));

        assertThat(response.message()).isEqualTo("recovered");
        RecordedRequest first = http.takeRequest();
        RecordedRequest second = http.takeRequest();
        assertThat(first.getMethod()).isEqualTo("GET");
        assertThat(second.getMethod()).isEqualTo("GET");
    }

    @Test
    void shouldNotRetryUnsafeMethodsByDefault() {
        MaxApiClientConfig config = http.buildConfig(RetryPolicy.fixed(3, Duration.ZERO));
        client = http.createClient(config, http.createTransport(config));
        http.enqueue(new MockResponse().setResponseCode(503).setBody("{\"error\":\"service_unavailable\"}"));

        assertThatThrownBy(() -> client.execute(new EchoRequest(HttpMethod.POST, new Payload("x"))))
                .isInstanceOf(MaxServiceUnavailableException.class);

        assertThat(http.requestCount()).isEqualTo(1);
    }

    @Test
    void shouldRetryTransportFailureForSafeMethods() {
        AtomicInteger attempts = new AtomicInteger();
        MaxHttpClient failingTransport = request -> {
            if (attempts.incrementAndGet() == 1) {
                throw new MaxTransportException("temporary failure", new IOException("io"));
            }
            return new MaxHttpResponse(
                    200,
                    Map.of("Content-Type", List.of("application/json")),
                    "{\"ok\":true,\"message\":\"after-retry\"}".getBytes()
            );
        };

        MaxApiClientConfig config = http.buildConfig(RetryPolicy.fixed(2, Duration.ZERO));
        client = http.createClient(config, failingTransport);

        EchoResponse response = client.execute(new EchoRequest(HttpMethod.GET, null));

        assertThat(response.message()).isEqualTo("after-retry");
        assertThat(attempts.get()).isEqualTo(2);
    }

    @Test
    void shouldNotifyRateLimiterWhen429IsReceived() {
        CapturingRateLimiter limiter = new CapturingRateLimiter();
        MaxApiClientConfig config = http.buildConfig(RetryPolicy.none(), limiter);
        client = http.createClient(config, http.createTransport(config));
        http.enqueue(new MockResponse().setResponseCode(429).setHeader("Retry-After", "3").setBody("{\"error\":\"rate_limit\"}"));

        assertThatThrownBy(() -> client.execute(new EchoRequest(HttpMethod.GET, null)))
                .isInstanceOf(MaxRateLimitException.class);

        assertThat(limiter.beforeRequestCalls.get()).isEqualTo(1);
        assertThat(limiter.rateLimitedCalls.get()).isEqualTo(1);
        assertThat(limiter.lastRetryAfterSeconds.get()).isEqualTo(3L);
    }

    @Test
    void shouldCallRateLimiterBeforeEachRetryAttempt() {
        CapturingRateLimiter limiter = new CapturingRateLimiter();
        MaxApiClientConfig config = http.buildConfig(RetryPolicy.fixed(2, Duration.ZERO), limiter);
        client = http.createClient(config, http.createTransport(config));

        http.enqueue(new MockResponse().setResponseCode(503).setBody("{\"error\":\"temporary\"}"));
        http.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"ok\":true,\"message\":\"done\"}"));

        EchoResponse response = client.execute(new EchoRequest(HttpMethod.GET, null));

        assertThat(response.message()).isEqualTo("done");
        assertThat(limiter.beforeRequestCalls.get()).isEqualTo(2);
    }

    @Test
    void shouldRespectRetryAfterHeaderWhenRetrying429() {
        MaxApiClientConfig config = http.buildConfig(RetryPolicy.fixed(2, Duration.ZERO));
        client = http.createClient(config, http.createTransport(config));
        http.enqueue(new MockResponse().setResponseCode(429).setHeader("Retry-After", "1").setBody("{\"error\":\"rate_limit\"}"));
        http.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"ok\":true,\"message\":\"after-rate-limit\"}"));

        long startedAt = System.currentTimeMillis();
        EchoResponse response = client.execute(new EchoRequest(HttpMethod.GET, null));
        long elapsedMillis = System.currentTimeMillis() - startedAt;

        assertThat(response.message()).isEqualTo("after-rate-limit");
        assertThat(elapsedMillis).isGreaterThanOrEqualTo(800L);
    }

    private static final class CapturingRateLimiter implements RequestRateLimiter {
        private final AtomicInteger beforeRequestCalls = new AtomicInteger();
        private final AtomicInteger rateLimitedCalls = new AtomicInteger();
        private final AtomicLong lastRetryAfterSeconds = new AtomicLong(-1);

        @Override
        public void beforeRequest(ru.tardyon.botframework.client.http.MaxHttpRequest request) {
            beforeRequestCalls.incrementAndGet();
        }

        @Override
        public void onRateLimited(
                ru.tardyon.botframework.client.http.MaxHttpRequest request,
                MaxHttpResponse response,
                Long retryAfterSeconds
        ) {
            rateLimitedCalls.incrementAndGet();
            lastRetryAfterSeconds.set(retryAfterSeconds == null ? -1 : retryAfterSeconds);
        }
    }

    private record EchoRequest(HttpMethod method, Object payload) implements MaxRequest<EchoResponse> {

        @Override
        public String path() {
            return "/v1/ping";
        }

        @Override
        public Class<EchoResponse> responseType() {
            return EchoResponse.class;
        }

        @Override
        public java.util.Optional<Object> body() {
            return java.util.Optional.ofNullable(payload);
        }

        @Override
        public Map<String, String> queryParameters() {
            return Map.of("limit", "10");
        }
    }

    private record EchoResponse(boolean ok, String message) {
    }

    private record Payload(String value) {
    }
}
