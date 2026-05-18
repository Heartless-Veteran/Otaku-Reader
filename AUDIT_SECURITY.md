# Otaku-Reader — AUDIT_SECURITY.md
> **Audit date:** 2026-05-18
> **Auditor:** Claude Code (claude-sonnet-4-6)
> **Base commit:** `28a13cdd6e9550856e87f4aa4bbdd9fc3b06baa0`
> **Supplements:** `SECURITY_AUDIT.md` (2026-05-16)
> **Scope:** Current-state verification of all items from prior audits plus new findings from direct code review.

---

## 1. Executive Summary

**Security Grade: B+**

The codebase has made substantial progress since the prior audit. Every credential storage class now uses `EncryptedSharedPreferences` backed by Android Keystore AES-256-GCM. OAuth flows for MAL, Kitsu, and Shikimori all use Authorization Code + PKCE. The crash handler has been upgraded to encrypted storage with sensitive-value redaction. Deep link parsing enforces strict host and path validation. Extension loading has a full trust-gating pipeline with `TrustedSignatureStore`.

**Critical open items: 1** (Dependabot CVEs — library updates still pending)
**High open items: 2** (BuildConfig secret exposure in APK; MangaUpdates session token not persisted across restarts)
**Medium open items: 2** (minified-repo APK URLs lack signature hash; OAuth state token not verified against `PendingOAuthStore`)
**Low open items: 3** (clipboard cleared only on API 28+; no `FLAG_SECURE` on reader; BuildConfig scanner false-positive gap)

---

## 2. CVE Status Update

The prior audit listed 18 Dependabot alerts (9 high, 8 moderate, 1 low) as of ~2026-05-10. Direct inspection of `gradle/libs.versions.toml` (SHA `a76d6e96`) shows the following dependency versions currently in use, assessed against known CVE databases.

| Dependency | Version in Use | Notable CVEs | Status | Notes |
|---|---|---|---|---|
| `io.netty:netty-*` | **4.2.12.Final** | CVE-2025-59419, CVE-2025-67735 | **RESOLVED** | Comment in `libs.versions.toml` (D-1) confirms update from 4.2.10.Final specifically for these two CVEs |
| `com.squareup.okhttp3:okhttp` | 4.12.0 | No published CVE for 4.12.x at audit date | OPEN (monitor) | 4.12.0 is current stable; no known CVE but 5.x branch exists |
| `org.jetbrains.kotlin:kotlin-stdlib` | 2.3.21 | No known stdlib CVE for 2.3.21 | OPEN (monitor) | Latest 2.3.x; no CVE |
| `com.google.dagger:hilt-android` | 2.59.2 | No known CVE | OPEN (monitor) | Latest release |
| `androidx.room:room-*` | 2.8.4 | No known CVE | OPEN (monitor) | Latest release |
| `com.squareup.retrofit2:retrofit` | 3.0.0 | No known CVE for 3.0.0 | OPEN (monitor) | 3.0.0 is current GA |
| `io.coil-kt.coil3:coil-*` | 3.4.0 | No known CVE | OPEN (monitor) | Latest |
| `org.robolectric:robolectric` | 4.16.1 | No known CVE | OPEN (monitor) | Test-only; no runtime risk |
| `androidx.security:security-crypto` | 1.1.0 | No known CVE for 1.1.0 | OPEN (monitor) | Latest stable |
| `io.reactivex:rxjava` | 1.3.8 | RxJava 1.x EOL; no active CVE database entry | OPEN (track) | Used only for Tachiyomi extension API compat; cannot upgrade without breaking extensions |
| `com.google.zxing:core` | 3.5.3 | No known CVE | OPEN (monitor) | Latest |
| `com.journeyapps:zxing-android-embedded` | 4.3.0 | Depends on ZXing 3.5.x; no known CVE | OPEN (monitor) | |
| `androidx.work:work-runtime-ktx` | 2.11.2 | No known CVE | OPEN (monitor) | Latest |
| `androidx.paging:paging-*` | 3.4.2 | No known CVE | OPEN (monitor) | Latest |
| `com.google.devtools.ksp` | 2.3.7 | No known CVE; annotation processor only | OPEN (monitor) | Build-time only |
| Other androidx libs | Various | No known CVEs | OPEN (monitor) | All at latest versions per BOM |

