# Backups & Sync

Your library is yours — back it up like it.

## What's in a backup

A backup includes essentially everything you've done in the app:

- Library entries, categories (including per-category update schedules and locks), and reading progress
- Chapter read states, bookmarks, and your **chapter notes**
- **All your customizations** — edited titles/authors/descriptions/genres, per-manga reader settings (direction, mode, color filters, preload), completed/dropped flags
- Tracking links, reading history, and settings

The one deliberate exception: custom cover *image files* are device-local (the paths wouldn't exist on a new phone), so re-pick covers after restoring to a different device.

## Local backup & restore

**Settings → Backup** creates a backup file wherever you point it (SAF — so SD cards and USB drives work). Restore from the same screen. Backups are versioned; newer app versions restore older backups cleanly.

## Scheduled cloud backup (WebDAV)

Point the app at any WebDAV server — Nextcloud, ownCloud, a NAS — and it uploads backups on a schedule. Credentials are stored encrypted. This is the recommended way to survive a lost phone.

## Coming from Tachiyomi?

The app imports **Tachiyomi/Mihon backup files** directly — library, categories, progress, and tracking links carry over. Combined with the shared extension ecosystem, switching takes minutes.

## Reading-position sync

Mid-chapter positions queue locally and ride along with backups, so picking up on another device puts you close to where you left off.

## An honest note on privacy

Backups contain your full library and reading history. They're your data and they only go where you send them — but treat a backup file with the same care as the library itself.
