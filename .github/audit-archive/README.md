# Audit Archive

Historical audit snapshots generated **2026-05-24** — one day before alpha ship (2026-05-25).

These files document the state of the codebase at the pre-alpha audit. They are preserved for architectural reference but are **not actively maintained**. Open issues from these audits are tracked as GitHub issues [#926–#958](https://github.com/Heartless-Veteran/Otaku-Reader/issues).

## Files

| File | Covers |
|------|--------|
| `AUDIT_MASTER.md` | Overall readiness scores and gates summary |
| `AUDIT_ARCHITECTURE.md` | Layer violations, DI, MVI compliance |
| `AUDIT_CODE_SMELLS.md` | Unguarded catch blocks, naming, patterns |
| `AUDIT_FEATURES.md` | Pre-alpha feature gap analysis vs Mihon/Komikku |
| `AUDIT_PERFORMANCE.md` | WorkManager gaps, DB indexing, memory |
| `AUDIT_SECURITY.md` | Credential storage, extension sandboxing, network |
| `AUDIT_TESTING.md` | Coverage gaps and untested critical paths |
| `AUDIT_UI.md` | Accessibility (contentDescription), layout issues |

`PATCH_QUEUE.md` was removed — its items were either applied in alpha PRs #920–#925 / PR #1011, or promoted to the GitHub issues backlog.