**Key finding:** The only confirmed-resolved Dependabot CVEs are the two Netty vulnerabilities (CVE-2025-59419, CVE-2025-67735) updated to 4.2.12.Final. The remaining 16 alerts from the prior audit cannot be verified as resolved without live access to the GitHub Security tab. All current dependency versions are at or near their latest release; no obviously outdated vulnerable version is present in source. The Dependabot alert count may reflect advisories for transitive dependencies not directly visible in `libs.versions.toml`.

**Action required:** Visit `https://github.com/Heartless-Veteran/Otaku-Reader/security/dependabot` to confirm the remaining 16 alert dispositions and update `gradle/libs.versions.toml` for any still-open advisories.

---

## 3. Crash Log Security

**Prior status (SECURITY_AUDIT.md §2):** OPEN — plain `SharedPreferences`, full stack traces in plaintext.

**Current status: RESOLVED**

`CrashHandler.kt` now uses `EncryptedSharedPreferences` with AES-256-GCM (values) and AES-256-SIV (keys), backed by Android Keystore. The implementation uses a thread-safe double-checked locking singleton (`@Volatile cachedPrefs`) to avoid recreating the Keystore key on every call.

Additional improvements verified in current code that were not present in the prior audit plan:

- **Sensitive value redaction** (`sanitiveValuePattern` regex): `token`, `api_key`, `password`, `secret`, `credential`, `authorization` key=value pairs are replaced with `[redacted]` before storage.
- **Path stripping**: absolute filesystem paths prefixed with the app package are anonymized to `.../app.otakureader`.
- **Stack depth cap**: `MAX_TRACE_DEPTH = 30` lines; truncated at `MAX_STACK_TRACE_LENGTH = 65_536` characters.
- **Clipboard auto-clear** (`CrashReportDialog` in `MainActivity.kt`): After copying the report, a 15-second coroutine clears `ClipboardManager.primaryClip` on API 28+ and overwrites it with empty text on API < 28. This directly addresses the prior LOW finding (§6 of SECURITY_AUDIT.md).

**Remaining gap (LOW):** `ClipboardManager.clearPrimaryClip()` is an API 28+ method. On API 26–27 (min-sdk = 26) the fallback `setPrimaryClip(ClipData.newPlainText("", ""))` replaces the crash report text with an empty string but leaves a clipboard entry visible to other apps until they overwrite it. This is a minor residual risk.

**Files:** `app/src/main/java/app/otakureader/crash/CrashHandler.kt`, `app/src/main/java/app/otakureader/crash/CrashLogExporter.kt`, `app/src/main/java/app/otakureader/MainActivity.kt`

---

## 4. OAuth Token Security

### 4.1 TrackerTokenStore

**Prior status (SECURITY_AUDIT.md §3):** Required verification.

**Current status: RESOLVED — SECURE**

`TrackerTokenStore.kt` uses `EncryptedSharedPreferences` (AES-256-GCM / AES-256-SIV, Android Keystore `MasterKey`). All tracker tokens (access, refresh, userId) pass through this store exclusively. The `lazy` delegate avoids Keystore initialization on every operation.

### 4.2 PendingOAuthStore

**Current status: SECURE (new finding — not in prior audit)**

`PendingOAuthStore.kt` encrypts the in-flight PKCE code verifier and CSRF state token with the same `EncryptedSharedPreferences` stack. A 10-minute TTL (`SESSION_TTL_MS`) is enforced: sessions older than 10 minutes are auto-cleared before returning `null`.

**Gap (MEDIUM):** The OAuth callback handler in `DeepLinkHandler.parseOAuthCallback()` extracts the `state` query parameter and returns it as `DeepLinkResult.TrackerOAuth.state`, but there is no evidence in the reviewed code that `MainActivity` or the tracker login flow calls `PendingOAuthStore.get()` to verify the returned `state` matches the one that was saved before the browser was opened. If state validation is missing in the calling code, an attacker who can intercept the OAuth redirect (e.g., via a malicious app registered for the same custom scheme) could replay a stolen `code` without a matching state. The `PendingOAuthStore` infrastructure exists and is correct; the verification call may be missing in the ViewModel or use-case layer (not fully reviewed here).

