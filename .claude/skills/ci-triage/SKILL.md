# CI Triage — Otaku Reader

## CI Workflows Overview

| Workflow | File | Trigger | Jobs |
|---------|------|---------|------|
| Main CI | `ci.yml` | Push/PR to `main`, `develop` | Detekt, Ktlint, Unit Tests, Coverage Gate, Screenshot Tests (Roborazzi), Assemble |
| Build | `build.yml` | Push/PR | Build Debug APK |
| Release | `release.yml` | Tag push (`v*`) | Signed release APK, GitHub Release |
| Benchmark | `benchmark.yml` | Manual dispatch | Baseline profile generation |
| Preview APK | `build_preview.yml` | PR trigger | Preview APK build |
| Cert Pin | `cert-pin-check.yml` | Push/PR | Certificate pinning verification |
| Extension Smoke | `extension-smoke-test.yml` | Push/PR | Extension loading smoke tests |
| Website | `pages.yml` | Push to `main` | VitePress build → GitHub Pages deploy |
| Labels | `label.yml` | PR events | Auto-label PRs by changed paths |
| CodeQL | `.github/workflows/codeql.yml` | Push/PR/schedule | Static security analysis |
| Review | `review-on-mention.yml` | PR comments | Copilot review on @mention |

---

## Merge Criteria

All of the following must be green before merging a PR:

| Check | Notes |
|-------|-------|
| Unit Tests | No failures allowed |
| Detekt | No new violations |
| Ktlint | No formatting issues |
| Assemble | Debug APK must build |
| Build Debug APK | Full APK artifact |
| Coverage Gate | Domain ≥60%, Data ≥35%, Database ≥25% (ratchet — never lower) |
| Screenshot Tests (Roborazzi) | No unexpected visual regressions |
| Security Check | No new security issues |
| CodeQL | Must pass (see flake section below) |

---

## Known CI Flakes

### CodeQL — "Analyze (java-kotlin)" Intermittent Failure

**Symptom:** `Analyze (java-kotlin)` fails with:
```
CodeQL could not process any code written in Java/Kotlin.
```

**Root cause:** GitHub's CodeQL autobuild runner sometimes fails to finalize the database even when the Gradle build itself succeeds ("BUILD SUCCESSFUL in 43s" appears in the same log). This is an infra flake — the code is fine.

**How to identify it's a flake:**
1. The job log shows "BUILD SUCCESSFUL" before the failure.
2. A second, concurrent or subsequent run of `Analyze (java-kotlin)` (different run ID) completed with `conclusion: success`.
3. No code change was made to the affected files.

**Action:** If the successful `Analyze (java-kotlin)` run is green, the stale failure is safe to ignore and you can merge.

**Action if both fail:** Investigate — this may indicate a real Kotlin compile error introduced in the PR.

---

## How to Check CI Status (via GitHub MCP)

```
mcp__github__pull_request_read(
    method="get_check_runs",
    owner="Heartless-Veteran",
    repo="Otaku-Reader",
    pullNumber=<PR number>
)
```

Look at `conclusion` for each check:
- `"success"` — passed
- `"failure"` — failed (investigate)
- `null` / `"in_progress"` — still running

For a failed check, get logs:
```
mcp__github__get_job_logs(
    owner="Heartless-Veteran",
    repo="Otaku-Reader",
    job_id=<id from check_runs>,
    return_content=true,
    tail_lines=50
)
```

---

## Common CI Failure Causes & Fixes

### Unit Test Failure
**Check:** What test failed, in which module.
**Common causes:**
- Effect type changed in ViewModel but test still asserts old type (e.g., `ShowSnackbar` → `ShowUndoBatchSnackbar`)
- `advanceUntilIdle()` missing after triggering a delayed coroutine
- Mock not set up for a new method call

**Fix pattern:** Update the test assertion to match the new effect type and structure. See `HistoryViewModelTest` for the `ShowUndoBatchSnackbar` example.

### Detekt Failure
**Check:** Which rule fired. Common ones:
- `UnusedPrivateMember` — unused `@Suppress` annotation left over
- `MagicNumber` — use a named constant in `companion object`
- `ForbiddenImport` — layer violation (feature importing data directly)

### Ktlint Failure
**Fix:** Run `./gradlew ktlintFormat` locally and commit the result.

### Coverage Gate Failure
**Check:** Which module dropped below its floor.
**Fix:** Add tests. Never lower the floor — floors only increase.
Current floors: domain=60%, data=35%, database=25%, reader=15%, tracking=15%, settings=5%.

### Build Failure
**Check:** Compilation error or missing dependency.
**Common causes:**
- New sealed class branch added but `when` expression in Screen not updated
- Hilt binding missing (`@Provides` / `@Binds` not added to the DI module)
- KSP annotation processor error — check for `@HiltViewModel` on a non-ViewModel class

---

## Merge Procedure

1. Check all CI checks via `get_check_runs` — confirm all are `"success"` (or the known CodeQL flake pattern).
2. If Unit Tests failed: read the test logs, fix the test or the code, push a new commit, wait for CI.
3. If CodeQL is the only failure and a successful CodeQL run exists for the same commit: proceed to merge.
4. Merge using squash: `mcp__github__merge_pull_request(merge_method="squash")`.
5. Write a descriptive squash commit title and body summarizing all changes in the PR.
6. Per standing instruction: "when it's all green merge."

---

## Coverage Gate Details

Kover 0.9.8 measures coverage for real (prior Kover 0.8.x with AGP 9 passed vacuously). Current gate floors (module → minimum %):

| Module | Floor |
|--------|-------|
| `:domain` | 60% |
| `:data` | 35% |
| `:core:database` | 25% |
| `:feature:reader` | 15% |
| `:feature:tracking` | 15% |
| `:feature:settings` | 5% |

Run locally: `./gradlew koverXmlReportDebug` then check the XML report.
