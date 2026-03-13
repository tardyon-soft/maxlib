package ru.max.botframework.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        assertEquals("pay:1", keyboard.rows().getFirst().getFirst().value());
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
    void rejectsEmptyRow() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new KeyboardBuilder().row()
        );
    }
}
