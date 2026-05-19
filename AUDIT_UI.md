# AUDIT_UI.md — Otaku-Reader Android Jetpack Compose UI Audit

---

## 1. Executive Summary

**Compose Maturity Grade: C+**

The codebase demonstrates solid architectural intent (MVI, Hilt, Lifecycle-aware collection, proper `key` usage in most lists, content type hints) and a rich visual design system (`OtakuColors`, dynamic color, OLED mode, sepia mode). However several structural anti-patterns create measurable recomposition overhead, OOM risk, and accessibility gaps that prevent a higher grade.

### Top 5 Issues

| # | Severity | Issue | Location |
|---|----------|-------|----------|
| 1 | **Critical** | `DetailsScreen.kt` is ~69 KB / 1,700+ lines — a single god composable triggering full-tree recomposition on every state change | `DetailsScreen.kt` |
| 2 | **Critical** | `NeonSlider` calls `System.currentTimeMillis()` on every Canvas draw frame — causes infinite recomposition loops | `NeonSlider.kt` |
| 3 | **High** | `displayedManga` filter computed inline in `MangaGrid` body without `derivedStateOf` — re-filters the entire list on every recomposition | `LibraryScreen.kt` |
| 4 | **High** | `renderMarkdown()` called unconditionally inside the composable body (not in `remember`) — re-runs regex parsing on every recomposition | `DetailsScreen.kt` |
| 5 | **High** | `ContinueReadingCard` and `MangaHeader` use hardcoded `Color.White` / `Color.Black` overlay text that is invisible or illegible in light theme | `LibraryScreen.kt`, `DetailsScreen.kt` |

---

## 2. Recomposition Analysis

### 2.1 `MangaGrid` — Inline filter without `derivedStateOf`

**File:** `feature/library/src/main/java/app/otakureader/feature/library/LibraryScreen.kt`

```kotlin
// BEFORE — re-evaluates on every recomposition of MangaGrid
val displayedManga = when (selectedContentFilter) {
    1 -> state.mangaList.filter { !isManhwa(it) }
    2 -> state.mangaList.filter { isManhwa(it) }
    else -> state.mangaList
}
```

Every time *any* part of `LibraryState` changes (e.g. `selectedManga` set on long-press), `MangaGrid` recomposes and the `filter {}` runs over potentially thousands of items. Because `displayedManga` is a plain `val` (not a Compose state), it is not itself a recomposition trigger, but the list object identity changes on every call so all downstream grid items are invalidated.

**Fix:**
```kotlin
// AFTER
val displayedManga by remember(state.mangaList, selectedContentFilter) {
    derivedStateOf {
        when (selectedContentFilter) {
            1 -> state.mangaList.filter { !isManhwa(it) }
            2 -> state.mangaList.filter { isManhwa(it) }
            else -> state.mangaList
        }
    }
}
```

### 2.2 `isManhwa()` — Pure function called per grid item, per recomposition

**File:** `LibraryScreen.kt`

`isManhwa(manga)` is called twice per item in `MangaGrid` (once in the `staggeredItems` block, once in `gridItems`). It does a `toString()` on `sourceId` plus four `contains()` checks. With 500 items and 4 columns this is 2,000 string allocations per recomposition. The result should be cached on `LibraryMangaItem` (a domain model property), or at minimum memoized with `remember(manga.sourceId)`.

### 2.3 `AnimatedVisibility(visible = true, ...)` — Animates nothing, triggers every frame

**File:** `LibraryScreen.kt` — `MangaGrid` staggered and fixed grid blocks

```kotlin
// BEFORE — AnimatedVisibility with a permanently-true condition is a no-op that
// still runs animation infrastructure on every item every recomposition
AnimatedVisibility(visible = true, enter = fadeIn(tween(300)) + scaleIn(...)) {
    cardContent()
}
```

`visible = true` means the animation never runs, yet Compose still instantiates `AnimatedVisibility`'s internal `EnterExitState` machinery and checks it on every recomposition for every visible grid item. With 100+ items in a large library, this is significant overhead.

**Fix:** Drive the animation with a real state key. If the intent is an enter animation on first composition, use a `remember { mutableStateOf(false) }` launched to `true` via `LaunchedEffect(Unit)`, or use `AnimatedContent` on a meaningful state change.

