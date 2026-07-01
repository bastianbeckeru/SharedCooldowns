package com.sharedcooldowns.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Utility for translating legacy '&' color codes into Adventure Components.
 */
public final class ColorUtil {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private ColorUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Translates a string with '&' color codes into an Adventure {@link Component}.
     *
     * @param text The text with color codes.
     * @return The colorized Component.
     */
    public static Component colorize(String text) {
        if (text == null) {
            return Component.empty();
        }
        return SERIALIZER.deserialize(text);
    }
}