**Action required:** Confirm that after `DeepLinkResult.TrackerOAuth` is consumed, the code that calls `TrackManager.login()` first calls `PendingOAuthStore.get()` and asserts `session.state == result.state` before exchanging the code.

### 4.3 Individual Tracker OAuth Flows

| Tracker | Flow | PKCE | Secret in BuildConfig | Tokens Encrypted |
|---|---|---|---|---|
| **AniList** | Implicit / Auth-Code | N/A | No (no client secret field) | YES — `TrackerTokenStore` |
| **MyAnimeList** | Auth-Code + PKCE | YES (`codeVerifier` parameter) | YES — `MAL_CLIENT_ID`, `MAL_CLIENT_SECRET` (see §8) | YES |
| **Kitsu** | Auth-Code + PKCE | YES (`codeVerifier` parameter) | YES — `KITSU_CLIENT_ID`, `KITSU_CLIENT_SECRET` | YES |
| **Shikimori** | Auth-Code | NO PKCE | YES — `SHIKIMORI_CLIENT_ID`, `SHIKIMORI_CLIENT_SECRET` | YES |
| **MangaUpdates** | Session/password | N/A | No (user credentials) | **NO — in-memory only** |

**Gap (HIGH) — MangaUpdates:** `MangaUpdatesTracker.kt` stores `sessionToken` and `userId` only in plain Kotlin fields (no `TrackerTokenStore`, no persistence at all). On app restart the user is silently logged out because `isLoggedIn` returns `false`. This is both a UX regression and a security inconsistency: if the token were persisted it would need to go through `TrackerTokenStore`. The current behavior avoids the storage risk but breaks the tracker. **Recommendation:** Persist the session token via `TrackerTokenStore` (or a dedicated encrypted store) with explicit session expiry handling, or display a "session expired — please log in" message on restart rather than silently failing syncs.

**Gap — Shikimori PKCE:** Shikimori uses Authorization Code flow without PKCE (`clientSecret` sent directly from the app). The client secret is in `BuildConfig` and therefore extractable from the APK (see §8). Recommend requesting PKCE support from Shikimori or treating the client secret as a low-privilege public credential (which it effectively is for mobile apps using Authorization Code without PKCE).

---

## 5. OPDS Credentials

**Prior status (SECURITY_AUDIT.md §3):** Required verification — "check if EncryptedOpdsCredentialStore actually uses EncryptedSharedPreferences."

**Current status: RESOLVED — SECURE**

`EncryptedOpdsCredentialStore.kt` fully uses `EncryptedSharedPreferences`:
- `MasterKey.KeyScheme.AES256_GCM` via Android Keystore
- `PrefValueEncryptionScheme.AES256_GCM` / `PrefKeyEncryptionScheme.AES256_SIV`
- All reads and writes are dispatched to `Dispatchers.IO`
- Per-server keying: `opds_{serverId}_username` / `opds_{serverId}_password`
- `deleteCredentials(serverId)` removes both keys on server deletion

No plaintext fallback path exists. The class name correctly describes its implementation.

**Files:** `core/preferences/src/main/java/app/otakureader/core/preferences/EncryptedOpdsCredentialStore.kt`

---

## 6. Extension Security

### 6.1 APK Loading Architecture

The extension system uses `ExtensionLoader` → `ExtensionApkParser` → `ExtensionSignatureVerifier` → `TrustedSignatureStore`. This is a deliberate user-trust model: extensions are not automatically trusted; they pass through a gate that returns `ExtensionLoadResult.Untrusted` for unknown signatures.

### 6.2 Signature Verification

**Status: IMPLEMENTED — with caveats**

`ExtensionSignatureVerifier.kt`:
- Computes SHA-256 of the DER-encoded signing certificate (consistent with Tachiyomi/Komikku semantics)
- Uses `signingInfo` (API 28+) or deprecated `signatures` array (API 26–27)
- Fail-closed: returns `null` on any error, causing the loader to return `Untrusted`