### 2.4 `NeonSlider` — `System.currentTimeMillis()` in Canvas draw scope

**File:** `core/ui/src/main/java/app/otakureader/core/ui/components/NeonSlider.kt`

```kotlin
// BEFORE — System.currentTimeMillis() inside Canvas lambda = reads wall clock on
// every draw frame; because it always returns a new value it prevents the Canvas
// from ever being skipped by the skip-if-equal optimisation.
val sparkleAlpha = ((kotlin.math.sin(
    (System.currentTimeMillis() + sparkle.phase) * 0.005
) + 1) / 2).toFloat() * 0.8f * pulseAlpha
```

This is the most expensive rendering bug in the codebase. `System.currentTimeMillis()` is not a Compose `State` object — reading it inside a Canvas draw lambda does **not** trigger recomposition (the Composition phase is separate from the Draw phase). However, because it always returns a new value, the draw lambda can never be skipped by Compose's equality optimization. If anything external drives frame invalidation (an `Animatable`, a `LaunchedEffect` update loop, or a parent state change), the Canvas will redraw on every frame using a wall-clock value that was never meant to be a continuous animation driver. The practical effect:
- Every animation tick or state update forces a full Canvas redraw instead of skipping unchanged frames.
- The draw lambda allocates on every frame (new `Float` from `sin()`).
- Any parent composable observing state near this slider participates in the continuous redraw cycle.

**Fix:** Use `rememberInfiniteTransition` to drive the sparkle phase as a proper animated `Float` state:

```kotlin
// AFTER
val sparkleTime by rememberInfiniteTransition(label = "sparkle")
    .animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "sparkleCycle"
    )

// In Canvas:
val sparkleAlpha = ((sin(sparkleTime + sparkle.phase * 0.001f) + 1) / 2f) * 0.8f * pulseAlpha
```

### 2.5 `renderMarkdown()` called directly inside composable body

**File:** `DetailsScreen.kt` — `MangaNotes`

```kotlin
// BEFORE — regex operations run on every recomposition
Text(text = renderMarkdown(notes), ...)
```

`renderMarkdown` creates a `buildAnnotatedString` block and runs three regex `find()` calls every recomposition. The `notes` string changes rarely; memoize it.

**Fix:**
```kotlin
// AFTER
val annotated = remember(notes) { renderMarkdown(notes) }
Text(text = annotated, ...)
```

### 2.6 `DetailsScreen` — `scrollOffset` lambda created inline, causes reference inequality

**File:** `DetailsScreen.kt` — `DetailsContent`

```kotlin
// BEFORE — new lambda object created on every recomposition, defeating skip
val scrollOffset: () -> Float = {
    if (listState.firstVisibleItemIndex == 0)
        listState.firstVisibleItemScrollOffset.toFloat()
    else Float.MAX_VALUE
}
```

Passed as a parameter to `detailsInfoItems` and ultimately `MangaHeader`. Because it is a new lambda on every recomposition, `MangaHeader` (which takes `scrollOffset: () -> Float`) will always recompose even when no scroll happened.

**Fix:** Capture the lambda in `remember`:
```kotlin
val scrollOffset = remember(listState) {
    { if (listState.firstVisibleItemIndex == 0)
        listState.firstVisibleItemScrollOffset.toFloat()
      else Float.MAX_VALUE }
}
```

### 2.7 `GlitchText` — Three `rememberInfiniteTransition` instances

**File:** `GlitchText.kt`

```kotlin
val offsetR by rememberInfiniteTransition(label = "glitchR").animateValue(...)
val offsetG by rememberInfiniteTransition(label = "glitchG").animateValue(...)
val offsetB by rememberInfiniteTransition(label = "glitchB").animateValue(...)
```

Each `rememberInfiniteTransition` creates a separate animation object and compositor node. All three could share one transition, reducing overhead:

```kotlin
val transition = rememberInfiniteTransition(label = "glitch")
val offsetR by transition.animateValue(...)
val offsetG by transition.animateValue(...)
val offsetB by transition.animateValue(...)
```

---

## 3. God Composables

### 3.1 `DetailsScreen.kt` — 69 KB, ~1,700 lines

