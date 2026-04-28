# AI Removal Audit Report for Otaku-Reader

**Date**: 2026-04-29
**Branch**: main
**Status**: MOSTLY CLEAN — 4 items need attention

---

## ✅ CLEAN — No AI Found

| Category | Result |
|----------|--------|
| **AI/ML Dependencies** | ✅ None in libs.versions.toml (no Gemini, OpenAI, Claude, TensorFlow, ONNX) |
| **Firebase / Crashlytics** | ✅ No references found |
| **Cloud Sync Module** | ✅ Already extracted (PR #710) |
| **AI Modules in settings.gradle** | ✅ No `:feature:ai`, `:core:cloud`, `:feature:smart` included |
| **Smart Search Cache** | ✅ Table dropped in migration 23→24 |
| **Recommendations DB Tables** | ✅ Dropped in migration 25→26 |
| **Production Code** | ✅ No active AI API calls, no ML inference, no model loading |

---

## ⚠️ STILL PRESENT — Needs Removal

### 1. `feature/recommendations` Module (Stub)
**File**: `feature/recommendations/`
**What**: Empty stub module with disabled AI state. Still compiled into APK.
**Why remove**: Dead code, "AI Recommendations" branding in UI strings.
**Files to delete**:
- `feature/recommendations/` (entire module)
- `app/build.gradle.kts` line: `implementation(projects.feature.recommendations)`
- `settings.gradle.kts` line: `include(":feature:recommendations")`
- `OtakuReaderNavHost.kt` line 346: `// AI recommendations` comment + Route.Search fallback
- `MoreScreen.kt`: `more_recommendations` and `more_recommendations_desc` string references
- `LibraryNavigation.kt`: `onRecommendationClick` parameter (unused, defaults to `{}`)

### 2. `EncryptedApiKeyStore.kt` (Orphan)
**File**: `core/preferences/src/main/java/.../EncryptedApiKeyStore.kt`
**What**: Stores/rotates Gemini API key. Never bound in Hilt, never called.
**Why remove**: Dead code. If we ever add AI back, we'd rewrite this anyway.
**Files to delete**:
- `EncryptedApiKeyStore.kt`
- `EncryptedApiKeyStoreRotationTest.kt` (test file)
- `AiKeyValidationTest.kt` in `feature/settings/src/test/`

### 3. Database Migration Artifacts
**File**: `core/database/src/main/java/.../DatabaseMigrations.kt`
**What**: Migrations 22→23 and 23→24 create then drop `recommendations` and `recommendation_refreshes` tables. Migration 20→21 creates then drops `smart_search_cache`.
**Action**: Keep historical migrations (needed for upgrades). These are already no-ops for new installs.

### 4. "AI Recommendations" Strings
**File**: `feature/recommendations/src/main/res/values/strings.xml`
**What**: User-facing strings say "AI Recommendations", "AI Features Not Available"
**Action**: Will be removed with the module.

---

## 📊 Summary

| Metric | Count |
|--------|-------|
| Active AI code in production | **0** ✅ |
| Dead AI stub modules | **1** (recommendations) |
| Orphan API key storage | **1** (EncryptedApiKeyStore) |
| AI-related test files | **2** |
| Total files to remove | **~15** |
| Risk level | **LOW** — nothing is actually running |

---

## 🎯 Recommended Action

Delete the 4 items above. Takes ~10 minutes. No functional impact — all are dead code.
