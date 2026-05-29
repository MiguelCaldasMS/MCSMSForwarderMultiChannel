# Handoff to next Copilot chat

This project was scaffolded by cloning `MCSMSForwarder` and replacing the SMS-send channel with a WhatsApp Cloud API channel. Open this file in chat first.

## State at handoff

- Source tree: complete. Package = `com.miguelcaldas.mcsmsforwarderwhatsapp`. New files: `util/WhatsAppCloudChannel.kt`, `util/WhatsAppConfig.kt`. Removed: `util/SmsSendResultTracker.kt` (HTTP gives synchronous status).
- `SmsReceiver` still receives SMS; outbound is **WhatsApp Cloud API** via `HttpURLConnection` on a single-thread executor with `BroadcastReceiver.goAsync()`.
- Manifest: adds `INTERNET` + `ACCESS_NETWORK_STATE`; keeps `RECEIVE_SMS` / `READ_SMS` / `READ_PHONE_STATE`; drops `SEND_SMS`.
- SharedPreferences file renamed `mc_sms_forwarder` → `mc_sms_whatsapp_forwarder`. New keys: `waPhoneNumberId`, `waAccessToken`, `waRecipient` (E.164, no `+`). Old `forwardTo` removed. Master switch, sender list, regex list, template, stats, logs unchanged.
- **Token is currently stored in plain SharedPreferences** — EncryptedSharedPreferences upgrade is deferred (noted in copilot-instructions).
- `applicationId` and `namespace` updated in `app/build.gradle.kts`. `settings.gradle.kts` `rootProject.name = "MCSMSForwarderWhatsAppTest"`.
- `copilot-instructions.md` rewritten for the WhatsApp pipeline.

## Not yet done

1. `gradlew.bat :app:assembleDebug` — never run in this project. Compile errors possible.
2. `git init` + initial commit. Not a git repo yet.
3. Create GitHub repo `MCSMSForwarderWhatsAppTest` under `MiguelCaldasMS` and push.
4. Emulator smoke test scripts (`scripts/test-emulator-*.ps1`) were intentionally NOT copied — they posted to a real SMS sink. New scripts need a mock HTTP server (or a real Meta sandbox phone number ID).
5. Settings UI test-send button: stub exists in `SettingsActivity` but not yet wired end-to-end.

## Suggested first prompt in the new window

> Run `./gradlew.bat :app:assembleDebug`. Fix any compile errors. Then `git init`, commit, create the GitHub repo `MCSMSForwarderWhatsAppTest` under `MiguelCaldasMS`, and push.

## Key design choices (so you don't re-litigate)

- HTTP client: `HttpURLConnection`, zero new deps. Do NOT add OkHttp without asking.
- Threading: shared single-thread `Executors.newSingleThreadExecutor()` inside `WhatsAppCloudChannel` + `goAsync()` in `SmsReceiver`.
- Retry: HTTP 429 / 5xx → exponential backoff, max 3 attempts. 4xx other than 429 = log + give up.
- Message type: free-form `text` only (no template support yet). User accepted the 24h-window limitation for the test app.
- Logging: never echo `Authorization` header or `waAccessToken` value. `LogUtils` truncates body at 500 chars.

## Files NOT to touch without reason

- `gradle/libs.versions.toml` — version catalog matches the original project; bumps need justification.
- `app/proguard-rules.pro` — empty, intentional.
- `.github/copilot-instructions.md` — read it; it is the source of truth for the WhatsApp pipeline conventions.