This is the most critical structural issue. The file contains at least 25 `@Composable` private functions ranging from `MangaHeader` to `ChapterFilterDialog` to `CustomTintColorPicker`. While the private functions help readability, they all live in one file and `DetailsScreen` itself orchestrates all of them with a single `state: DetailsContract.State` parameter containing dozens of fields.

**Impact:**
- Any field change in `DetailsContract.State` (e.g., scroll position, chapter read toggle) triggers recomposition of the top-level `DetailsScreen` body, which re-evaluates every `when` branch and conditional.
- Android Studio layout inspector shows this as a single recomposition scope.
- File size inhibits incremental compilation — the entire file is recompiled on any change.

**Recommended extraction:**

| New File | Contents |
|---|---|
| `MangaHeader.kt` | `MangaHeader`, `CoverHero`, `PanoramaBanner` |
| `MangaInfoSection.kt` | `MangaDescription`, `MangaNotes`, `NoteEditorDialog` |
| `ReaderSettingsSheet.kt` | `ReaderSettingsSection`, `DirectionOption`, `ReaderModeOption`, `ColorFilterOption`, `CustomTintColorPicker`, `BackgroundColorPicker`, `PreloadOption` |
| `ChapterFilterSheet.kt` | `ChapterFilterDialog`, `TriStateRow` |
| `SourceSuggestionsRow.kt` | `SourceSuggestionsSection`, `SuggestionItem` |

Each extracted file should receive only the state slice it needs (not the full `DetailsContract.State`), enabling Compose's skip-if-equal optimisation.

### 3.2 `LibraryScreen.kt` — 39 KB

`MangaGrid` contains both the staggered-grid and fixed-grid paths (nearly identical code duplicated), plus the header composable lambda `headerContent`. The two grid code paths should be unified via a `@Composable` parameter or extracted into a `MangaGridContent` that accepts a generic layout strategy.

`LibraryScreen` also embeds three `TopAppBar` variants inline in a `when` block. Each should be extracted: `LibrarySelectionAppBar`, `LibrarySearchAppBar`, `LibraryNormalAppBar`.

### 3.3 `ChapterList.kt` — ~600 lines, nested `LazyColumn` inside `Column`

