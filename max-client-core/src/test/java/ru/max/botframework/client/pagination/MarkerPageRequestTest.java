package ru.max.botframework.client.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class MarkerPageRequestTest {

    @Test
    void shouldCreateFirstPageWithoutMarker() {
        MarkerPageRequest request = MarkerPageRequest.firstPage(50);

        assertThat(request.limit()).isEqualTo(50);
        assertThat(request.marker()).isEmpty();
        assertThat(request.hasMarker()).isFalse();
        assertThat(request.toQueryParameters()).isEqualTo(Map.of("limit", "50"));
    }

    @Test
    void shouldIncludeMarkerInQueryParametersWhenPresent() {
        MarkerPageRequest request = new MarkerPageRequest(25, "m-123");

        assertThat(request.hasMarker()).isTrue();
        assertThat(request.toQueryParameters("page_size", "from_marker"))
                .isEqualTo(Map.of("page_size", "25", "from_marker", "m-123"));
    }

    @Test
    void shouldRejectInvalidRequest() {
        assertThatThrownBy(() -> new MarkerPageRequest(0, "marker"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("limit must be positive");

        assertThatThrownBy(() -> new MarkerPageRequest(10, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("marker");
    }
}
