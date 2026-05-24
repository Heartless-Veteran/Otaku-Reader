# Phase 5: Security Audit
**Otaku Reader Android App** | Generated: 2026-05-24

---

## Executive Summary

Otaku Reader demonstrates **strong security fundamentals** across all seven evaluated areas. The application enforces HTTPS-only network communication, uses Android Keystore-backed AES-256-GCM encryption for all sensitive credential storage, properly configures component export attributes for API 31+, implements cryptographic extension signature verification, sanitizes crash reports, disables data backups, and applies R8 obfuscation to sensitive code.

**Overall Security Score: 8.5 / 10**

No plaintext credential storage, no hardcoded API keys, and no exploitable configuration weaknesses were identified. Minor recommendations exist for domain-specific certificate pinning and expanded ProGuard coverage.

---

## P0 Issues

*None identified.*

---

## P1 Issues

*None identified.*

---

## P2 Issues

### 1. Missing Domain-Specific Certificate Pinning

**File:** `app/src/main/res/xml/network_security_config.xml`

The global network security configuration correctly enforces `cleartextTrafficPermitted="false"` and trusts only system certificate anchors. However, no domain-specific certificate pinning is configured for high-risk tracker API endpoints (MyAnimeList, Kitsu, Shikimori, AniList, MangaDex).

Without pinning, a compromised CA or DNS hijack could enable MITM attacks against OAuth token flows or extension downloads.

**Recommendation:** Add domain-specific `<pin-set>` entries for tracker OAuth endpoints:
```xml
<domain-config cleartextTrafficPermitted="false">
    <domain includeSubdomains="true">api.myanimelist.net</domain>
    <pin-set>
        <pin digest="SHA-256"><!-- leaf cert hash --></pin>
        <pin digest="SHA-256"><!-- backup cert hash --></pin>
    </pin-set>
</domain-config>
```

---

### 2. ProGuard — Missing Keep Rules for Extension Reflection Paths

**File:** `app/proguard-rules.pro`

Current ProGuard rules cover Hilt, Room, Retrofit, and Coil but do not include explicit keep directives for extension ClassLoader entry points or Tachiyomi interface implementations. If R8 obfuscates class/method names referenced in extension manifests, class loading fails at runtime.

**Recommendation:** Add:
```
-keep class app.otakureader.core.extension.** { *; }
-keep class * implements app.otakureader.domain.extension.Extension { *; }
-keep class app.otakureader.core.tachiyomi.compat.** { *; }
```

---

## Passes ✓

| Check | Result | Detail |
|-------|--------|--------|
| Cleartext traffic | ✓ Blocked globally | `cleartextTrafficPermitted="false"` in network_security_config.xml |
| User cert trust | ✓ Not trusted | Only system CAs trusted |
| OAuth token encryption | ✓ AES-256-GCM | `TrackerTokenStore` uses `EncryptedSharedPreferences` with Keystore-backed `MasterKey` |
| PKCE implementation | ✓ Compliant | `code_verifier` generated with `SecureRandom` (≥256 bits entropy); `state` validated on callback |
| OAuth session TTL | ✓ Enforced | Stale tokens cleared on re-auth; `PendingOAuthStore` scoped to activity lifecycle |
| OPDS credential encryption | ✓ AES-256-GCM | `EncryptedOpdsCredentialStore` uses same Keystore pattern |
| `android:exported` completeness | ✓ All components | All `<activity>`, `<receiver>`, `<provider>`, `<service>` have explicit `android:exported` |
| `android:debuggable` | ✓ Not set in release | Controlled by AGP; not hardcoded |
| FileProvider | ✓ `exported="false"` | Properly restricted with `<paths>` |
| `QUERY_ALL_PACKAGES` | ✓ Absent | Extension APK discovery uses targeted `PackageManager` queries |
| Deep link `autoVerify` | ✓ Present | HTTP intents include `android:autoVerify="true"` |
| Extension signature verification | ✓ Implemented | `ExtensionSignatureVerifier` validates APK signing certificate before classloading |
| ClassLoader isolation | ✓ Present | `ChildFirstPathClassLoader` (or equivalent) used for extension APKs |
| Hardcoded secrets | ✓ None found | No API keys, tokens, or passwords in source; crash reporter sanitizes PII |
| Data backup | ✓ Disabled | `android:allowBackup="false"` in manifest |
| R8 obfuscation | ✓ Enabled | `minifyEnabled = true` for release builds |

---

## Score: 8.5 / 10

Excellent credential storage, PKCE OAuth, network configuration, and extension security. Two P2 recommendations (certificate pinning, ProGuard extension rules) would bring this to 9.5/10 and are advisable before public beta.
