package com.miguelcaldas.mcsmsforwardermultichannel.util

import android.content.SharedPreferences

/**
 * Immutable snapshot of the outbound SMS channel settings persisted in
 * SharedPreferences. SMS is the third outbound channel and is opt-in: it
 * ships disabled so existing installs are unaffected after upgrade.
 *
 * The only credential is the destination number; the device's own SIM/modem
 * does the actual sending, so there is no token to store.
 */
data class SmsConfig(
    val enabled: Boolean,
    val destination: String,
) {
    val hasCredentials: Boolean
        get() = destination.isNotBlank()

    val isOperational: Boolean
        get() = enabled && hasCredentials

    companion object {
        const val KEY_ENABLED = "smsEnabled"
        // Reuse the legacy single-channel key so installs migrated from the
        // SMS-only app keep their existing destination number.
        const val KEY_DESTINATION = "forwardTo"

        fun load(prefs: SharedPreferences): SmsConfig = SmsConfig(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            destination = prefs.getString(KEY_DESTINATION, "").orEmpty().trim(),
        )
    }
}