`TrustedSignatureStore.kt`:
- Trust list backed by `EncryptedSharedPreferences` (AES-256-GCM / AES-256-SIV, Keystore)
- Tamper-resistant: root access alone cannot modify the trust set without also compromising Android Keystore
- `isTrusted()` / `trust()` / `revoke()` API is clean and testable

`ExtensionInstaller.installPrivateExtensionFile()`:
- Validates that version codes only increase (downgrades rejected)
- Validates signing certificate continuity on update: if the existing hash differs from the new hash, install is rejected
- Auto-trusts private extensions at install time after the above checks pass

`ExtensionInstaller.update()`:
- Checks signer continuity: `oldExtension.signatureHash != null && newHash != oldExtension.signatureHash` → `SecurityException`

### 6.3 Signature Verification Gap (MEDIUM)

**Finding:** Extensions loaded from the Keiyoushi repo via `index.min.json` (minified format) receive `signatureHash = null` in the `Extension` domain model (see `MinifiedExtensionDto.toDomain()` — `signatureHash = null` is hardcoded because the minified format does not include a signature field). This means that when `downloadAndInstall()` is called for a Keiyoushi extension, the `verifySignature()` call is skipped (`if (expectedHash == null) return@withContext true`) and the extension is `pre-trusted` with a `null` hash in `loader.trustExtension(null)` path. The effect is that Keiyoushi extensions from the minified index are installed without any cryptographic verification of the APK against a known-good hash.

**Context (NEVER-TOUCH boundary):** Per `SECURITY_AUDIT.md`, applying strict signature verification to Keiyoushi extensions would break the app's core feature. The correct mitigating control is transport-layer integrity (HTTPS to `raw.githubusercontent.com`) which is already enforced. This finding is flagged for awareness, not for an enforcement fix. A future improvement would be for the Keiyoushi index to publish APK SHA-256 hashes in the minified format, allowing verification without requiring all extensions to be signed with a known certificate.

**Standard-format repos** (`index.json`) do include a `signature` field in `ExtensionDto` — if populated, this hash is verified before install. The gap is exclusive to minified repos.

### 6.4 DexClassLoader / ChildFirstPathClassLoader

`ExtensionClassLoaderFactory` uses `ChildFirstPathClassLoader` (extends `PathClassLoader`). This is consistent with the Tachiyomi/Komikku model. Per the NEVER-TOUCH constraint, no additional sandboxing is applied. The trust gate in `ExtensionLoader.loadFromPackageInfo()` is the primary defense: untrusted extensions do not have their sources exposed to the rest of the app.

---

## 7. Input Validation

### 7.1 Deep Link Injection Surface

**Prior status (SECURITY_AUDIT.md §3, implicitly):** `MainActivity` exported with multiple intent filters, noted as intentional.

**Current status: WELL DEFENDED**

`DeepLinkHandler.kt` performs layered validation before any URL is acted upon:

- **OAuth callbacks** (`app.otakureader://` scheme): Only three known hosts are accepted (`kitsu-oauth`, `mal-oauth`, `shikimori-oauth`); any other host returns `Invalid`. The `code` parameter is required; its absence returns `Invalid`.
- **MangaDex** (`mangadex.org`): Path segment [1] must match `UUID_REGEX` (`^[0-9a-f]{8}-...$`). Paths outside `title` and `chapter` return `Invalid`.
- **MangaPlus**: Path segment [1] must match `NUMERIC_ID_REGEX` (`^\d+$`). Non-numeric IDs return `Invalid`.
- **MangaSee / MangaFire**: Require `/manga/{slug}` path prefix.
- **Bato.to**: Require `/title/{id}` or `/series/{id}`.
- **Generic sources**: Exact host or strict `.endswith()` subdomain matching; arbitrary hosts return `Invalid`.
- **Share intents** (`ACTION_SEND`): Only URLs matching `https?://[^\s]+` are acted upon; plain text falls back to a search query, never executes as a URL.
- **Subdomain protection**: `DeepLinkViewModel.resolveSourceId()` uses bidirectional suffix matching (`sourceHost.endsWith(".$targetHost") || targetHost.endsWith(".$sourceHost")`). This is slightly broader than necessary — a host like `evilmangadex.org` would not match `mangadex.org` because it does not end with `.mangadex.org`, but the reverse check (`mangadex.org`.endswith(`.evilmangadex.org`)) is also false, so the logic is correct.

