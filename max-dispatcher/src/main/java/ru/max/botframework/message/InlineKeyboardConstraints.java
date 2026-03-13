package ru.max.botframework.message;

import java.util.List;

/**
 * MAX inline keyboard limits validation for high-level keyboard API.
 */
final class InlineKeyboardConstraints {
    static final int MAX_TOTAL_BUTTONS = 210;
    static final int MAX_ROWS = 30;
    static final int MAX_BUTTONS_PER_ROW = 7;
    static final int MAX_BUTTONS_PER_ROW_FOR_LINK_LIKE = 3;
    static final int MAX_LINK_URL_LENGTH = 2048;

    private InlineKeyboardConstraints() {
    }

    static void validateLayout(List<List<InlineKeyboardButton>> rows) {
        if (rows.size() > MAX_ROWS) {
            throw new IllegalArgumentException(
                    "Inline keyboard supports up to %d rows, got %d".formatted(MAX_ROWS, rows.size())
            );
        }

        int totalButtons = 0;
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<InlineKeyboardButton> row = rows.get(rowIndex);
            if (row.size() > MAX_BUTTONS_PER_ROW) {
                throw new IllegalArgumentException(
                        "Inline keyboard row %d supports up to %d buttons, got %d"
                                .formatted(rowIndex, MAX_BUTTONS_PER_ROW, row.size())
                );
            }

            if (containsLinkLikeButton(row) && row.size() > MAX_BUTTONS_PER_ROW_FOR_LINK_LIKE) {
                throw new IllegalArgumentException(
                        "Inline keyboard row %d with link/open_app/request_geo_location/request_contact buttons"
                                + " supports up to %d buttons, got %d"
                                .formatted(rowIndex, MAX_BUTTONS_PER_ROW_FOR_LINK_LIKE, row.size())
                );
            }

            totalButtons += row.size();
            if (totalButtons > MAX_TOTAL_BUTTONS) {
                throw new IllegalArgumentException(
                        "Inline keyboard supports up to %d buttons in total, got %d"
                                .formatted(MAX_TOTAL_BUTTONS, totalButtons)
                );
            }
        }
    }

    private static boolean containsLinkLikeButton(List<InlineKeyboardButton> row) {
        for (InlineKeyboardButton button : row) {
            switch (button.kind()) {
                case LINK, OPEN_APP, REQUEST_GEO_LOCATION, REQUEST_CONTACT -> {
                    return true;
                }
                default -> {
                    // skip
                }
            }
        }
        return false;
    }
}
