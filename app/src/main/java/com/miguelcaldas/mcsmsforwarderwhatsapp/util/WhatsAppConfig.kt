package com.miguelcaldas.mcsmsforwarderwhatsapp.util

import android.content.SharedPreferences

/**
 * In-memory snapshot of the WhatsApp Cloud API credentials and template
 * settings persisted in SharedPreferences. Loaded once per send so the
 * receiver / settings UI can pass an immutable value around.
 */
data class WhatsAppConfig(
    val enabled: Boolean,
    val phoneNumberId: String,
    val accessToken: String,
    val recipient: String,
    val useTemplate: Boolean,
    val templateName: String,
    val templateLanguage: String,
) {
    val hasCredentials: Boolean
        get() = phoneNumberId.isNotBlank() && accessToken.isNotBlank() && recipient.isNotBlank()

    val isComplete: Boolean
        get() = hasCredentials &&
            (!useTemplate || (templateName.isNotBlank() && templateLanguage.isNotBlank()))

    val isOperational: Boolean
        get() = enabled && isComplete

    companion object {
        const val KEY_ENABLED = "waEnabled"
        const val KEY_PHONE_NUMBER_ID = "waPhoneNumberId"
        const val KEY_ACCESS_TOKEN = "waAccessToken"
        const val KEY_RECIPIENT = "waRecipient"
        const val KEY_USE_TEMPLATE = "waUseTemplate"
        const val KEY_TEMPLATE_NAME = "waTemplateName"
        const val KEY_TEMPLATE_LANGUAGE = "waTemplateLanguage"

        const val DEFAULT_TEMPLATE_LANGUAGE = "en_US"

        fun load(prefs: SharedPreferences): WhatsAppConfig = WhatsAppConfig(
            // Default true so existing installs keep forwarding to WhatsApp after upgrade.
            enabled = prefs.getBoolean(KEY_ENABLED, true),
            phoneNumberId = prefs.getString(KEY_PHONE_NUMBER_ID, "").orEmpty().trim(),
            accessToken = prefs.getString(KEY_ACCESS_TOKEN, "").orEmpty().trim(),
            recipient = prefs.getString(KEY_RECIPIENT, "").orEmpty().trim(),
            useTemplate = prefs.getBoolean(KEY_USE_TEMPLATE, true),
            templateName = prefs.getString(KEY_TEMPLATE_NAME, "").orEmpty().trim(),
            templateLanguage = prefs.getString(KEY_TEMPLATE_LANGUAGE, DEFAULT_TEMPLATE_LANGUAGE)
                .orEmpty().ifBlank { DEFAULT_TEMPLATE_LANGUAGE },
        )
    }
}