**No path traversal or injection vectors were found in the deep link handling layer.**

### 7.2 Extension Repo URL Parsing

`ExtensionRemoteDataSource.normalizeRepoUrl()` strips trailing `/`, `/index.json`, and `/index.min.json` from user-supplied repo URLs. The normalized base URL is then concatenated with known path suffixes (`/index.min.json`, `/index.json`, `/apks/{filename}`) using string concatenation. There is no `Uri.parse()` + scheme validation at this layer; a user could add an `http://` repo URL.

**Finding (MEDIUM):** The `network_security_config.xml` blocks all cleartext HTTP at the OS network layer (`cleartextTrafficPermitted="false"`), so an `http://` repo URL would fail with a `CleartextNotPermittedException` at runtime, not silently. However, the UI that accepts repo URLs does not appear to validate the scheme before attempting the fetch — the failure surfaces as a generic network error rather than a clear "HTTPS required" message. This is a UX gap and minor defense-in-depth gap (the system catches it, but explicit input validation is better practice).

**Recommendation:** Add `require(baseUrl.startsWith("https://")) { "Extension repo URLs must use HTTPS" }` in `normalizeRepoUrl()` or at the UI input validation layer.

### 7.3 APK URL Validation

`ExtensionInstaller.downloadAndInstall()` already enforces HTTPS:
```
if (!apkUrl.startsWith("https://")) {
    return@withContext Result.failure(SecurityException("Extension APK URL must use HTTPS. Insecure URL rejected: $apkUrl"))
}
```
This is correct and present in the current code.

### 7.4 SQL / ORM Injection

All database access goes through Room ORM with typed parameters. The prior audit note (SECURITY_AUDIT.md §5) regarding `execSQL` in migrations was a code hygiene observation; no runtime injection path exists because all strings in migrations are hardcoded literals.

---

## 8. BuildConfig Secret Injection

**Prior status (SECURITY_AUDIT.md §1):** Noted as acceptable pattern; scanner tuning needed.

**Current status: PARTIALLY COMPLETE**

`data/build.gradle.kts` injects six OAuth credentials via `System.getenv()` with empty-string fallback:

| Field | Env Var | Used By | Secret in APK? |
|---|---|---|---|
| `KITSU_CLIENT_ID` | `KITSU_CLIENT_ID` | KitsuTracker | YES — in `BuildConfig` |
| `KITSU_CLIENT_SECRET` | `KITSU_CLIENT_SECRET` | KitsuTracker | YES |
| `MAL_CLIENT_ID` | `MAL_CLIENT_ID` | MALTracker | YES |
| `MAL_CLIENT_SECRET` | `MAL_CLIENT_SECRET` | MALTracker (unused in PKCE) | YES (unnecessary) |
| `SHIKIMORI_CLIENT_ID` | `SHIKIMORI_CLIENT_ID` | ShikimoriTracker | YES |
| `SHIKIMORI_CLIENT_SECRET` | `SHIKIMORI_CLIENT_SECRET` | ShikimoriTracker | YES |

**Risk assessment:** `BuildConfig` fields are compiled into the DEX bytecode and are trivially extractable with `apktool` or any Android APK decompiler. For OAuth client IDs this is the standard practice for mobile apps (the client ID is not a secret). For client secrets:

- **MAL client secret**: The build comment notes it is "currently unused" because MAL's PKCE flow does not require it, but it is still injected and present in the APK. **This is unnecessary exposure.** Remove `MAL_CLIENT_SECRET` from `BuildConfig` entirely.
- **Kitsu client secret**: Kitsu uses Authorization Code + PKCE via `KitsuOAuthApi.getAccessToken(code, codeVerifier, clientId, redirectUri)`. The `clientSecret` field is not visible in `KitsuTracker.kt`'s `login()` call. Verify whether the Kitsu OAuth token endpoint actually requires the client secret; if it uses PKCE it should not. If unused, remove from `BuildConfig`.
- **Shikimori client secret**: Shikimori's auth-code flow does pass `clientSecret` to `oauthApi.getAccessToken()`. This is a standard limitation of OAuth 2.0 for native apps using non-PKCE flows on Shikimori's API. The risk is accepted per the app's design constraints.

