# Extensions & Repositories

Otaku Reader is fully compatible with the **Tachiyomi extension format**. Extensions are APKs maintained by the community; the app loads them in an isolated classloader and never modifies them, which is why the whole existing ecosystem (500+ sources) just works.

## Repositories

A repository is a URL serving an extension index (`index.min.json`). You can add as many as you like under **Browse → Extensions → Repositories**.

- Repos used in Komikku/Mihon work unchanged.
- Each repo is fetched independently — one broken or unreachable repo never blocks the others, and the failure message names the repo at fault.
- Removing a repo removes its available extensions from the list; already-installed extensions keep working.

## Trust & provenance

Extensions execute code, so the app treats signers carefully:

- **Trust prompts** — the first install from an unknown signing certificate shows its SHA-256 hash and asks for confirmation. Trusted signers are remembered (in encrypted storage) and can be revoked.
- **Provenance tracking** — the app records which repository each extension came from. If an update is offered from a *different* repo than the one it was installed from, you get a warning before anything changes.
- **Signer-change detection** — if an installed extension's signing certificate changes, it's flagged in the list with a warning.
- **Blocklist** — known-bad extensions are filtered out automatically via a daily-refreshed blocklist.

## Updates

Extension updates are checked in the background (WorkManager) and surfaced in the extensions list. Updating preserves your trust decisions and source settings.

## NSFW content

Sources flagged 18+ are hidden until you enable NSFW content in settings. The toggle also gates features that depend on adult sources, such as E-Hentai favorites sync.

## When a source misbehaves

- **Source health diagnostics** — Browse tracks per-source failures and shows a warning badge with a diagnostic sheet explaining what went wrong.
- **WebView fallback** — sources behind Cloudflare can open a WebView challenge; the solved session cookies are shared back to the app's network stack automatically.
