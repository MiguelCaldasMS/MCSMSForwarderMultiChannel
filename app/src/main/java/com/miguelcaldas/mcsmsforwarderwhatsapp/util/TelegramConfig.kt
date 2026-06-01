package com.miguelcaldas.mcsmsforwarderwhatsapp.util

import android.content.SharedPreferences

/**
 * Immutable snapshot of the Telegram bot credentials persisted in SharedPreferences.
 * Telegram is the second outbound channel and is opt-in: it ships disabled.
 */
data class TelegramConfig(
    val enabled: Boolean,
    val botToken: String,
    val chatId: String,
) {
    val hasCredentials: Boolean
        get() = botToken.isNotBlank() && chatId.isNotBlank()

    val isOperational: Boolean
        get() = enabled && hasCredentials

    companion object {
        const val KEY_ENABLED = "tgEnabled"
        const val KEY_BOT_TOKEN = "tgBotToken"
        const val KEY_CHAT_ID = "tgChatId"

        fun load(prefs: SharedPreferences): TelegramConfig = TelegramConfig(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            botToken = prefs.getString(KEY_BOT_TOKEN, "").orEmpty().trim(),
            chatId = prefs.getString(KEY_CHAT_ID, "").orEmpty().trim(),
        )
    }
}