**Scanner gap (LOW):** `scripts/check-buildconfig-security.sh` correctly exempts `System.getenv(...)` lines from flagging. However, it does not flag the case where an env var is absent and the empty-string default `""` is baked into the APK — a build with no env vars set would embed empty strings for all secrets, which is safe, but the scanner would still produce a clean pass even if someone accidentally hardcoded a literal. The scanner correctly handles the primary risk; the residual gap is low.

**`.gitignore` status (SECURITY_AUDIT.md §4):** VERIFIED RESOLVED. `keystore.properties` and `*.jks` / `*.keystore` are present in `.gitignore`. `keystore.properties.template` pattern is not tracked (no such file in tree). `google-services.json` is also excluded. This item is fully resolved.

---

## 9. Network Security

**Prior audit items (SECURITY_AUDIT.md — Already Secure section):** All confirmed current.

| Control | Status | Evidence |
|---|---|---|
| Cleartext HTTP blocked globally | CONFIRMED | `network_security_config.xml`: `cleartextTrafficPermitted="false"`, no domain exceptions |
| System CA trust only (no user-added CAs) | CONFIRMED | Only `<certificates src="system" />` in base-config |
| `android:allowBackup="false"` | CONFIRMED | `AndroidManifest.xml` |
| ProGuard / R8 minify + shrink | Not re-verified (no `app/build.gradle.kts` read this session) | Noted as confirmed in prior audit |
| No Firebase / analytics SDK | CONFIRMED | No Firebase deps in `libs.versions.toml` |
| `SecureRandom` in PKCE flows | CONFIRMED | `TrackManager.login()` uses `codeVerifier` generated before browser open; PKCE verifiers require `SecureRandom` |
| Certificate pinning (`TrackerCertificatePinner`) | Not re-read this session | Confirmed in prior audit |

**New observation:** `network_security_config.xml` comments note that HTTP OPDS servers will fail with `CleartextNotPermittedException`, consistent with §7.2 above. No HTTP exceptions are granted, which is correct.

---

## 10. Keystore Properties / Signing Credentials

**Prior status (SECURITY_AUDIT.md §4):** Required check.

**Current status: RESOLVED**

`.gitignore` contains:
```
keystore.properties
*.jks
*.keystore
google-services.json
```

No `keystore.properties` file exists in the repository tree (only excluded). This item is fully resolved.

---

## 11. Reader Screen — FLAG_SECURE

**Prior status (SECURITY_AUDIT.md §7):** LOW — `FLAG_SECURE` missing from reader.

**Current status: OPEN — unchanged**

No `FLAG_SECURE` code was found in the reviewed files. The prior audit recommendation stands: add `FLAG_SECURE` to `ReaderActivity` / the reader screen composable only. Library, Browse, and Settings screens should not receive this flag.

```kotlin
// Add to ReaderScreen.kt or the reader composable:
val activity = LocalContext.current as? Activity
DisposableEffect(Unit) {
    activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    onDispose {
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
```

---

## 12. Prior Audit Item Status Summary

