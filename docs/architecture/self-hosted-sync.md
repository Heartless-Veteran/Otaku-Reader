# Self-Hosted Sync Server Architecture

## Overview

The self-hosted sync solution consists of two components:

1. **Sync Server** (`/server`) - Lightweight Ktor-based HTTP server
2. **Android Provider** (`SelfHostedSyncProvider`) - Client implementation of `SyncProvider`

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Android App                              │
│  ┌─────────────────┐        ┌──────────────────────────────┐   │
│  │  Settings UI    │◄──────►│  SelfHostedSyncProvider      │   │
│  │  (URL + Token)  │        │  - Upload / Download         │   │
│  └─────────────────┘        │  - Bearer Auth               │   │
│                             └──────────────┬───────────────┘   │
│                                            │                    │
│                             ┌──────────────▼───────────────┐   │
│                             │  SelfHostedSyncApiFactory    │   │
│                             │  - Dynamic URL from prefs    │   │
│                             └──────────────┬───────────────┘   │
└────────────────────────────────────────────┼───────────────────┘
                                             │ HTTP + Bearer
┌────────────────────────────────────────────┼───────────────────┐
│                         Sync Server        │                   │
│  ┌─────────────────────────────────────────▼───────────────┐  │
│  │  Ktor Application                                        │  │
│  │  ├── /health          (no auth)                         │  │
│  │  ├── /sync/upload     (Bearer auth)                     │  │
│  │  ├── /sync/download   (Bearer auth)                     │  │
│  │  ├── /sync/timestamp  (Bearer auth)                     │  │
│  │  └── /sync            (DELETE, Bearer auth)             │  │
│  └────────────────────────┬────────────────────────────────┘  │
│                           │                                    │
│  ┌────────────────────────▼────────────────────────────────┐  │
│  │  SyncService                                            │  │
│  │  - Filesystem storage in /app/data                      │  │
│  │  - Single snapshot (overwrites on upload)               │  │
│  └─────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## API Endpoints

### Health Check
```
GET /health
Response: "OK"
```

### Upload Snapshot
```
POST /sync/upload
Authorization: Bearer <token>
Content-Type: application/json

{
  "data": "<base64-encoded-json>",
  "timestamp": 1712345678901
}

Response:
{
  "success": true,
  "timestamp": 1712345678901,
  "size": 12345
}
```

### Download Snapshot
```
GET /sync/download
Authorization: Bearer <token>

Response:
{
  "data": "<base64-encoded-json>",
  "timestamp": 1712345678901,
  "exists": true
}
```

### Get Timestamp
```
GET /sync/timestamp
Authorization: Bearer <token>

Response:
{
  "timestamp": 1712345678901,
  "exists": true
}
```

### Delete Snapshot
```
DELETE /sync
Authorization: Bearer <token>

Response:
{
  "success": true
}
```

## Data Flow

### Backup (Upload)
1. User taps "Backup to Server" in Settings
2. `SyncManager.createSnapshot()` builds `SyncSnapshot` from local DB
3. `SelfHostedSyncProvider.uploadSnapshot()`:
   - Serializes snapshot to JSON
   - Base64 encodes (handles binary, prevents encoding issues)
   - POSTs to `/sync/upload` with Bearer token
4. Server stores to filesystem
5. Response returns success + timestamp

### Restore (Download)
1. User taps "Restore from Server" (with confirmation dialog)
2. `SelfHostedSyncProvider.downloadSnapshot()`:
   - GETs from `/sync/download` with Bearer token
   - Decodes Base64 response
   - Deserializes to `SyncSnapshot`
3. `SyncManager.applySnapshot()` merges with local data
4. Conflict resolution strategy applied per entity
5. Local DB updated

## Security

- **Authentication**: Bearer token in `Authorization` header
- **Storage**: Single file on disk (`sync_snapshot.json`)
- **HTTPS**: Use reverse proxy (nginx/traefik) for production
- **Token Generation**: User-provided via env var or settings

## Configuration

### Server (Environment Variables)
| Variable | Default | Description |
|----------|---------|-------------|
| `AUTH_TOKEN` | (required) | Bearer token for API access |
| `HOST` | `0.0.0.0` | Bind address |
| `PORT` | `8080` | Server port |
| `STORAGE_PATH` | `/app/data` | Snapshot storage directory |

### Android (SyncPreferences)
| Preference | Type | Description |
|------------|------|-------------|
| `selfHostedServerUrl` | String | Server URL (e.g., `http://192.168.1.100:8080`) |
| `selfHostedAuthToken` | String | Bearer token |

## Files

### Server
- `server/build.gradle.kts` - Gradle build with Ktor
- `server/Dockerfile` - Multi-stage Docker build
- `server/docker-compose.yml` - Docker Compose setup
- `server/src/main/kotlin/.../Application.kt` - Main entry point
- `server/src/main/kotlin/.../routes/SyncRoutes.kt` - API routes
- `server/src/main/kotlin/.../service/SyncService.kt` - Business logic
- `server/src/main/kotlin/.../config/AppConfig.kt` - Configuration

### Android
- `data/src/.../sync/SelfHostedSyncProvider.kt` - Provider implementation
- `data/src/.../sync/remote/SelfHostedSyncApi.kt` - Retrofit interface
- `data/src/.../sync/remote/SelfHostedSyncApiFactory.kt` - Dynamic URL factory
- `data/src/.../sync/di/SyncModule.kt` - DI module
- `core/preferences/.../SyncPreferences.kt` - Settings storage

## Manual Sync Mode

Per issue #462, this implements **Option A: Manual Backup/Restore**:

- No automatic background sync
- No real-time synchronization
- No conflict resolution UI
- User explicitly triggers backup/restore
- Last upload wins (server is source of truth on restore)

This keeps the implementation simple and predictable for single-user personal use.
