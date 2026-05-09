# Extension Loader Consolidation

## Why Two Loaders?

The extension system has two classes that load Tachiyomi-compatible APKs:

| Class | Module | Responsibility |
|-------|--------|---------------|
| `ExtensionLoader` | `core:extension` | Validates, installs, and trusts extensions. Returns `ExtensionLoadResult` domain types. Used by `ExtensionInstaller`. |
| `TachiyomiExtensionLoader` | `core:tachiyomi-compat` | Scans all installed packages, wraps sources in `TachiyomiSourceAdapter`. Used by `SourceRepositoryImpl`. |

Both classes share the same core loading logic (feature-flag detection, metadata reading, class instantiation via `ChildFirstPathClassLoader`).

## Why the Code Is Duplicated

A shared utility in `core:extension` cannot be consumed by `core:tachiyomi-compat` without creating a **circular Gradle dependency**:

```
core:extension  →  core:tachiyomi-compat   (already exists — ExtensionLoader uses tachiyomi stubs)
core:tachiyomi-compat  →  core:extension   (would create a cycle)
```

Extracting to a third module (e.g. `core:extension-api`) would be the long-term fix, but that is a larger refactor than the current stabilisation phase warrants.

## Constants That Must Stay in Sync

If either file changes any of these constants, the other file **must be updated in the same commit**:

| Constant | Value |
|----------|-------|
| `TACHIYOMI_EXTENSION_FEATURE` / `EXTENSION_FEATURE` | `"tachiyomi.extension"` |
| `METADATA_SOURCE_CLASS` | `"tachiyomi.extension.class"` |
| `METADATA_SOURCE_FACTORY` | `"tachiyomi.extension.factory"` |
| `METADATA_NSFW` | `"tachiyomi.extension.nsfw"` |

## Future Consolidation Path

When this module boundary causes enough friction, the fix is:

1. Create `core:extension-base` (pure Kotlin, no Android UI) with the shared constants and loading utilities.
2. Make both `core:extension` and `core:tachiyomi-compat` depend on `core:extension-base`.
3. Delete the duplicated code from both modules.

This is tracked but not scheduled.
