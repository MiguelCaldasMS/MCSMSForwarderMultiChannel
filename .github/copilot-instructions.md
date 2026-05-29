# Copilot Instructions — MC SMS → WhatsApp Test

## Build

```powershell
.\gradlew.bat :app:assembleDebug          # build debug APK
.\gradlew.bat :app:installDebug           # build + install on connected device/emulator
```

No test suite or linter is configured.

## Architecture

Single-module Android app (`:app`), Kotlin, no Compose — XML layouts with Material 3.

**Pipeline** (`SmsReceiver`): incoming SMS → master kill-switch (`prefs.getBoolean("master_enabled", true)`,
default ON) → bail if WhatsApp credentials / senders / regexes are empty → reassemble multipart →
match sender against allowed list via `SenderMatcher` (`PhoneNumberUtils.areSamePhoneNumber` +
case-insensitive exact match for alphanumeric IDs) → normalize body with
`TextNormalizer.normalizeForMatching` (NFD + strip combining marks + lowercase) → compile each
regex once and match any (`runCatching` per pattern; invalid patterns silently skip) → apply
optional `ForwardTemplate` (`%s`/`%t`/`%m` tokens) → `goAsync()` keeps the receiver alive →
`WhatsAppCloudChannel.send` POSTs to Meta and calls `pending.finish()` from its completion callback.
Stats only increment on HTTP 2xx; failures are logged as `SEND FAILED` with the HTTP code and the
Meta `error.{code,type,message}` summary. The original (accented, cased) body is what gets
forwarded — normalization is only for matching.

**No loop guard.** SMS arriving on the device cannot re-trigger a WhatsApp send to the same SMS
sender (different transport), so the loop-guard chip / log entry from the upstream SMS-only
project is intentionally absent.

**WhatsApp Cloud channel** (`util/WhatsAppCloudChannel.kt`): `object` with a single-thread daemon
`Executor` named `wa-sender`. `send(context, config, body, onComplete)` builds JSON via
`buildPayload` (free-form `text` when `useTemplate=false`, otherwise a template with a single
body parameter `{{1}}` bound to `body`), strips the leading `+` from the recipient, opens
`HttpURLConnection` to `https://graph.facebook.com/v21.0/{phoneNumberId}/messages`, writes the
body with `setFixedLengthStreamingMode`, sets `Authorization: Bearer …`, 10 s connect / 20 s
read timeout, then logs `SEND OK → {recipient} (HTTP {code})` or
`SEND FAILED → {recipient} (HTTP {code}) {summary}` / `SEND FAILED → {recipient} (transport) {msg}`.
The access token never appears in logs. Stats are incremented inside the channel on success only.

**Master switch**: `MasterSwitchTileService` (Quick Settings tile) and the main-screen
`MaterialSwitch` both write the same `master_enabled` pref; `MainActivity.onResume` re-syncs
the switch in case the tile flipped it while paused.

**Boot**: `BootReceiver` exists purely to make the framework load the package on
`BOOT_COMPLETED` (no real work; just a log line) so the manifest SMS receiver is warm before the
first message.

**Persistence**: everything is in a single `SharedPreferences` file named `mc_sms_fwd_wa`. No
database. Lists (senders, regexes) are newline-delimited strings. Logs use a
`timestamp\x1Fmessage` format with auto-pruning (35 days / 2000 entries). WhatsApp credentials
live under keys defined in `WhatsAppConfig`: `waPhoneNumberId`, `waAccessToken`, `waRecipient`,
`waUseTemplate` (default true), `waTemplateName`, `waTemplateLanguage` (default `en_US`).

**Activities** are plain `AppCompatActivity` subclasses — no fragments, no ViewModel, no
navigation component. Settings fields are **debounced-saved** (~150 ms after the last keystroke
via `Handler.postDelayed`) and force-flushed in `onPause()` via `flushPendingWrites()`. Dynamic
rows that share an `EditText` id (e.g. `R.id.senderEntry`) set `isSaveEnabled = false` so view-
state restore doesn't copy the last-focused row's text onto every row after recreation.

## Conventions

- **Util objects** in `util/` are Kotlin `object` singletons (not classes). They take
  `SharedPreferences` or `Context` as parameters — no dependency injection. Current set:
  `SenderListStore`, `RegexListStore`, `SenderMatcher`, `TextNormalizer`, `ForwardTemplate`,
  `ForwardStatsStore`, `WhatsAppConfig`, `WhatsAppCloudChannel`, `LogUtils`.
- **Edge-to-edge** + insets handling is repeated in every activity's `onCreate` using
  `enableEdgeToEdge()` and `ViewCompat.setOnApplyWindowInsetsListener`.
- **Regex matching** uses `TextNormalizer.normalizeForMatching` (NFD + strip combining marks +
  lowercase) so patterns can be written accent-free and case-free. Invalid regexes are
  silently treated as non-matches.
- **Version catalog** (`gradle/libs.versions.toml`) manages all dependency and SDK versions;
  `app/build.gradle.kts` references them via `libs.*`.
- Release signing is opt-in via Gradle properties (`RELEASE_KEYSTORE_PATH`, etc.). No keystore
  or access token is committed to the repository.
- The access token is stored as plain text in `SharedPreferences` for this test variant — never
  write it to logs, never include it in error messages, never paste it into bug reports.
