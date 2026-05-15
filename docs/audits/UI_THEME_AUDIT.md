# Otaku Reader — UI / Theme Audit Report

> Scope: `feature/*/`, `core/ui/`, `app/src/main/`  
> Focus: Material3 compliance, theme consistency, dark mode, accessibility, typography, screen adaptability, loading states, ripple feedback

---

## Critical

- **File**: `feature/details/src/main/java/app/otakureader/feature/details/DetailsMvi.kt:277-282`
- **Issue**: Manga status colors are six hardcoded `Color(0xFF...)` hex literals (green, blue, orange, purple, red, yellow) mapped to `MangaStatus` enum. These colors are not pulled from `MaterialTheme.colorScheme`, so they will not adapt to user theme or dynamic color.
- **Severity**: **Critical**
- **Suggestion**: Map status colors to semantic theme slots (e.g., `primary`, `tertiary`, `error`, `outline`) or add `statusOngoing`, `statusCompleted`, etc. to the custom `OtakuColors` palette so they respect light/dark and dynamic theming.

---

- **File**: `feature/reader/src/main/java/app/otakureader/feature/reader/ui/ReaderMenuOverlay.kt:539`
- **Issue**: Color preset chips use `Color(0xFF000000L or preset.rgb)` — a raw `Color` constructor that bypasses the theme entirely. The reader background color picker writes these raw values to persistence and applies them directly.
- **Severity**: **Critical**
- **Suggestion**: Store only the ARGB `Long` value internally, but expose the chip through a theme-aware wrapper. If the preset is meant to be a fixed palette, document it; otherwise derive preset chips from `MaterialTheme.colorScheme` swatches so they harmonize with the active theme.

---

- **File**: `feature/reader/src/main/java/app/otakureader/feature/reader/ReaderScreen.kt:453`
- **Issue**: Grayscale color-filter mode draws a hardcoded `Color(0xFF808080)` rectangle with `BlendMode.Saturation`. This is a magic constant that may not produce consistent grayscale across different display profiles.
- **Severity**: **Critical**
- **Suggestion**: Define `GrayscaleFilterColor` in the theme or use a well-named constant. If the value must remain neutral gray, reference it through the theme system (e.g., `Color.Gray` or a custom semantic color) rather than a raw hex literal.

---

## Warning

- **File**: `feature/library/src/main/java/app/otakureader/feature/library/LibraryScreen.kt:642-651`
- **Issue**: `ContinueReadingCard` hardcodes `Color.Black` and `Color.White` for its gradient overlay and text. In a dark theme, black may be too harsh; in a forced-light widget/theme scenario, white text may clash.
- **Severity**: **Warning**
- **Suggestion**: Use `MaterialTheme.colorScheme.surface` / `onSurface` with appropriate alpha, or add dedicated `overlayGradientStart` / `overlayGradientEnd` / `overlayText` tokens to `OtakuColors`.

---

- **File**: `core/ui/src/main/java/app/otakureader/core/ui/components/OtakuComponents.kt:64, 86`
- **Issue**: `OtakuChip` reads colors from `LocalOtakuColors.current` instead of `MaterialTheme.colorScheme`. The custom palette is disconnected from Material3 dynamic theming and may drift from the system theme.
- **Severity**: **Warning**
- **Suggestion**: Provide a mapping layer so `OtakuChip` resolves semantic colors (selected/unselected/background) from `MaterialTheme.colorScheme` first, falling back to `LocalOtakuColors` only when the palette is explicitly customized.

---

- **File**: `core/ui/src/main/java/app/otakureader/core/ui/modifiers/Modifiers.kt:90-100`
- **Issue**: `Modifier.ripple()` uses `LocalOtakuColors.current.ripple` rather than `MaterialTheme.colorScheme` ripple defaults. This creates a second source of truth for ripple color.
- **Severity**: **Warning**
- **Suggestion**: Default ripple tint to `MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)` (or the Material3 ripple spec) and only override via `LocalOtakuColors` when the user explicitly sets a custom accent.

---

