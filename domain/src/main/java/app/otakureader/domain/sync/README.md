# Cloud Sync Implementation

This directory contains the interfaces and prototypes for cross-device cloud synchronization.

## Components

### Interfaces (Domain Layer)

#### `SyncManager`
The main coordinator for all sync operations. Key features:
- Enable/disable sync for specific cloud providers
- Manual and automatic sync triggers
- Conflict resolution with multiple strategies
- Sync status monitoring (idle, syncing, success, error)

#### `SyncProvider`
Abstract cloud storage provider interface. Implementations can support:
- Google Drive (prototype included)
- Dropbox (future)
- WebDAV/Nextcloud (future)
- Custom backend server (future)

### Data Structures

#### `SyncSnapshot`
Lightweight sync data format optimized for cross-device synchronization.

**Key differences from full backup:**
- ✅ Only syncs library state (favorites, categories, read progress)
- ✅ Excludes user preferences (device-specific settings)
- ✅ Excludes reading history (privacy + size optimization)
- ✅ Includes modification timestamps for conflict resolution
- ✅ Includes device ID for tracking sync source
- ✅ Supports incremental sync with version tracking

**Target size:** < 1MB for 1000 manga with 10 chapters each

#### `SyncManga`
Minimal manga representation with:
- Source ID and URL (unique identifier)
- Title and thumbnail
- Favorite status
- Category associations
- User notes
- Chapter read progress (URL + read state only)

#### `SyncChapter`
Lightweight chapter progress with:
- URL (stable identifier)
- Read/bookmark status
- Last page read
- Modification timestamp

#### `SyncCategory`
Category with:
- ID (stable across devices)
- Name and display order
- Modification timestamp

#### `SyncResult`
Detailed sync operation statistics:
- Counts for additions, updates, deletions
- Conflict count
- Success/error status
- Human-readable messages

### Conflict Resolution

Supported strategies:

1. **PREFER_NEWER** (default): Use modification timestamps
2. **PREFER_LOCAL**: Keep local changes
3. **PREFER_REMOTE**: Accept remote changes
4. **MERGE**: Intelligent merge (union of favorites, max progress)

## Google Drive Prototype

Located in `data/src/main/java/app/otakureader/data/sync/`

### `GoogleDriveSyncProvider`
Reference implementation using Google Drive REST API v3.

**Features:**
- Single-file storage in app data folder (hidden from user)
- JSON serialization of snapshots
- CRUD operations on Drive files

**Status:** Prototype - requires OAuth integration

### `GoogleDriveAuthenticator`
OAuth 2.0 authentication handler.

**Required for production:**
- Google Sign-In SDK integration
- `drive.appdata` scope (files hidden from user)
- Automatic token refresh
- Secure token storage (EncryptedSharedPreferences)

## Testing

Unit tests verify:
- ✅ Serialization/deserialization of all sync models
- ✅ Backward compatibility with missing fields
- ✅ Forward compatibility (ignores unknown fields)
- ✅ Default values
- ✅ Statistics calculations

Run tests:
```bash
./gradlew :domain:test --tests "app.otakureader.domain.model.SyncModelsTest"
```

All 11 tests passing ✓

## Implementation Status

### Completed ✅
- [x] `SyncManager` interface
- [x] `SyncProvider` interface
- [x] `SyncSnapshot` data structures
- [x] `GoogleDriveSyncProvider` prototype
- [x] `GoogleDriveAuthenticator` stub
- [x] Unit tests for sync models
- [x] Architecture documentation

### Next Steps (Future PRs)
- [ ] `SyncManagerImpl` implementation
- [ ] Snapshot creation from database
- [ ] Snapshot application to database
- [ ] Conflict resolution logic
- [ ] Google Sign-In SDK integration
- [ ] Token refresh mechanism
- [ ] Sync preferences UI
- [ ] Background sync (WorkManager)
- [ ] Integration tests

## Documentation

See `docs/SYNC_ARCHITECTURE.md` for detailed architecture documentation including:
- Design principles
- Data flow diagrams
- Security considerations
- Size optimization techniques
- Future enhancements

## Usage Example (Future)

```kotlin
// Enable Google Drive sync
syncManager.enableSync("google_drive")

// Manual sync
val result = syncManager.sync()
when (result) {
    is Result.Success -> {
        println("Synced ${result.value.totalChanges} changes")
    }
    is Result.Failure -> {
        println("Sync failed: ${result.error}")
    }
}

// Observe sync status
syncManager.syncStatus.collect { status ->
    when (status) {
        is SyncStatus.Idle -> showIdleState()
        is SyncStatus.Syncing -> showProgress(status.progress)
        is SyncStatus.Success -> showSuccess(status.result)
        is SyncStatus.Error -> showError(status.message)
    }
}
```

## Contributing

When implementing sync features:
1. Follow Clean Architecture (domain → data separation)
2. Use timestamps for all modifications
3. Test serialization thoroughly
4. Handle conflicts gracefully
5. Optimize for small snapshot size
6. Consider privacy (don't sync sensitive data)

## References

- [Google Drive REST API v3](https://developers.google.com/drive/api/v3/reference)
- [OAuth 2.0 for Mobile Apps](https://developers.google.com/identity/protocols/oauth2/native-app)
- Architecture doc: `docs/SYNC_ARCHITECTURE.md`
