# Session Rules

Permanent standing instructions for every session on Otaku-Reader. These override defaults and apply without exception.

## Attribution — NEVER add any of these anywhere

Do NOT add to commits, PR bodies, code comments, or any other artifact:
- `Co-Authored-By: Claude ...`
- `Claude-Session: https://...`
- `🤖 Generated with [Claude Code](...)`
- Any model identifier (claude-sonnet-4-6, etc.)

Model identity is for chat replies only — never in code artifacts.

## Commit Message Format

```
<imperative sentence describing what changed>

<optional body paragraph explaining why, if non-obvious>
```

No attribution lines. No bullet lists of changed files. No "Fix #NNN" unless the issue number is directly relevant.

## After Every Push

1. Check if a PR already exists for the branch.
2. If not, create a draft PR immediately — no user confirmation needed.
3. Check the repo for a PR template (`.github/pull_request_template.md`) and mirror its headings.
4. Subscribe to PR activity: `mcp__github__subscribe_pr_activity`.

## Merge Policy

- Merge when ALL CI checks are `"success"` — no need to ask the user first.
- The only safe-to-ignore flake: `Analyze (java-kotlin)` (CodeQL) when a concurrent successful run of the same check exists. All other checks must be green.
- Merge method: **squash** (`merge_method="squash"`).
- After merge, move to the next task immediately.

## Autonomy Rule

> "Continue always unless the previous PR is holding you up."

This means: do not pause between tasks for user confirmation. Push → draft PR → subscribe → start next task. Only stop if a blocking CI failure or merge conflict requires human decision.

## GitHub Operations

Use MCP tools ONLY — no `gh` CLI, no `hub`, no direct API calls:
- List PRs: `mcp__github__list_pull_requests`
- Read PR: `mcp__github__pull_request_read`
- Create PR: `mcp__github__create_pull_request`
- Merge PR: `mcp__github__merge_pull_request`
- CI status: `mcp__github__actions_list` + `mcp__github__get_check_run`
- Push files: `mcp__github__push_files`
- Subscribe: `mcp__github__subscribe_pr_activity`

Load tool schemas via `ToolSearch` before first use if not already loaded.

## Branches

- Otaku-Reader: `claude/otaku-reader-audit-c4b7uo`
- Komikku-HV: `claude/otaku-reader-audit-c4b7uo`
- Never push to `main`, `develop`, or any other branch without explicit permission.

## Repositories in Scope

- `heartless-veteran/otaku-reader` — the app under development
- `heartlessveteran2/komikku-hv` — read-only reference spec (Mihon/Komikku fork)

## Developer Context

- Solo developer, veteran background, newer to Kotlin.
- Always explain what was wrong and WHY the fix works — never drop code without context.
- Multi-agent workflow: Claude (architecture + debugging), Copilot, Gemini, Kimi Claw.
