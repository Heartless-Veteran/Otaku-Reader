# Otaku Reader — Security Audit & Fix Plan
> **For:** Claude Code / ruflo team
> **Date:** 2026-05-16
> **Context:** This is a pirated manga reader app. The extension system (DexClassLoader, APK side-loading, Keiyoushi repo) is the core feature. Do NOT apply security "best practices" that break this.

---

## 🚫 NEVER TOUCH — These Break the App

| # | Item | Why Breaking It = App Death |
|---|------|------------------------------|
| 1 | **ExtensionInstaller / DexClassLoader** | This IS the app. Users install extensions via APK. Certificate validation would reject unsigned Keiyoushi extensions. |
| 2 | **FileProvider paths** (`extension_downloads/`, `extensions/`, `shared_stats/`) | Extensions need cache + file access to load. Locking this down breaks install flow. |
| 3 | **MainActivity exported + deep links** | Handles MangaDex deep links + OAuth callbacks (Kitsu/MAL/Shikimori) + text/plain share intents. Required for tracker sync and link handling. |
| 4 | **Extension HTTPS enforcement** | Already correct. Do NOT make stricter (some repos use valid HTTPS, don't add cert pinning to extension repos). |
| 5 | **ExtensionRemoteDataSource** fetching from `raw.githubusercontent.com/keiyoushi/extensions/repo` | This is the official extension index. Blocking GitHub raw = no extensions. |

---

## 🔴 CRITICAL — Fix These (Safe for App Functionality)

### 1. Dependency Vulnerabilities (18 Dependabot Alerts)
**Status:** 🔴 **CRITICAL** — 9 high, 8 moderate, 1 low
**Risk:** App ships vulnerable libraries to users
**Safe to fix:** YES — updating library versions doesn't break features

**Action:**
1. Visit https://github.com/Heartless-Veteran/Otaku-Reader/security/dependabot
2. Identify which dependencies are flagged
3. Update `gradle/libs.versions.toml` to patched versions
4. Run `./gradlew :app:assembleDebug` to verify build
5. Common suspects:
   - OkHttp 4.12.0 → check for 4.12.x patch or 5.x
   - Kotlin 2.3.21 → check for 2.3.x patch
   - Hilt 2.59.2 → check for patch release
   - Room 2.8.4 → check for patch
   - Retrofit 3.0.0 → check for patch

**Files:**
- `gradle/libs.versions.toml`

---

### 2. SharedPreferences — Unencrypted Crash Reports
**Status:** 🟠 **HIGH**  
**Risk:** Full stack traces (including file paths, usernames, device info) stored in plain text  
**Safe to fix:** YES — only affects crash handler, not manga functionality

**Current code:**
```kotlin
// app/src/main/java/app/otakureader/crash/CrashHandler.kt
private fun prefs(context: Context): SharedPreferences =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
```

**Fix:** Replace with `EncryptedSharedPreferences` (from `androidx.security:security-crypto`):
```kotlin
private fun prefs(context: Context): SharedPreferences {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    return EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}
```

**Files:**
- `app/src/main/java/app/otakureader/crash/CrashHandler.kt`

---

### 3. EncryptedOpdsCredentialStore — Verify It Uses Encryption
**Status:** 🟠 **HIGH**  
**Risk:** OPDS credentials might be stored in plain SharedPreferences despite the class name suggesting encryption  
**Safe to fix:** YES — OPDS is a secondary feature

**Action:** Check `core/preferences/.../EncryptedOpdsCredentialStore.kt` and verify it actually uses `EncryptedSharedPreferences`, not plain `SharedPreferences`.

**Files:**
- `core/preferences/src/main/java/app/otakureader/core/preferences/EncryptedOpdsCredentialStore.kt`

---

### 4. Keystore Properties — Git Safety Check
**Status:** 🟡 **MEDIUM**  
**Risk:** Accidental commit of `keystore.properties` with real signing credentials  
**Safe to fix:** YES — only affects release signing, not app functionality

**Action:**
1. Verify `keystore.properties` is in `.gitignore`
2. Verify `keystore.properties.template` is the ONLY tracked file
3. If `keystore.properties` is tracked, remove it from git history (BFG Repo-Cleaner or git-filter-repo)

**Files:**
- `.gitignore`
- `keystore.properties.template`
- `app/build.gradle.kts` (signing config section)

---

## 🟡 MEDIUM — Fix If Time Allows

### 5. SQL Injection Pattern in Migrations
**Status:** 🟡 **MEDIUM**  
**Risk:** `execSQL` with string interpolation is dangerous if ever copied for dynamic queries  
**Current risk:** LOW — all strings are hardcoded, no user input  
**Safe to fix:** YES — migrations are one-time, offline, don't affect runtime

**Action:** Add defensive comment and verify no user-controlled input ever reaches `execSQL`. This is a code hygiene fix, not an active vulnerability.

**Files:**
- `core/database/src/main/java/app/otakureader/core/database/migrations/DatabaseMigrations.kt`

---

### 6. Clipboard — Crash Report Exposure
**Status:** 🟢 **LOW**  
**Risk:** User copies crash report to clipboard; any app with `READ_CLIPBOARD` can read it  
**Safe to fix:** YES — only affects crash UX

**Action:** After copying to clipboard, clear it after a short delay, OR show a warning toast: "Crash report copied — sensitive info may be readable by other apps."

**Files:**
- `app/src/main/java/app/otakureader/MainActivity.kt` (crash report copy button)

---

### 7. No Screenshot Prevention (FLAG_SECURE)
**Status:** 🟢 **LOW**  
**Risk:** Screen recorders/screenshots can capture manga content and reading history  
**Safe to fix:** PARTIAL — applying to Reader screen only is safe. Applying globally might interfere with user workflows (sharing screenshots of library).

**Action:** Add `FLAG_SECURE` ONLY to `ReaderActivity` / reader screen composable:
```kotlin
val activity = LocalContext.current as? Activity
activity?.window?.setFlags(
    WindowManager.LayoutParams.FLAG_SECURE,
    WindowManager.LayoutParams.FLAG_SECURE
)
```

**Do NOT apply to:** Library, Browse, Settings (users want to screenshot these).

**Files:**
- `feature/reader/.../ReaderScreen.kt` or `ReaderActivity.kt`

---

### 8. BuildConfig Secret Scanner — Tune False Positives
**Status:** 🟢 **LOW**  
**Risk:** CI might flag legitimate build config fields as secrets  
**Safe to fix:** YES

**Action:** Review `scripts/check-buildconfig-security.sh` and ensure it doesn't flag:
- Version numbers that look like hex (e.g., `BuildConfig.VERSION_CODE`)
- Harmless string constants in test builds

**Files:**
- `scripts/check-buildconfig-security.sh`

---

## ✅ ALREADY SECURE — No Action Needed

| # | Item | Evidence |
|---|------|----------|
| 1 | Cleartext HTTP blocked | `network_security_config.xml`: `cleartextTrafficPermitted="false"` |
| 2 | Certificate pinning active | `TrackerCertificatePinner.kt`: 6 tracker endpoints, 2 pins each |
| 3 | Backup disabled | `AndroidManifest.xml`: `android:allowBackup="false"` |
| 4 | ProGuard/R8 enabled | `app/build.gradle.kts`: `isMinifyEnabled = true`, `isShrinkResources = true` |
| 5 | BuildConfig secret scanner in CI | `scripts/check-buildconfig-security.sh` runs in CI |
| 6 | No Firebase/Google analytics | No `google-services.json`, no Firebase deps |
| 7 | SecureRandom used | `TrackingViewModel.kt`: `java.security.SecureRandom()` |
| 8 | Extension HTTPS enforced | `ExtensionInstallScreen.kt` rejects non-HTTPS URLs |

---

## 🎯 Execution Order for Claude / ruflo

1. **Start with Dependabot** — highest impact, safest fix
2. **EncryptedSharedPreferences** for crash handler — quick win
3. **Verify EncryptedOpdsCredentialStore** — check if misnamed
4. **keystore.properties git safety** — one-liner check
5. **FLAG_SECURE on reader** — if easy to add without side effects
6. **Skip the rest** until build is stable

---

## ⚠️ Golden Rule

> If a "security fix" would change how extensions are loaded, verified, or installed — **DO NOT APPLY IT**. The extension system is the app's reason for existence. Users need unsigned Keiyoushi extensions. Any cert pinning, signature verification, or APK sandboxing on extensions = broken app.
