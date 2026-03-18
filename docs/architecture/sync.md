# Cloud Sync Architecture

> **Note:** This project has moved from Google Drive sync to **self-hosted sync**. The original cloud sync architecture docs are preserved below for reference, but the current implementation uses a lightweight Docker-based server.

## Current Implementation: Self-Hosted Sync

See [self-hosted-sync.md](./self-hosted-sync.md) for the current architecture.

## Quick Links

- **[Self-Hosted Sync Guide](./self-hosted-sync.md)** - Current implementation
- **[Server Setup](../../server/README.md)** - Docker deployment
- **Issue #462** - Original feature request

---

## Historical: Cloud Sync Architecture (Reference)

The following sections document the original cloud sync design that supported multiple providers (Google Drive, Dropbox, WebDAV). This has been superseded by the simpler self-hosted approach.

### Overview (Historical)

The cloud sync architecture was designed to enable cross-device synchronization of library data (favorites, categories, read progress) while keeping the system flexible and extensible.

### Design Principles (Historical)

1. **Provider Abstraction**: `SyncProvider` interface allowed multiple cloud storage backends
2. **Lightweight Snapshots**: `SyncSnapshot` contains only essential sync data
3. **Conflict Resolution**: Multiple strategies for handling concurrent modifications
4. **Incremental Sync**: Metadata tracking for version-based conflict detection
5. **Device Tracking**: Each snapshot includes device ID for debugging

### Why We Moved to Self-Hosted

| Aspect | Cloud (Google Drive) | Self-Hosted |
|--------|---------------------|-------------|
| **Complexity** | OAuth 2.0, token refresh, API quotas | Simple Bearer token |
| **Privacy** | Data on Google's servers | Your own server |
| **Control** | Subject to API changes/charges | Fully controlled |
| **Setup** | Complex OAuth consent screen | Docker one-liner |
| **Use Case** | Multi-user, enterprise | Personal, single-user |

For a personal manga reader app, the self-hosted approach is simpler, more private, and easier to maintain.

### Removed Components

The following were removed in PR #461:

- `GoogleDriveSyncProvider` - Google Drive integration
- `GoogleDriveAuthenticator` - OAuth flow
- `DropboxSyncProvider` - Dropbox integration stub
- `WebDavSyncProvider` - WebDAV integration stub
- `EncryptedGoogleDriveTokenStore` - OAuth token storage
- Firebase Firestore dependencies

### Preserved Components

The following remain and are used by the self-hosted implementation:

- `SyncManager` - Core sync orchestration
- `SyncProvider` interface - Provider abstraction
- `SyncSnapshot` - Data model for sync payloads
- `SyncManagerImpl` - Business logic
- Conflict resolution strategies
- Unit tests (31 tests passing)

### Architecture Layers (Historical Reference)

#### Domain Layer (`domain/src/main/java/app/otakureader/domain/sync/`)

**`SyncManager`** - Central coordinator:
- Enable/disable sync for a provider
- Trigger manual sync
- Create and apply sync snapshots
- Manage sync status
- Handle conflict resolution

**`SyncProvider`** interface:
- Authenticate with service
- Upload/download snapshots
- Query timestamps
- Manage storage lifecycle

#### Data Layer (`data/src/main/java/app/otakureader/data/sync/`)

**`SyncManagerImpl`**:
- Creates snapshots from local DB
- Applies snapshots with conflict resolution
- Handles merge strategies

### Conflict Resolution (Still Used)

Strategies preserved in current implementation:

1. **PREFER_NEWER** (default): Use modification timestamp
2. **PREFER_LOCAL**: Keep local changes
3. **PREFER_REMOTE**: Accept remote changes
4. **MERGE**: Intelligent merge (union of favorites, max progress)

### Migration Path

If you were using the old cloud sync stubs (they weren't functional), migrate to self-hosted:

1. Deploy the sync server (see [server/README.md](../../server/README.md))
2. Configure server URL and token in app settings
3. Use Backup/Restore buttons to sync

---

*Last updated: March 2026*