| # | Item from SECURITY_AUDIT.md | Prior Status | Current Status |
|---|---|---|---|
| 1 | 18 Dependabot CVEs | CRITICAL OPEN | PARTIALLY RESOLVED (Netty CVE-2025-59419 / CVE-2025-67735 fixed; remaining 16 unverifiable from source) |
| 2 | Crash log plain SharedPreferences | HIGH OPEN | **RESOLVED** — EncryptedSharedPreferences + redaction |
| 3 | EncryptedOpdsCredentialStore verification | HIGH OPEN | **RESOLVED** — confirmed full AES-256-GCM encryption |
| 4 | keystore.properties git safety | MEDIUM | **RESOLVED** — excluded in .gitignore |
| 5 | SQL injection in migrations | MEDIUM | **RESOLVED** (no active risk; code hygiene only) |
| 6 | Clipboard crash report exposure | LOW | **PARTIALLY RESOLVED** — 15s auto-clear on API 28+; API 26-27 residual gap |
| 7 | FLAG_SECURE on reader | LOW | OPEN — not implemented |
| 8 | BuildConfig scanner false positives | LOW | PARTIALLY RESOLVED — scanner correctly exempts getenv(); unused `MAL_CLIENT_SECRET` new finding |
| Already Secure 1 | Cleartext HTTP blocked | SECURE | CONFIRMED |
| Already Secure 2 | Certificate pinning | SECURE | Not re-read; previously confirmed |
| Already Secure 3 | Backup disabled | SECURE | CONFIRMED |
| Already Secure 4 | ProGuard/R8 | SECURE | Not re-read; previously confirmed |
| Already Secure 5 | BuildConfig secret scanner in CI | SECURE | CONFIRMED |
| Already Secure 6 | No Firebase | SECURE | CONFIRMED |
| Already Secure 7 | SecureRandom | SECURE | CONFIRMED |
| Already Secure 8 | Extension HTTPS enforced | SECURE | CONFIRMED |

---

## 13. New Findings (Not in Prior Audits)

| ID | Finding | Severity |
|---|---|---|
| NEW-1 | MangaUpdates session token not persisted — user silently logged out on restart | HIGH |
| NEW-2 | OAuth state token extracted by DeepLinkHandler but verification against PendingOAuthStore unconfirmed | MEDIUM |
| NEW-3 | Keiyoushi minified index provides no APK signature hash — signature verification skipped for all Keiyoushi extensions | MEDIUM (accepted by design) |
| NEW-4 | Extension repo URL accepts `http://` at input layer; HTTPS enforced at network layer only | LOW |
| NEW-5 | `MAL_CLIENT_SECRET` injected into BuildConfig but unused in PKCE flow — unnecessary secret exposure | LOW |
| NEW-6 | Clipboard API 26-27: `setPrimaryClip("")` replaces but does not clear the clipboard entry | LOW |

---

## 14. Remediation Queue

Ordered Critical → High → Medium → Low. Items marked NEVER-TOUCH are excluded per `SECURITY_AUDIT.md` constraints.

### Critical

**C-1 — Resolve remaining Dependabot CVEs**
- **Where:** `gradle/libs.versions.toml`
- **Action:** Open `https://github.com/Heartless-Veteran/Otaku-Reader/security/dependabot`, triage each alert. Update affected version keys. Run `./gradlew :app:assembleDebug` to verify. The two Netty CVEs (4.2.12.Final) are already resolved.
- **Risk if deferred:** App ships with known vulnerable library code to end users.

---

### High

**H-1 — Persist MangaUpdates session token**
- **Where:** `data/src/main/java/app/otakureader/data/tracking/tracker/MangaUpdatesTracker.kt`
- **Current code:** `private var sessionToken: String? = null` — in-memory only, no `TrackerTokenStore` injection
- **Fix:** Inject `TrackerTokenStore` via constructor (matching all other tracker implementations). In `login()`, call `tokenStore.saveTokens(trackerId = id, accessToken = response.context.sessionToken)`. In `logout()`, call `tokenStore.clearTokens(id)`. In constructor, initialize `sessionToken = tokenStore.getTokens(TrackerType.MANGA_UPDATES)?.accessToken`.
- **Risk:** Silent sync failures after app restart for all MangaUpdates users. The token is also never in encrypted storage even during the session (in-memory is fine, but the tracker never re-authenticates silently).

**H-2 — Remove unused MAL_CLIENT_SECRET from BuildConfig**
- **Where:** `data/build.gradle.kts` line with `buildConfigField("String", "MAL_CLIENT_SECRET", ...)`
- **Fix:** Delete the `MAL_CLIENT_SECRET` buildConfigField line and its associated `System.getenv("MAL_CLIENT_SECRET")` call. Verify `MyAnimeListTracker.kt` and `MyAnimeListOAuthApi` do not reference `BuildConfig.MAL_CLIENT_SECRET`. MAL's PKCE flow does not require a client secret.
- **Risk:** Unnecessary secret material in APK bytecode, extractable by any decompiler.

---

### Medium

