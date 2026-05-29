# MC SMS → WhatsApp Test

Sister project to **MC SMS Forwarder**. Listens for incoming SMS on an Android device, runs them through the same sender/regex filter pipeline, and forwards the matched body as a **WhatsApp message** through the [WhatsApp Cloud API](https://developers.facebook.com/docs/whatsapp/cloud-api/) instead of via cellular SMS.

> **Test variant.** The WhatsApp access token is stored as plain text in the app's private `SharedPreferences`. Only install on a device you fully control, and use a system-user token with the minimum scope required.

## What it does

- `BroadcastReceiver` listens to `SMS_RECEIVED`.
- Reassembles multipart messages.
- Drops everything unless **master switch** is on.
- Matches the sender against the **allowed senders** list (E.164 phone numbers via `PhoneNumberUtils.areSamePhoneNumber`, or case-insensitive exact match for alphanumeric IDs).
- Normalizes the body (NFD + strip combining marks + lowercase) and matches it against **any** of the configured regex patterns.
- Optionally re-formats the outgoing text with a template (`%s` = source, `%t` = time, `%m` = original message).
- Sends the result via `POST https://graph.facebook.com/v21.0/{PHONE_NUMBER_ID}/messages` with a `Bearer` token, as either a free-form `text` message (inside the customer 24‑hour window) or an **approved template** with the SMS body bound to a single body parameter.

The reception, filtering, normalization, multipart handling, and template logic are intentionally **byte-for-byte** identical to the upstream SMS forwarder. Only the outbound channel changed.

## What is NOT included

- No retry / backoff queue (SMS provider does that for the upstream project; the Cloud API requires app-side handling and that's out of scope for this test variant).
- No webhook server for delivery receipts.
- No media (image/audio/document) forwarding — text only.
- No loop-guard: SMS arriving on your phone can never re-trigger a WhatsApp send back to that same SMS sender, so the guard from the upstream project is gone.

## Build & install

```powershell
.\gradlew.bat :app:assembleDebug          # build debug APK
.\gradlew.bat :app:installDebug           # build + install on connected device/emulator
```

`minSdk` 33, `targetSdk` 36, Kotlin 2.0, AGP 8.13, Material 3.

Release signing is opt-in via Gradle properties (`RELEASE_KEYSTORE_PATH`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`). No keystore is committed.

## One-time Meta setup

1. Create a **Meta for Developers** account at <https://developers.facebook.com/>.
2. Create a **Business** app and add the **WhatsApp** product.
3. From *WhatsApp → API Setup*:
   - Copy the **Phone Number ID** of the test number Meta provisions for you (or the production number you onboarded).
   - Generate a **temporary access token** (valid 24 h) for first runs. For anything longer, create a **System User** in Business Settings and generate a permanent token with `whatsapp_business_messaging` scope.
4. In *WhatsApp → API Setup*, register one or more **recipient numbers** that the test number is allowed to message; or onboard your own number.
5. (Optional) Create and submit an **approved message template** if you want to send outside the 24-hour customer window. The template must declare exactly **one body parameter `{{1}}`** — the SMS body will be bound to it.

## In-app configuration

Settings screen:

- **Phone Number ID** — numeric, from Meta.
- **Access token** — Bearer token; stored as plain text on device (see warning above).
- **Recipient** — destination WhatsApp number in E.164 form (`+35191XXXXXXX`).
- **Send as approved template** — on by default. When on, fill **Template name** and **Template language** (e.g. `en_US`). When off, messages are sent as free-form `text` (only works inside the 24-hour customer service window).
- **Allowed senders** — one per row; phone numbers or alphanumeric IDs.
- **Message format regexes** — one per row; a message is forwarded if **any** pattern matches.
- **Forwarding template** (optional) — `%s`, `%t`, `%m` tokens.

There is also a **Send WhatsApp test message** button that POSTs a synthetic message to your recipient using your current settings.

## Architecture

Single-module Android app (`:app`), Kotlin, no Compose — XML layouts with Material 3.

**Pipeline** (`SmsReceiver`): incoming SMS → master kill-switch (`mc_sms_fwd_wa`/`master_enabled`, default ON) → bail if destination/senders/regexes/credentials missing → reassemble multipart → match sender via `SenderMatcher` → normalize body via `TextNormalizer.normalizeForMatching` → compile each regex once and match any → apply optional `ForwardTemplate` → `BroadcastReceiver.goAsync()` → `WhatsAppCloudChannel.send` POSTs to Meta and finishes the pending result.

`WhatsAppCloudChannel` uses `HttpURLConnection` on a single-thread daemon executor (10 s connect / 20 s read). Stats only increment on `HTTP 2xx`; failures are written to the activity log with the HTTP code and Meta error code/type/message. The access token is **never** logged.

## License

MIT — see [LICENSE](LICENSE).
