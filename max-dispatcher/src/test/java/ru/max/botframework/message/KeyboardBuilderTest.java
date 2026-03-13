package ru.max.botframework.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import ru.max.botframework.model.MessageAttachmentType;

class KeyboardBuilderTest {

    @Test
    void buildsSingleButtonKeyboard() {
        InlineKeyboard keyboard = Keyboards.inline(k -> k.row(
                Buttons.callback("Pay", "pay:1")
        ));

        assertEquals(1, keyboard.rows().size());
        assertEquals(1, keyboard.rows().getFirst().size());
        assertEquals(InlineKeyboardButton.Kind.CALLBACK, keyboard.rows().getFirst().getFirst().kind());
        assertEquals("pay:1", keyboard.rows().getFirst().getFirst().callbackData());
    }

    @Test
    void buildsKeyboardWithMultipleRows() {
        InlineKeyboard keyboard = Keyboards.inline(k -> k
                .row(Buttons.callback("One", "v:1"), Buttons.callback("Two", "v:2"))
                .row(Buttons.link("Docs", "https://example.com/docs"))
        );

        assertEquals(2, keyboard.rows().size());
        assertEquals(2, keyboard.rows().getFirst().size());
        assertEquals(1, keyboard.rows().get(1).size());
        assertEquals(InlineKeyboardButton.Kind.LINK, keyboard.rows().get(1).getFirst().kind());
    }

    @Test
    void mapsInlineKeyboardToLowLevelAttachmentRepresentation() {
        MessageBuilder builder = Messages.text("Choose")
                .keyboard(k -> k
                        .row(Buttons.callback("Buy", "buy:1"))
                        .row(Buttons.link("Site", "https://example.com"))
                );

        var body = builder.toNewMessageBody();
        assertEquals(1, body.attachments().size());
        var keyboardAttachment = body.attachments().getFirst();
        assertEquals(MessageAttachmentType.INLINE_KEYBOARD, keyboardAttachment.type());
        assertEquals(2, keyboardAttachment.inlineKeyboard().rows().size());
        assertEquals("buy:1", keyboardAttachment.inlineKeyboard().rows().getFirst().getFirst().callbackData());
        assertEquals("https://example.com", keyboardAttachment.inlineKeyboard().rows().get(1).getFirst().url());
    }

    @Test
    void mapsRequestContactButton() {
        MessageBuilder builder = Messages.text("Choose")
                .keyboard(k -> k.row(Buttons.requestContact("Share contact")));

        var button = builder.toNewMessageBody()
                .attachments().getFirst()
                .inlineKeyboard().rows().getFirst().getFirst();
        assertEquals(Boolean.TRUE, button.requestContact());
    }

    @Test
    void mapsRequestGeoLocationButton() {
        MessageBuilder builder = Messages.text("Choose")
                .keyboard(k -> k.row(Buttons.requestGeoLocation("Share location")));

        var button = builder.toNewMessageBody()
                .attachments().getFirst()
                .inlineKeyboard().rows().getFirst().getFirst();
        assertEquals(Boolean.TRUE, button.requestGeoLocation());
    }

    @Test
    void mapsOpenAppButton() {
        MessageBuilder builder = Messages.text("Choose")
                .keyboard(k -> k.row(Buttons.openApp("Open app", "app:orders")));

        var button = builder.toNewMessageBody()
                .attachments().getFirst()
                .inlineKeyboard().rows().getFirst().getFirst();
        assertEquals("app:orders", button.openApp());
    }

    @Test
    void mapsMessageButton() {
        MessageBuilder builder = Messages.text("Choose")
                .keyboard(k -> k.row(Buttons.message("Send", "hello-from-button")));

        var button = builder.toNewMessageBody()
                .attachments().getFirst()
                .inlineKeyboard().rows().getFirst().getFirst();
        assertEquals("hello-from-button", button.message());
    }

    @Test
    void rejectsEmptyRow() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new KeyboardBuilder().row()
        );
    }

    @Test
    void rejectsMoreThanThirtyRows() {
        KeyboardBuilder builder = new KeyboardBuilder();
        IntStream.range(0, 31)
                .forEach(i -> builder.row(Buttons.callback("b" + i, "v:" + i)));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);
        assertEquals("Inline keyboard supports up to 30 rows, got 31", exception.getMessage());
    }

    @Test
    void rejectsMoreThanSevenButtonsInRow() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Keyboards.inline(k -> k.row(
                        Buttons.callback("1", "v1"),
                        Buttons.callback("2", "v2"),
                        Buttons.callback("3", "v3"),
                        Buttons.callback("4", "v4"),
                        Buttons.callback("5", "v5"),
                        Buttons.callback("6", "v6"),
                        Buttons.callback("7", "v7"),
                        Buttons.callback("8", "v8")
                ))
        );

        assertEquals("Inline keyboard row 0 supports up to 7 buttons, got 8", exception.getMessage());
    }

    @Test
    void rejectsLinkLikeRowWithMoreThanThreeButtons() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Keyboards.inline(k -> k.row(
                        Buttons.link("Docs", "https://example.com/1"),
                        Buttons.link("Site", "https://example.com/2"),
                        Buttons.link("Help", "https://example.com/3"),
                        Buttons.link("More", "https://example.com/4")
                ))
        );

        assertEquals(
                "Inline keyboard row 0 with link/open_app/request_geo_location/request_contact buttons"
                        + " supports up to 3 buttons, got 4",
                exception.getMessage()
        );
    }

    @Test
    void rejectsMoreThanTwoHundredTenButtonsInTotal() {
        KeyboardBuilder builder = new KeyboardBuilder();
        IntStream.range(0, 30)
                .forEach(row -> builder.row(
                        Buttons.callback("1", "v:" + row + ":1"),
                        Buttons.callback("2", "v:" + row + ":2"),
                        Buttons.callback("3", "v:" + row + ":3"),
                        Buttons.callback("4", "v:" + row + ":4"),
                        Buttons.callback("5", "v:" + row + ":5"),
                        Buttons.callback("6", "v:" + row + ":6"),
                        Buttons.callback("7", "v:" + row + ":7")
                ));
        builder.row(Buttons.callback("extra", "extra"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);
        assertEquals("Inline keyboard supports up to 210 buttons in total, got 211", exception.getMessage());
    }
}