- **File**: `feature/browse/src/main/java/app/otakureader/feature/browse/BrowseScreen.kt:434-448` (MangaCard)
- **Issue**: `AsyncImage` inside `MangaCard` has **no `contentDescription`** set. Screen readers will announce it as "unlabeled image."
- **Severity**: **Warning**
- **Suggestion**: Add `contentDescription = manga.title` (or a localized "Cover of ${manga.title}" string) to the `AsyncImage`.

---

- **File**: `feature/history/src/main/java/app/otakureader/feature/history/HistoryScreen.kt:364, 372`
- **Issue**: `IconButton`s for chapter resume and delete are sized to `32.dp`, well below the Material3 minimum touch target of `48.dp`. This makes them difficult to tap, especially for users with motor impairments.
- **Severity**: **Warning**
- **Suggestion**: Remove the explicit `.size(32.dp)` on `IconButton`. `IconButton` already enforces a 48.dp ripple/touch target by default; shrinking it breaks accessibility. If the visual icon must stay small, keep the button at default size and let the icon scale down inside.

---

- **File**: `feature/browse/src/main/java/app/otakureader/feature/browse/GlobalSearchScreen.kt:195, 206, 221, 258`
- **Issue**: Fixed dimensions `.height(160.dp)`, `.height(48.dp)`, `.width(100.dp)` are used for search result items and chips. On tablets or foldables in expanded width, these hardcoded sizes can look comically small or leave excessive whitespace.
- **Severity**: **Warning**
- **Suggestion**: Use `Modifier.fillMaxWidth(fraction = ...)` or adaptive width modifiers (e.g., `weight` inside `Row`, or `LazyVerticalGrid` with adaptive cells) instead of fixed dp widths for list/grid items.

---

- **File**: `feature/details/src/main/java/app/otakureader/feature/details/DetailsScreen.kt:562`
- **Issue**: Related-manga cover thumbnail uses a fixed `.size(width = 100.dp, height = 150.dp)`. On large screens or in landscape, this may appear too small or create uneven spacing in the horizontal scroll.
- **Severity**: **Warning**
- **Suggestion**: Use adaptive sizing — e.g., `Modifier.widthIn(max = 120.dp)` with `aspectRatio(2f/3f)` — or make the row scrollable with snap behavior so cards scale with container width.

---

- **File**: `feature/updates/src/main/java/app/otakureader/feature/updates/UpdatesScreen.kt:155-186`
- **Issue**: Loading, error, and empty states are implemented as **inline `Box + Text/CircularProgressIndicator`** rather than using the shared `LoadingScreen`, `ErrorScreen`, or `EmptyScreen` components from `core/ui`. This produces inconsistent styling, spacing, and missing retry actions across screens.
- **Severity**: **Warning**
- **Suggestion**: Replace the inline Boxes with the standardized `LoadingScreen`, `ErrorScreen`, and `EmptyScreen` components (or extend those components to accept an optional `onRetry` lambda).

---

- **File**: `feature/feed/src/main/java/app/otakureader/feature/feed/FeedScreen.kt:112-140`
- **Issue**: Same as above — `FeedScreen` implements its own inline loading/error/empty states instead of reusing `core/ui` shared state components.
- **Severity**: **Warning**
- **Suggestion**: Unify with `LoadingScreen`, `ErrorScreen`, and `EmptyScreen` to ensure consistent padding, typography, and iconography across all feature screens.

---

- **File**: `feature/history/src/main/java/app/otakureader/feature/history/HistoryScreen.kt:149-171`
- **Issue**: `HistoryScreen` also uses custom inline loading/error/empty boxes rather than the shared components.
- **Severity**: **Warning**
- **Suggestion**: Migrate to the shared `LoadingScreen` / `ErrorScreen` / `EmptyScreen` components for visual consistency.

---

