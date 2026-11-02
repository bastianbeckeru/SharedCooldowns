package com.sharedcooldowns.listener;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class CommandMatcherTest {

    private static final Set<String> COMMANDS = Set.of("home", "spawn", "warp list");
    private static final Map<String, List<String>> REGISTERED = Map.of(
        "h", List.of("home", "h"),
        "other:h", List.of("home", "h"),
        "cmi", List.of("cmi"),
        "c", List.of("cmi", "c")
    );
    private static final Function<String, Collection<String>> NAMES =
        label -> REGISTERED.getOrDefault(label, List.of());

    private static String match(String message) {
        return CommandMatcher.match(message, COMMANDS, NAMES);
    }

    @Test
    void matchesCommandWithArgumentsAndAnyCase() {
        assertEquals("home", match("/HOME base"));
    }

    @Test
    void matchesNamespaces() {
        assertEquals("home", match("/cmi:home base"));
        assertEquals("home", match("/:home"));
    }

    @Test
    void matchesRegisteredAliases() {
        assertEquals("home", match("/h base"));
        assertEquals("home", match("/other:h"));
    }

    @Test
    void matchesCmiSubcommandsAndCmiAliases() {
        assertEquals("spawn", match("/cmi spawn"));
        assertEquals("spawn", match("/c spawn"));
    }

    @Test
    void matchesMultiWordEntries() {
        assertEquals("warp list", match("/warp list 2"));
        assertNull(match("/warp nether"));
    }

    @Test
    void ignoresEverythingElse() {
        assertNull(match("/homes"));
        assertNull(match("/msg home hi"));
        assertNull(match("/"));
        assertNull(match("home"));
    }
}