`ChapterList` wraps a `LazyColumn` inside a `Column(fillMaxSize)` with a fixed-size header. The outer `Column` has no scroll and `fillMaxSize`, so the `LazyColumn` correctly fills the remainder — but it means the entire component cannot participate in a parent lazy list (it would need `LazyListScope` extension functions instead, as correctly done in `DetailsScreen`'s `detailsChapterItems`). The `ChapterList` composable itself is unused for the main phone layout but may be used elsewhere; if used inside another scrollable container it will cause nested scrolling conflicts.

---

## 4. Side Effect Violations

### 4.1 `LaunchedEffect(viewModel.effect)` — Unstable key

**Files:** `LibraryScreen.kt`, `DetailsScreen.kt`, `ReaderScreen.kt`

```kotlin
// BEFORE
LaunchedEffect(viewModel.effect) {
    viewModel.effect.collectLatest { ... }
}
```

`viewModel.effect` is a `SharedFlow` or `Channel`. Its reference is stable (same object for the lifetime of the ViewModel), so this is functionally correct. However, using a Flow as a `LaunchedEffect` key is semantically misleading — if the ViewModel is ever recreated the key would change and the effect would restart, but a new ViewModel means a new `effect` reference anyway.

The preferred pattern is:
```kotlin
LaunchedEffect(Unit) {
    viewModel.effect.collectLatest { ... }
}
```
`Unit` makes the intent explicit: "run once per composition entry". `LibraryScreen` also wraps `snackbarHostState.showSnackbar()` in a nested `scope.launch` inside the `LaunchedEffect` coroutine — this is unnecessary since `LaunchedEffect` already provides a coroutine scope; the nested launch adds an extra coroutine.

### 4.2 `MangaHeader` — `LaunchedEffect` writing to `bloomProgress` local state

**File:** `DetailsScreen.kt`

```kotlin
var bloomProgress by remember { mutableFloatStateOf(0f) }
LaunchedEffect(manga.id) {
    val animatable = Animatable(0f)
    animatable.animateTo(1f, animationSpec = tween(800, ...))
    bloomProgress = animatable.value  // Always sets 1f
}
```

The final assignment `bloomProgress = animatable.value` always sets `1f` (the `animateTo` target). The intermediate values from `animateTo` are never read — `animatable.value` is not observed as state. The entire `bloomProgress` variable is unused during the animation itself. The `Animatable` should drive a `graphicsLayer` directly or use `animateFloatAsState`; the `bloomProgress` var and `LaunchedEffect` are dead code.

**Fix:**
```kotlin
var visible by remember(manga.id) { mutableStateOf(false) }
LaunchedEffect(manga.id) { visible = true }
val bloomProgress by animateFloatAsState(
    targetValue = if (visible) 1f else 0f,
    animationSpec = tween(800, easing = FastOutSlowInEasing),
    label = "bloom"
)
```

### 4.3 `HankoBadge` — Empty `LaunchedEffect`

**File:** `HankoBadge.kt`

```kotlin
LaunchedEffect(count) {
    // Trigger recomposition animation
}
```

This is dead code. An empty `LaunchedEffect` does nothing but register a coroutine that immediately completes. The comment suggests intent to animate on count change, but no animation is triggered. Remove it or implement the intended scale-pop animation.

### 4.4 `CoverColorExtraction` — `ImageLoader(context)` created per call

**File:** `CoverColorExtraction.kt`

```kotlin
val result = ImageLoader(context).execute(request)
```

A new `ImageLoader` instance is created on every `extractColorSchemeFromUrl` call. `ImageLoader` holds a thread pool, memory cache, and disk cache. Creating one per invocation leaks resources and bypasses any caching. This should use the application's singleton `ImageLoader` obtained via `context.imageLoader` (Coil3 extension).

---

## 5. Material3 Compliance

### 5.1 Dynamic Color — Correct but guarded only on API 31+

**File:** `OtakuReaderTheme.kt`

`colorScheme == 0` (System Default) correctly falls back to `DarkColorScheme`/`LightColorScheme` on API < 31. Dynamic color is fully implemented. **Grade: Pass.**

### 5.2 Custom accent scheme — Manual RGB arithmetic instead of `MaterialKolor` or HCT

**File:** `OtakuReaderTheme.kt` — `buildCustomLightScheme` / `buildCustomDarkScheme`

```kotlin
val containerColor = Color(
    red = (r + (255 - r) * 0.7f).toInt()...
)
```

Linear RGB interpolation does not produce perceptually balanced tones. Material3 color roles must be generated from the HCT color space (Hue/Chroma/Tone) to guarantee WCAG contrast ratios. The hand-rolled scheme may produce `primary`/`onPrimary` combinations with contrast ratios below 4.5:1.

**Recommendation:** Use the `material-color-utilities` or `MaterialKolor` library:
```kotlin
val scheme = Scheme.dark(argb) // from material-color-utilities
```

### 5.3 `ManhwaCard` — Hardcoded container color

**File:** `ManhwaCard.kt`

```kotlin
colors = CardDefaults.cardColors(
    containerColor = Color(0xFF16161F)
)
```

This is always near-black regardless of theme. In light theme or sepia mode the card background is jarring. It should be:
```kotlin
containerColor = MaterialTheme.colorScheme.surfaceContainerLow
```

### 5.4 `ManhwaCard` — Hardcoded neon purple accent

**File:** `ManhwaCard.kt`

```kotlin
Color(0xFF9B59B6)  // hardcoded in gradient rim, title shadow, badge glow, border
```

This color does not adapt to the user's chosen color scheme. It will clash with teal, green, or red accent themes. Should be `LocalOtakuColors.current.accent` or `MaterialTheme.colorScheme.secondary`.

### 5.5 `LibraryScreen` — Tab indicator hardcoded colours

**File:** `LibraryScreen.kt` — `MangaGrid` header tab row

```kotlin
val indicatorColor = when (selectedContentFilter) {
    1 -> Color(0xFFFF4757)   // Manga = hardcoded red
    2 -> Color(0xFF9B59B6)   // Manhwa = hardcoded purple
    else -> MaterialTheme.colorScheme.primary
}
```

The "Manga" and "Manhwa" tab indicators are hardcoded colours that ignore the user's colour scheme. The `contentTabs` list is also built from hardcoded string literals `"All"`, `"Manga"`, `"Manhwa"` without string resources.

### 5.6 `CoverColorExtraction` — Dark-mode surface colours are hardcoded

**File:** `CoverColorExtraction.kt`

```kotlin
background = Color(0xFF1A1A1A),
surface = Color(0xFF121212),
surfaceVariant = Color(0xFF2D2D2D),
```

These ignore the user's Pure Black (OLED) or Sepia settings. The surface roles should be derived from the existing theme infrastructure or left unspecified so M3 defaults apply.

### 5.7 `NeonSlider` — Track background hardcoded

**File:** `NeonSlider.kt`

```kotlin
drawRoundRect(color = Color(0xFF1E1E2A), ...)  // Always near-black
```

In light theme this is invisible against a white background. Should use `MaterialTheme.colorScheme.surfaceVariant.toArgb()` or be passed as a parameter.

---

## 6. Reader Engine UX

### 6.1 WebtoonReader — OOM risk mitigated but guard incomplete

**File:** `WebtoonReader.kt`

The `SubsamplingWebtoonDecoder.Factory()` is correctly remembered and handles tall single-strip images. However, `LazyColumn` in the Webtoon reader has no `beyondViewportPageCount` equivalent (it uses the default, which in a `LazyColumn` is 1 item above and 1 below the viewport). For high-resolution manga pages (4K+ pixels per page) this means 3 pages worth of decoded bitmaps reside in memory simultaneously. On a 4 GB device with a typical `LargeHeap` of 512 MB this can OOM with images >2000×3000 px.

**Recommendation:** Add a `DisposableEffect` that calls `ImageLoader.memoryCache?.clear()` when the reader is closed to release page bitmaps on exit.

### 6.2 `SinglePageReader` — Dual `LaunchedEffect` on `currentPage` and `pagerState`

**File:** `SinglePageReader.kt`

```kotlin
LaunchedEffect(currentPage) {
    if (pagerState.currentPage != currentPage) {
        pagerState.animateScrollToPage(currentPage)
    }
}
LaunchedEffect(pagerState) {
    snapshotFlow { pagerState.currentPage }
        .distinctUntilChanged()
        .collect { page -> onPageChange(page) }
}
```

When the user swipes to page N, `snapshotFlow` fires `onPageChange(N)` → ViewModel updates `currentPage` → `LaunchedEffect(currentPage)` fires `animateScrollToPage(N)` — but the pager is already at page N. The guard prevents an actual scroll, but the redundant coroutine launch still occurs on every page turn. Use `snapshotFlow` exclusively; let `currentPage` only drive programmatic jumps (deep links, chapter change):

```kotlin
LaunchedEffect(pagerState) {
    snapshotFlow { pagerState.currentPage }
        .distinctUntilChanged()
        .collect(onPageChange)
}
LaunchedEffect(externalJumpTarget) {
    if (externalJumpTarget != pagerState.currentPage)
        pagerState.animateScrollToPage(externalJumpTarget)
}
```

### 6.3 `ReaderScreen` — Duplicate `focusRequester.requestFocus()` effects

**File:** `ReaderScreen.kt`

```kotlin
LaunchedEffect(Unit) { focusRequester.requestFocus() }
LaunchedEffect(state.isMenuVisible, state.isGalleryOpen) { focusRequester.requestFocus() }
```

The `Unit` effect is subsumed by the second; merge them:
```kotlin
LaunchedEffect(state.isMenuVisible, state.isGalleryOpen) {
    focusRequester.requestFocus()
}
```

### 6.4 Image loading — No explicit `diskCacheKey` for signed CDN URLs

Signed CDN URLs with expiry parameters will cause Coil3 to use the full URL as the disk cache key. On expiry, the key misses and images are re-downloaded. Fix:
```kotlin
ImageRequest.Builder(context)
    .data(page.imageUrl)
    .diskCacheKey(page.id.toString())
    .memoryCacheKey(page.id.toString())
    .build()
```

### 6.5 `PageThumbnailStrip` and `PageSlider` — Simultaneous composition

**File:** `ReaderScreen.kt`

Both render at `Alignment.BottomCenter`. They should be wrapped in a single `AnimatedVisibility` rather than each managing their own visibility, to avoid doubling the recomposition scope on page state changes.

---

## 7. Accessibility Gaps

### 7.1 `LibraryGridItem` — Cover image has bare title, no role context

```kotlin
// BEFORE
AsyncImage(contentDescription = manga.title, ...)

// AFTER
AsyncImage(
    contentDescription = stringResource(R.string.library_cover_art_description, manga.title),
    ...
)
// string: "Cover art for %s"
```

### 7.2 `ContinueReadingCard` — Touch target may be under 48dp

```kotlin
// BEFORE
Card(modifier = modifier.width(130.dp).clickable(onClick = onClick), ...)

// AFTER
Card(modifier = modifier.width(130.dp).defaultMinSize(minHeight = 48.dp).clickable(onClick = onClick), ...)
```

### 7.3 `UnreadBadge` — No content description

The badge has no `Modifier.semantics { contentDescription = ... }`. TalkBack reads the raw number with no context. The outer `MangaCard` should have a merged semantics node that includes the unread count.

### 7.4 `MangaCard` — Missing `Role.Button`

```kotlin
// BEFORE
.combinedClickable(onClick = onClick, onLongClick = onLongClick)

// AFTER
.combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick)
```

### 7.5 `ChapterListItem` — Duplicate interactive elements for TalkBack

When `isSelected = true`, both the card's `combinedClickable` and the `Checkbox` fire `onClick()`. Use `Modifier.semantics(mergeDescendants = true)` on the parent row to collapse them into one accessibility node.

### 7.6 `DetailsScreen` — `IconButton` panorama toggle is 28dp

```kotlin
// BEFORE
modifier = Modifier.size(28.dp)  // Violates 48dp minimum

// AFTER — keep icon small, let IconButton provide the touch target
IconButton(onClick = onTogglePanoramaCover) {
    Icon(modifier = Modifier.size(20.dp), ...)
}
// IconButton defaults to 48dp touch target
```

### 7.7 Hardcoded English strings in `contentDescription`

| File | Hardcoded String | Fix |
|------|-----------------|-----|
| `LibraryGridItem.kt` | `"Completed"`, `"Dropped"` | `stringResource(R.string.badge_completed)` etc. |
| `DetailsScreen.kt` | `"More options"` | `stringResource(R.string.details_more_options)` |
| `NeonSlider.kt` | No description at all | Add semantics with `ProgressBarRangeInfo` |

### 7.8 `NeonSlider` — No semantics for screen readers

```kotlin
// Add to NeonSlider modifier — use string resources for localization
.semantics {
    contentDescription = stringResource(R.string.slider_description, (value * 100).toInt())
    progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
    stateDescription = stringResource(R.string.slider_state, (value * 100).toInt())
}
// Add to res/values/strings.xml:
// <string name="slider_description">Slider, %d%%</string>
// <string name="slider_state">%d%%</string>
```

---

## 8. Invalid API Usage

### 8.1 `TextShimmer` — All shimmer lines stacked at (0,0)

**File:** `ShimmerPlaceholders.kt`

```kotlin
// BEFORE — all lines overlap because Box stacks at (0,0)
Box(modifier = modifier) {
    repeat(lines) { index ->
        Box(modifier = Modifier.fillMaxWidth(widthFraction).height(lineHeight.dp)...)
    }
}

// AFTER
Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
    repeat(lines) { index ->
        val isFinal = index == lines - 1
        Box(
            modifier = Modifier
                .fillMaxWidth(if (isFinal) lastLineWidthPercent else 1f)
                .height(lineHeight.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmer()
        )
    }
}
```

This is a **visible rendering bug** — only the last shimmer line is currently rendered.

### 8.2 `HankoBadge` — `Shape.createOutline()` called per frame, no caching

**File:** `HankoBadge.kt`

The polygon path is rebuilt on every `createOutline` call. Cache the `Outline` keyed on `size`:

```kotlin
private object HankoShape : Shape {
    private var cachedSize: Size? = null
    private var cachedOutline: Outline? = null
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        if (size == cachedSize) return cachedOutline!!
        cachedSize = size
        cachedOutline = Outline.Generic(buildPath(size))
        return cachedOutline!!
    }
}
```

### 8.3 `formatDate` — `SimpleDateFormat` instantiated per `ChapterListItem` recomposition

**File:** `ChapterList.kt`

```kotlin
// BEFORE — new SimpleDateFormat on every call
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// AFTER — use API 26+ java.time or cache in remember
val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
```

### 8.4 `CoverColorExtraction` — `ImageLoader(context)` created per call

**File:** `CoverColorExtraction.kt`

```kotlin
// BEFORE — new ImageLoader (with thread pool + memory cache) on every call
val result = ImageLoader(context).execute(request)

// AFTER — use singleton
val result = context.imageLoader.execute(request)
```

---

## 9. Before/After Snippets for Critical Fixes

### Fix A — `MangaGrid` — Replace inline filter with `derivedStateOf`

```kotlin
// BEFORE (LibraryScreen.kt)
val displayedManga = when (selectedContentFilter) {
    1 -> state.mangaList.filter { !isManhwa(it) }
    2 -> state.mangaList.filter { isManhwa(it) }
    else -> state.mangaList
}

// AFTER
val displayedManga by remember(state.mangaList, selectedContentFilter) {
    derivedStateOf {
        when (selectedContentFilter) {
            1 -> state.mangaList.filter { !isManhwa(it) }
            2 -> state.mangaList.filter { isManhwa(it) }
            else -> state.mangaList
        }
    }
}
```

### Fix B — `NeonSlider` — Replace wall-clock read with `InfiniteTransition`

```kotlin
// BEFORE (NeonSlider.kt) — busy-loop recomposition
val sparkleAlpha = ((kotlin.math.sin(
    (System.currentTimeMillis() + sparkle.phase) * 0.005
) + 1) / 2).toFloat() * 0.8f * pulseAlpha

// AFTER — declare at composable scope
val sparklePhase by rememberInfiniteTransition(label = "sparkle")
    .animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI * 1000).toFloat(),
        animationSpec = infiniteRepeatable(tween(durationMillis = 2000, easing = LinearEasing)),
        label = "sparklePhase"
    )
// In Canvas:
val sparkleAlpha = ((sin((sparklePhase + sparkle.phase) * 0.001f) + 1) / 2f) * 0.8f * pulseAlpha
```

### Fix C — `MangaHeader` — Replace dead `Animatable` with `animateFloatAsState`

```kotlin
// BEFORE (DetailsScreen.kt)
var bloomProgress by remember { mutableFloatStateOf(0f) }
LaunchedEffect(manga.id) {
    val animatable = Animatable(0f)
    animatable.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
    bloomProgress = animatable.value  // always 1f
}

// AFTER
var bloomVisible by remember(manga.id) { mutableStateOf(false) }
LaunchedEffect(manga.id) { bloomVisible = true }
val bloomProgress by animateFloatAsState(
    targetValue = if (bloomVisible) 1f else 0f,
    animationSpec = tween(800, easing = FastOutSlowInEasing),
    label = "bloom"
)
```

### Fix D — `TextShimmer` — Fix overlapping lines

```kotlin
// BEFORE (ShimmerPlaceholders.kt) — lines all at (0,0)
Box(modifier = modifier) {
    repeat(lines) { index -> Box(Modifier.height(lineHeight.dp)...) }
}

// AFTER
Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
    repeat(lines) { index ->
        Box(
            modifier = Modifier
                .fillMaxWidth(if (index == lines - 1) lastLineWidthPercent else 1f)
                .height(lineHeight.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmer()
        )
    }
}
```

### Fix E — Hardcoded accessibility strings

```kotlin
// BEFORE (LibraryGridItem.kt)
Icon(contentDescription = "Completed", ...)
Icon(contentDescription = "Dropped", ...)

// AFTER
Icon(contentDescription = stringResource(R.string.library_badge_completed), ...)
Icon(contentDescription = stringResource(R.string.library_badge_dropped), ...)
```

### Fix F — `MangaCard` — Add `Role.Button`

```kotlin
// BEFORE
.combinedClickable(onClick = onClick, onLongClick = onLongClick)

// AFTER
.combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick)
```

---

*Audit conducted against commit `28a13cdd6e` (HEAD at time of review). Ruflo agent: `ui-viper` (ruflo-docs plugin).*
