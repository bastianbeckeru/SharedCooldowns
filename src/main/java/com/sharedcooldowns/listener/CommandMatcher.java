package com.sharedcooldowns.listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/**
 * Finds which configured command a typed message refers to. Handles arguments,
 * multi-word entries ("cmi home"), namespaces ("/cmi:home"), registered aliases
 * and CMI subcommands ("/cmi home" for a configured "home").
 */
final class CommandMatcher {

    private CommandMatcher() {}

    /**
     * @param message   the raw message, including the leading '/'
     * @param commands  normalized configured commands
     * @param namesOf   the registered name and aliases of a label, empty if unknown
     * @return the configured command, or null if there is no match
     */
    static String match(String message, Set<String> commands, Function<String, Collection<String>> namesOf) {
        if (message == null || !message.startsWith("/")) {
            return null;
        }
        String clean = message.substring(1).trim().toLowerCase(Locale.ROOT);
        if (clean.isEmpty()) {
            return null;
        }
        List<String> tokens = Arrays.asList(clean.split("\\s+"));
        String label = tokens.get(0);
        List<String> args = tokens.subList(1, tokens.size());

        Set<String> heads = new LinkedHashSet<>();
        heads.add(label.substring(label.indexOf(':') + 1));
        for (String name : namesOf.apply(label)) {
            heads.add(name.toLowerCase(Locale.ROOT));
        }

        for (String head : heads) {
            List<String> candidate = new ArrayList<>();
            candidate.add(head);
            candidate.addAll(args);
            String found = longestPrefix(candidate, commands);
            if (found != null) {
                return found;
            }
        }
        return heads.contains("cmi") ? longestPrefix(args, commands) : null;
    }

    private static String longestPrefix(List<String> tokens, Set<String> commands) {
        for (int length = tokens.size(); length >= 1; length--) {
            String candidate = String.join(" ", tokens.subList(0, length));
            if (commands.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }
}