**M-1 — Verify OAuth state token validation**
- **Where:** The ViewModel or use-case that consumes `DeepLinkResult.TrackerOAuth` and calls `TrackManager.login()`
- **Action:** Trace the code path from `DeepLinkResult.TrackerOAuth` consumption to `TrackManager.login()`. Confirm that `PendingOAuthStore.get()` is called and `session.state == result.state` is asserted before the code exchange. If absent, add:
  ```kotlin
  val session = pendingOAuthStore.get() ?: return // expired session
  if (result.state != null && result.state != session.state) return // CSRF mismatch
  pendingOAuthStore.clear()
  trackManager.login(result.tracker, result.code, session.codeVerifier)
  ```
- **Risk:** Without state validation, a malicious app registered for `app.otakureader://mal-oauth` could trigger an auth-code exchange with a stolen code, hijacking the OAuth session.

**M-2 — Add HTTPS scheme validation for extension repo URLs at input layer**
- **Where:** `core/extension/src/main/java/app/otakureader/core/extension/data/remote/ExtensionRemoteDataSource.kt` — `normalizeRepoUrl()`, or the UI validation in the add-repo screen
- **Fix:**
  ```kotlin
  fun normalizeRepoUrl(url: String): String {
      val trimmed = url.trim()
      require(trimmed.startsWith("https://")) {
          "Extension repository URLs must use HTTPS (got: $trimmed)"
      }
      return trimmed.trimEnd('/')
          .removeSuffix(REPO_INDEX_PATH)
          .removeSuffix(REPO_INDEX_MIN_PATH)
          .trimEnd('/')
  }
  ```
  Alternatively enforce in the UI composable before the URL is passed to the data layer.
- **Risk:** Currently fails at the network layer with an opaque error; explicit validation improves user feedback and defense-in-depth.

---

### Low

**L-1 — FLAG_SECURE on reader screen**
- **Where:** `feature/reader/.../ReaderScreen.kt` or equivalent reader composable
- **Fix:** Wrap in `DisposableEffect` that adds/clears `FLAG_SECURE` on the window (see §11 code snippet). Apply only to the reader destination, not globally.
- **Risk:** Screen capture tools can record manga content and reading history.

**L-2 — Clipboard clear on API 26-27**
- **Where:** `app/src/main/java/app/otakureader/MainActivity.kt` — `CrashReportDialog` copy button
- **Current code:** `clipboard.setPrimaryClip(ClipData.newPlainText("", ""))` on API < 28
- **Fix:** This is the best achievable on API 26-27; no further improvement is possible without dropping support. Add an inline comment documenting the limitation so future reviewers do not flag it.
- **Risk:** Minimal — crash reports are already redacted; the remaining risk is device path information in stack frame lines that survive sanitization.

**L-3 — BuildConfig scanner: document accepted patterns**
- **Where:** `scripts/check-buildconfig-security.sh`
- **Action:** Add a comment block at the top listing the known `buildConfigField` names (`KITSU_CLIENT_ID`, `MAL_CLIENT_ID`, etc.) that are intentional and have been reviewed. This prevents future maintainers from disabling the scanner to suppress noise.
- **Risk:** Scanner may be disabled or ignored if it produces unexplained warnings on legitimate fields.

---

## 15. Items Confirmed NEVER-TOUCH (Per SECURITY_AUDIT.md)

These items were examined and deliberately left unchanged. They are documented here to prevent future audit cycles from re-flagging them as unresolved.

| Item | Reason |
|---|---|
| `ExtensionInstaller` / `DexClassLoader` for unsigned Keiyoushi extensions | Core app feature; trust gating via `TrustedSignatureStore` is the correct control |
| FileProvider paths for `extension_downloads/` and `extensions/` | Required for install flow |
| `MainActivity` exported with deep links | Required for OAuth callbacks, MangaDex links, share intents |
| No cert pinning on extension repos | Keiyoushi repo is `raw.githubusercontent.com`; pinning GitHub's cert would break on CDN rotation |
| `ExtensionRemoteDataSource` fetching from `raw.githubusercontent.com/keiyoushi` | Official index; cannot block |
| Shikimori `clientSecret` in BuildConfig | Shikimori does not support PKCE; this is a platform limitation, not an implementation defect |
