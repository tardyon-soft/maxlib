package ru.tardyon.botframework.client.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class MarkerPageTest {

    @Test
    void shouldExposeNextRequestWhenMarkerIsPresent() {
        MarkerPage<String> page = new MarkerPage<>(List.of("a", "b"), "marker-2");
        MarkerPageRequest current = MarkerPageRequest.firstPage(2);

        assertThat(page.hasNext()).isTrue();
        assertThat(page.nextPageRequest(current)).contains(new MarkerPageRequest(2, "marker-2"));
        assertThat(page.nextPageRequest(10)).contains(new MarkerPageRequest(10, "marker-2"));
    }

    @Test
    void shouldNotExposeNextRequestWhenMarkerIsBlank() {
        MarkerPage<String> page = new MarkerPage<>(List.of("a"), "");

        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextPageRequest(10)).isEmpty();
    }

    @Test
    void shouldCopyItemsAndValidateInput() {
        List<String> mutable = new java.util.ArrayList<>(List.of("x"));
        MarkerPage<String> page = new MarkerPage<>(mutable, "");
        mutable.add("y");

        assertThat(page.items()).containsExactly("x");
        assertThatThrownBy(() -> page.items().add("z")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> new MarkerPage<>(null, "n")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new MarkerPage<>(List.of(), null)).isInstanceOf(NullPointerException.class);
    }
}
