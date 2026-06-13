# Tracking

Connect your reading to the services you already use. Otaku Reader supports **five trackers**:

- **MyAnimeList**
- **AniList**
- **Kitsu**
- **MangaUpdates**
- **Shikimori**

## Connecting

Log in once per service under **Settings → Tracking** (or **More → Tracking**). MAL, AniList, and Shikimori use OAuth in your browser — the app never sees your password. Tokens are stored in Android's encrypted Keystore.

## Linking a series

On any manga's Details screen, open tracking and search the service for the matching entry. Once linked you can edit status, score, and progress from inside the app.

## Two-way sync

A background job periodically syncs **both directions**:

- Chapters you read locally push to the tracker.
- Progress you log elsewhere (say, on the AniList website) pulls back into the app.

When both sides changed since the last sync, the per-tracker **conflict resolution** strategy decides: keep local, keep remote, keep the most recent — or ask you. Pending conflicts wait on the Tracking screen; nothing is overwritten silently.

Sync respects **incognito mode** — reading in incognito never updates a tracker.

## Notes

- Sync requires a network connection and backs off automatically when a tracker is down; a permanently failing login stops retrying instead of draining your battery.
- Certificate pinning protects connections to all five tracker APIs.