- **File**: `feature/reader/src/main/java/app/otakureader/feature/reader/ui/TapZoneOverlay.kt:115-145` (SimpleTapZoneOverlay)
- **Issue**: All three tap zones use `.clickable(interactionSource = null, indication = null)`. While intentional for an invisible overlay, this **completely suppresses ripple and any visual feedback**. Users have no confirmation their tap registered.
- **Severity**: **Warning**
- **Suggestion**: Keep `indication = null` for the production overlay, but consider adding a brief flash or haptic feedback on tap. Alternatively, expose a debug mode that shows zone borders with a subtle ripple so users can learn the zones.

---

- **File**: `app/src/main/java/app/otakureader/widget/ContinueReadingWidget.kt:123, 140, 170, 178`
- **Issue**: Glance widget uses hardcoded `fontSize = 18.sp`, `14.sp`, `14.sp`, `12.sp`. These do not scale with the system font size preference (`fontScale`), making widgets inaccessible for users who need larger text.
- **Severity**: **Warning**
- **Suggestion**: Use `TextDefaults` from Glance or scale sizes manually via `LocalDensity.current.fontScale`. Alternatively, switch to `FontSize.Sp` with the system scale applied.

---

- **File**: `app/src/main/java/app/otakureader/widget/UnreadCountWidget.kt:103, 114, 123`
- **Issue**: Same as above — hardcoded `fontSize` values in the unread-count widget.
- **Severity**: **Warning**
- **Suggestion**: Apply system font scaling or use Glance's default typography that respects accessibility settings.

---

- **File**: `app/src/main/java/app/otakureader/widget/NowReadingWidget.kt:127, 139, 147, 157`
- **Issue**: Same pattern — hardcoded `fontSize` values in the now-reading widget.
- **Severity**: **Warning**
- **Suggestion**: Use scalable text units or Glance's built-in text styles.

---

- **File**: `app/src/main/java/app/otakureader/widget/RecentUpdatesWidget.kt:112, 130, 156, 164`
- **Issue**: Same pattern — hardcoded `fontSize` values in the recent-updates widget.
- **Severity**: **Warning**
- **Suggestion**: Apply system font scaling or use Glance's default typography.

---

## Polish

- **File**: `feature/reader/src/main/java/app/otakureader/feature/reader/ui/ReaderMenuOverlay.kt:288, 335, 380`
- **Issue**: Several `.clickable(onClick = ...)` calls inside the reader menu overlay do not pass an explicit `indication` parameter. Because they sit on a `Surface` with near-opaque background, the default ripple may be visually weak or clipped.
- **Severity**: **Polish**
- **Suggestion**: Add `indication = ripple(bounded = true, radius = 24.dp)` or wrap clickables in `IconButton` / `TextButton` where appropriate so the ripple is clearly visible against the overlay surface.

---

- **File**: `feature/details/src/main/java/app/otakureader/feature/details/DetailsScreen.kt:587, 1336, 1352, 1368, 1384`
- **Issue**: Multiple headings and labels use hardcoded `fontWeight = FontWeight.Bold` / `SemiBold` instead of using `MaterialTheme.typography` styles (e.g., `titleLarge`, `labelLarge`). This creates visual inconsistency if the user changes the system font or the app switches to a different type scale.
- **Severity**: **Polish**
- **Suggestion**: Map UI hierarchy to `MaterialTheme.typography` tokens. Bold accents should be expressed by choosing the correct style (e.g., `titleMedium` is already weight 500/700 depending on the font) rather than overriding weight manually.

---

- **File**: `core/ui/src/main/java/app/otakureader/core/ui/components/MangaCard.kt:143`
- **Issue**: Status badge container is sized to `36.dp`. While not egregious, it is below the recommended 48.dp accessible touch target if the badge is meant to be tappable (e.g., for filtering by status).
- **Severity**: **Polish**
- **Suggestion**: If the badge is purely decorative, no change needed. If it is clickable, increase the touch target to at least 48.dp or wrap it in a `Box` with `minimumTouchTargetSize()`.

---

