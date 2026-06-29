package com.sharedcooldowns.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * Utility for translating legacy '&' color codes into Adventure Components.
 */
object ColorUtil {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()

    /**
     * Translates a string with '&' color codes into an Adventure [Component].
     *
     * Example: "&cHello &eWorld" → red "Hello " + yellow "World"
     */
    fun String.colorize(): Component = serializer.deserialize(this)
}
