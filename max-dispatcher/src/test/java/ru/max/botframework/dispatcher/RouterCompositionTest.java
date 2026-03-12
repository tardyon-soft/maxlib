package ru.max.botframework.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RouterCompositionTest {

    @Test
    void includeRouterAddsChildAndSetsParent() {
        Router parent = new Router("parent");
        Router child = new Router("child");

        parent.includeRouter(child);

        assertEquals(1, parent.children().size());
        assertEquals("child", parent.children().getFirst().name());
        assertTrue(child.parent().isPresent());
        assertEquals("parent", child.parent().orElseThrow().name());
    }

    @Test
    void includeRouterSupportsNestedComposition() {
        Router root = new Router("root");
        Router feature = new Router("feature");
        Router nested = new Router("nested");

        root.includeRouter(feature);
        feature.includeRouter(nested);

        assertEquals("feature", root.children().getFirst().name());
        assertEquals("nested", feature.children().getFirst().name());
        assertEquals("feature", nested.parent().orElseThrow().name());
    }

    @Test
    void traversalOrderIsDepthFirstPreOrder() {
        Router root = new Router("root");
        Router first = new Router("first");
        Router second = new Router("second");
        Router firstNested = new Router("firstNested");

        root.includeRouter(first);
        root.includeRouter(second);
        first.includeRouter(firstNested);

        List<String> names = root.traversalOrder().stream().map(Router::name).toList();

        assertEquals(List.of("root", "first", "firstNested", "second"), names);
    }

    @Test
    void includeRouterRejectsSelfInclusion() {
        Router router = new Router("self");

        assertThrows(IllegalArgumentException.class, () -> router.includeRouter(router));
    }

    @Test
    void includeRouterRejectsCycles() {
        Router root = new Router("root");
        Router child = new Router("child");
        root.includeRouter(child);

        assertThrows(IllegalArgumentException.class, () -> child.includeRouter(root));
    }

    @Test
    void includeRouterRejectsSecondParent() {
        Router firstParent = new Router("firstParent");
        Router secondParent = new Router("secondParent");
        Router child = new Router("child");
        firstParent.includeRouter(child);

        assertThrows(IllegalStateException.class, () -> secondParent.includeRouter(child));
    }
}