- **File**: `feature/more/src/main/java/app/otakureader/feature/more/MoreScreen.kt:250`
- **Issue**: Avatar image in the "More" screen uses a fixed `.size(52.dp)`. On very large tablets or in landscape multi-pane layouts, this can look disproportionately small next to larger text.
- **Severity**: **Polish**
- **Suggestion**: Use a relative size or clamp (e.g., `.sizeIn(max = 64.dp)` with `aspectRatio(1f)`) so the avatar scales with the available space.

---

- **File**: `feature/library/src/main/java/app/otakureader/feature/library/LibraryScreen.kt:611`
- **Issue**: `MangaDetailPanel` has a `.clickable(onClick = onClick)` with no explicit indication. The panel sits inside a `Row` with `weight(0.45f)` on expanded width; ripple feedback may be subtle on large tablet screens.
- **Severity**: **Polish**
- **Suggestion**: Use `Surface(onClick = ...)` or `Card(onClick = ...)` (Material3 components) which provide default Material ripple automatically, rather than a raw `Modifier.clickable`.

---

## Summary Table

| # | File | Issue | Severity |
|---|------|-------|----------|
| 1 | `DetailsMvi.kt:277-282` | Hardcoded status colors (6 hex literals) | Critical |
| 2 | `ReaderMenuOverlay.kt:539` | Raw `Color(0xFF000000L or preset.rgb)` for reader bg | Critical |
| 3 | `ReaderScreen.kt:453` | Hardcoded `Color(0xFF808080)` for grayscale filter | Critical |
| 4 | `LibraryScreen.kt:642-651` | ContinueReadingCard hardcodes Black/White overlay | Warning |
| 5 | `OtakuComponents.kt:64,86` | `OtakuChip` uses `LocalOtakuColors` not `colorScheme` | Warning |
| 6 | `Modifiers.kt:90-100` | Custom ripple uses `LocalOtakuColors` not `colorScheme` | Warning |
| 7 | `BrowseScreen.kt:434-448` | `MangaCard` `AsyncImage` missing `contentDescription` | Warning |
| 8 | `HistoryScreen.kt:364,372` | `IconButton`s at `32.dp` (below 48dp touch target) | Warning |
| 9 | `GlobalSearchScreen.kt:195-258` | Fixed `160.dp`/`48.dp`/`100.dp` dimensions | Warning |
| 10 | `DetailsScreen.kt:562` | Related manga cover fixed at `100x150.dp` | Warning |
| 11 | `UpdatesScreen.kt:155-186` | Inline loading/error/empty, not shared components | Warning |
| 12 | `FeedScreen.kt:112-140` | Inline loading/error/empty, not shared components | Warning |
| 13 | `HistoryScreen.kt:149-171` | Inline loading/error/empty, not shared components | Warning |
| 14 | `TapZoneOverlay.kt:115-145` | `clickable(indication = null)` — zero feedback | Warning |
| 15 | `ContinueReadingWidget.kt` | Hardcoded `fontSize` values (no system scaling) | Warning |
| 16 | `UnreadCountWidget.kt` | Hardcoded `fontSize` values (no system scaling) | Warning |
| 17 | `NowReadingWidget.kt` | Hardcoded `fontSize` values (no system scaling) | Warning |
| 18 | `RecentUpdatesWidget.kt` | Hardcoded `fontSize` values (no system scaling) | Warning |
| 19 | `ReaderMenuOverlay.kt:288,335,380` | `.clickable` without explicit ripple on overlay | Polish |
| 20 | `DetailsScreen.kt` | Multiple `fontWeight = Bold/SemiBold` overrides | Polish |
| 21 | `MangaCard.kt:143` | Badge container at `36.dp` touch target | Polish |
| 22 | `MoreScreen.kt:250` | Avatar fixed at `52.dp` | Polish |
| 23 | `LibraryScreen.kt:611` | `MangaDetailPanel.clickable` lacks clear indication | Polish |

---

> **Bottom line:** The biggest risks are the **hardcoded color literals** (especially status colors and the reader background picker) and the **accessibility gaps** (missing content descriptions, sub-48dp touch targets, and widget font scaling). Fixing those first will materially improve theming consistency and accessibility compliance.
