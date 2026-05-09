# Extension Loader Consolidation

## Problem

There is intentional code duplication between:

- `core/extension/loader/ExtensionLoadingUtils.kt` (or constants within `ExtensionLoader.kt`)
- `core/tachiyomi-compat/TachiyomiExtensionLoader.kt`

Both define the same Tachiyomi-compatible constants for APK manifest metadata parsing:

| Constant | Value | Purpose |
|---|---|---|
| `TACHIYOMI_EXTENSION_FEATURE` | `"tachiyomi.extension"` | The `<uses-feature>` tag that identifies an extension APK |
| `METADATA_SOURCE_CLASS` | `"tachiyomi.extension.class"` | Manifest meta-data key for the source class name |
| `METADATA_SOURCE_FACTORY` | `"tachiyomi.extension.factory"` | Manifest meta-data key for the source factory class |
| `METADATA_NSFW` | `"tachiyomi.extension.nsfw"` | Manifest meta-data key for NSFW flag |

## Why It Can't Be Consolidated

The two modules have a dependency direction that makes consolidation impossible:

```
core:extension → depends on → core:tachiyomi-compat (for Tachiyomi models and source API stubs)
```

The reverse dependency — `core:tachiyomi-compat` depending on `core:extension` — would create a **circular Gradle dependency**, which is illegal.

`TachiyomiExtensionLoader.kt` lives in `core:tachiyomi-compat` because it bridges Tachiyomi APK loading into the Otaku Reader extension system. It needs the Tachiyomi model stubs (`SManga`, `SChapter`, etc.) and the `source-api` interfaces, which are all in `core:tachiyomi-compat`.

`ExtensionLoader.kt` / `ExtensionLoadingUtils.kt` lives in `core:extension` because it orchestrates the full extension lifecycle — signature validation, DEX loading, repository lookup, trust store checks — and needs access to the app's Room database, preferences, and network stack.

## Maintenance Rule

**If either file changes any of the four constants above, the other must be updated in the same commit.**

These are the only duplicated constants that must be kept in sync:

- `TACHIYOMI_EXTENSION_FEATURE`
- `METADATA_SOURCE_CLASS`
- `METADATA_SOURCE_FACTORY`
- `METADATA_NSFW`

All other logic in `TachiyomiExtensionLoader.kt` (the actual APK parsing via `PackageManager.getPackageArchiveInfo()`, the `DexClassLoader` instantiation, the reflection-based `CatalogueSource` wrapping) is specific to the Tachiyomi bridge and does not need to match `ExtensionLoader.kt`.

## Future Resolution (if ever)

If the project ever reorganizes modules such that the Tachiyomi bridge can live inside `core:extension` (or a new shared module that both depend on), these constants could be extracted into a shared object. Until then, duplication is intentional and correct.
