package org.shotrush.atom.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

object ChatUtil {
    private val miniMessage = MiniMessage.miniMessage()
    private val legacySerializer = LegacyComponentSerializer.builder()
        .character('&')
        .hexColors()
        .build()

    fun color(message: String?): Component {
        if (message.isNullOrBlank()) return Component.empty()

        if (message.contains("<") && message.contains(">")) {
            return try {
                miniMessage.deserialize(message)
            } catch (_: Exception) {
                legacySerializer.deserialize(message)
            }
        }

        // Legacy
        return legacySerializer.deserialize(message)
    }
}
