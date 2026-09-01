# Module Reference

## Module Overview

| Module | Type | Package | Description |
|--------|------|---------|-------------|
| `:app` | Application | `com.uncaan.imit.app` | Entry point, DI wiring, navigation |
| `:core:model` | Kotlin Library | `com.uncaan.imit.core.model` | Domain models (no Android deps) |
| `:core:network` | Android Library | `com.uncaan.imit.core.network` | Retrofit API, DTOs, mappers, repository |
| `:core:database` | Android Library | `com.uncaan.imit.core.database` | Room DB, entities, DAOs, converters |
| `:core:designsystem` | Android Library | `com.uncaan.imit.core.designsystem` | Theme, components, design tokens |
| `:core:player` | Android Library | `com.uncaan.imit.core.player` | Media3 ExoPlayer wrapper |
| `:core:download` | Android Library | `com.uncaan.imit.core.download` | Download management (WIP) |
| `:feature:catalog` | Android Library | `com.uncaan.imit.feature.catalog` | Video catalog listing + search |
| `:feature:details` | Android Library | `com.uncaan.imit.feature.details` | Video detail + stream selection |
| `:feature:downloads` | Android Library | `com.uncaan.imit.feature.downloads` | Downloaded videos management |

---

## `:app` — Application Module

Entry point. Wires all Koin modules, hosts navigation graph and bottom bar.

### File Inventory

| File | Purpose |
|------|---------|
| `IMITApplication.kt` | `Application` subclass. Initializes Koin with all modules. |
| `MainActivity.kt` | Single Activity. Sets `MitOcwTheme` and renders `MitOcwApp`. |
| `navigation/NavRoutes.kt` | Route constants and parameterized route builders. |
| `navigation/MitOcwNavGraph.kt` | `NavHost` with all screen composable destinations. |
| `navigation/BottomNavBar.kt` | `NavigationBar` with `BottomNavItem` sealed class. |
| `ui/MitOcwApp.kt` | Root composable. `Scaffold` + bottom bar + `NavGraph`. |
| `ui/theme/` | App-level theme overrides (Color, Theme, Type). |

### Key Behaviors

- `IMITApplication` registers 8 Koin modules in order: core first, then features
- Bottom bar visible only on `catalog` and `downloads` routes
- Navigation uses `popUpTo(startDestination)` + `saveState`/`restoreState` for tab persistence

---

## `:core:model` — Domain Models

Pure Kotlin module (uses `src/main/kotlin`, no Android dependencies). Contains domain models shared across all modules.

### File Inventory

| File | Type | Description |
|------|------|-------------|
| `CourseVideo.kt` | `data class` | Video listing model with identifier, title, creator, year, thumbnail, downloads count |
| `VideoDetail.kt` | `data class` | Full video metadata with list of `PlayableStream`. Computed properties: `bestStream` (max height), `sdStream` (min height) |
| `PlayableStream.kt` | `data class` | Single playable video stream with URL, dimensions, size, duration. Computed: `qualityLabel` ("HD 720p"), `formattedSize` ("250.0 MB"), `formattedDuration` ("50:00") |
| `DownloadStatus.kt` | `enum class` | `PENDING`, `DOWNLOADING`, `COMPLETED`, `FAILED`, `PAUSED`. Has `fromString()` companion factory. |
| `DownloadTask.kt` | `data class` | Download task metadata. Computed: `isPlayable` (completed + has local path), `formattedSize` |

### Design Decisions

- All models are `@Serializable` for kotlinx-serialization compatibility
- Computed properties use `val` with `get()` (not stored in constructor)
- `PlayableStream` handles format display logic (quality labels, size formatting)
- `DownloadStatus.fromString()` defaults to `PENDING` on unknown values (safe parsing)

---

## `:core:network` — Network Layer

Retrofit API client, DTOs, mappers, repository interface + implementation.

### Package Structure

```
com.uncaan.imit.core
├── data.repository/
│   ├── VideoRepository.kt          # Interface
│   └── VideoRepositoryImpl.kt      # Implementation with offline fallback
└── network/
    ├── api/
    │   └── ArchiveApiService.kt    # Retrofit interface
    ├── di/
    │   ├── NetworkModule.kt        # OkHttp, Retrofit, Json Koin module
    │   └── DataModule.kt           # Repository binding Koin module
    ├── exception/
    │   └── NoConnectivityException.kt
    ├── interceptor/
    │   ├── ConnectivityInterceptor.kt  # Throws if offline
    │   └── RateLimitInterceptor.kt     # API rate limiting
    ├── mapper/
    │   └── NetworkMappers.kt       # DTO → Domain extension functions
    └── model/
        ├── ArchiveSearchResponseDto.kt
        ├── ArchiveSearchDocDto.kt
        ├── ArchiveMetadataResponseDto.kt
        └── ArchiveFileDto.kt
```

### API Endpoints

| Method | Path | Parameters | Returns |
|--------|------|-----------|---------|
| `GET` | `advancedsearch.php` | `q`, `fl[]`, `sort[]`, `rows`, `page`, `output` | `ArchiveSearchResponseDto` |
| `GET` | `metadata/{identifier}` | `identifier` (path) | `ArchiveMetadataResponseDto` |

### Repository Methods

| Method | Signature | Behavior |
|--------|-----------|----------|
| `getMitOcwVideos` | `(page: Int): Flow<Result<List<CourseVideo>>>` | Network + cache. On page 1: cleanup expired (7 days). On fail: fallback to cache. |
| `getVideoDetail` | `(identifier: String): Flow<Result<VideoDetail>>` | Network only. No cache fallback. |
| `searchVideos` | `(query: String, page: Int): Flow<Result<List<CourseVideo>>>` | Network only. Prepends collection filter to query. |

### OkHttp Interceptor Chain (order matters)

1. **ConnectivityInterceptor** — Checks network connectivity, throws `NoConnectivityException` if offline
2. **RateLimitInterceptor** — Prevents API abuse
3. **HttpLoggingInterceptor** — Logs at `Level.BASIC`

---

## `:core:database` — Persistence Layer

Room 3.x database with two tables for video caching and download tracking.

### Package Structure

```
com.uncaan.imit.core.database
├── MitOcwDatabase.kt           # @Database class (version 1)
├── converter/
│   └── DatabaseConverters.kt   # DownloadStatus ↔ String
├── dao/
│   ├── VideoCacheDao.kt        # Cache CRUD + search + expiry
│   └── DownloadedVideoDao.kt   # Download tracking CRUD + status queries
├── di/
│   └── DatabaseModule.kt       # Koin module
├── entity/
│   ├── VideoCacheEntity.kt     # Cached video listing
│   └── DownloadedVideoEntity.kt # Download record
└── mapper/
    └── DatabaseMappers.kt      # Entity ↔ Domain model mappers
```

### Tables

| Table | Primary Key | Purpose |
|-------|------------|---------|
| `video_cache` | `identifier` | Offline cache for video listings. Auto-expires after 7 days. |
| `downloaded_videos` | `identifier` | Tracks download state, progress, local file paths. |

### Room 3.x Specifics

- Uses `androidx.room3.*` package (not `androidx.room`)
- `@ColumnTypeConverters` (not `@TypeConverters`) for converter registration
- `@ColumnTypeConverter` (not `@TypeConverter`) on converter methods
- KSP processor: `androidx.room3:room3-compiler`

---

## `:core:designsystem` — UI Design System

Centralized theme tokens and reusable Compose components.

### Package Structure

```
com.uncaan.imit.core.designsystem
├── DesignSystem.kt                    # Module marker
├── component/
│   ├── VideoCard.kt                   # Video listing card with thumbnail
│   ├── VideoCardShimmerItem.kt        # Shimmer placeholder for VideoCard
│   ├── LoadingShimmer.kt              # Generic shimmer animation
│   ├── ErrorMessage.kt               # Error state with retry button
│   ├── OfflineBanner.kt              # Offline indicator banner
│   └── DownloadProgressIndicator.kt  # Download progress display
└── theme/
    ├── Color.kt                       # MIT brand color palette (light + dark)
    ├── Type.kt                        # Full Material3 Typography scale
    ├── Shape.kt                       # Material3 shape definitions
    ├── Spacing.kt                     # Custom spacing tokens via CompositionLocal
    └── Theme.kt                       # MitOcwTheme composable
```

---

## `:core:player` — Media Player

ExoPlayer wrapper for video playback.

### File Inventory

| File | Purpose |
|------|---------|
| `VideoPlayerManager.kt` | Singleton player lifecycle manager. Lazy ExoPlayer creation, auto-retry on network errors (exponential backoff, max 3 retries). |
| `PipHelper.kt` | Picture-in-Picture (PiP) helper for Android O+ with 16:9 aspect ratio and Activity extension. |
| `VideoPlayerScreen.kt` | Compose screen wrapping `PlayerView` via `AndroidView`. Handles `LaunchedEffect` for URL changes, `DisposableEffect` for player cleanup, and PiP / top bar controls. |
| `di/PlayerModule.kt` | Koin module providing `VideoPlayerManager` singleton. |

### Player Features

- Lazy initialization: player created on first `getPlayer()` call
- Exponential backoff retry: 1s, 2s, 4s on `ERROR_CODE_IO_NETWORK_CONNECTION_FAILED`
- Retry counter resets on `STATE_READY`
- Player released on screen disposal via `DisposableEffect`
- Picture-in-Picture (PiP) mode support with 16:9 aspect ratio on Android 8.0+ (API 26+)

---

## `:core:download` — Download Management

WorkManager background download engine with Koin worker injection, foreground notification progress tracking, and storage guard (<500MB via `StatFs`).

### File Inventory

| File | Purpose |
|------|---------|
| `VideoDownloadWorker.kt` | `CoroutineWorker` injected via Koin. Manages OkHttp stream with 8KB buffer, StatFs storage guard (<500MB), low-importance foreground notification with progress bar, cooperative stop cleanup, and `CancellationException` rethrow rule. |
| `DownloadManagerHelper.kt` | Helper service managing `WorkManager` enqueue (`ExistingWorkPolicy.KEEP`), unique work tagging, cancellation, disk storage checks, and reactive `WorkInfo` Flow observation. |
| `di/DownloadModule.kt` | Koin module providing `DownloadManagerHelper` singleton and `VideoDownloadWorker` via Koin's `worker { }` DSL. |

### Key Behaviors

- **Koin Worker Injection:** `VideoDownloadWorker` constructor dependencies (`Context`, `WorkerParameters`, `OkHttpClient`, `DownloadedVideoDao`) are injected via Koin's `worker { }` DSL and `workManagerFactory()`.
- **Storage Space Guard:** Downloads abort and fail early if the device has less than 500MB free disk space.
- **Foreground Notification:** Ongoing low-importance notification displays real-time percentage progress. Uses `FOREGROUND_SERVICE_TYPE_DATA_SYNC` on Android 10+ (API 29+).
- **Coroutines & Cooperative Cancellation:** Listens to `isStopped` during stream reading; deletes partial files and updates status to `PAUSED`. Propagates `CancellationException` without swallowing.

---

## `:feature:catalog` — Video Catalog

Main screen showing paginated MIT OCW video grid with search.

### File Inventory

| File | Responsibility |
|------|---------------|
| `CatalogUiState.kt` | `Loading`, `Success` (videos, pagination, offline, search), `Error` |
| `CatalogUiEvent.kt` | `Refresh`, `LoadMore`, `Retry`, `SearchQueryChanged`, `ClearSearch` |
| `CatalogViewModel.kt` | Pagination, 300ms debounced search, offline-aware error handling |
| `CatalogScreen.kt` | 2-column `LazyVerticalGrid`, `PullToRefreshBox`, shimmer loading, infinite scroll |
| `di/CatalogModule.kt` | `viewModelOf(::CatalogViewModel)` |

### Key Behaviors

- Infinite scroll triggers when 4 items from end
- Search debounced 300ms to prevent excessive API calls
- `canLoadMore` set `false` when page returns fewer than 20 items
- Offline: shows cached data with `OfflineBanner` if network fails

---

## `:feature:details` — Video Detail

Detail screen with video metadata, quality selection, streaming, and download initiation.

### File Inventory

| File | Responsibility |
|------|---------------|
| `DetailUiState.kt` | `Loading`, `Success` (detail, selectedStream, downloadStatus, progress, description expanded), `Error` |
| `DetailUiEvent.kt` | `SelectQuality`, `StreamVideo`, `DownloadVideo`, `ToggleDescription`, `Retry` |
| `DetailViewModel.kt` | Loads video detail, manages quality selection, initiates downloads, navigates to player |
| `DetailScreen.kt` | Thumbnail with scrim overlay, play button, quality chips (`FlowRow` + `FilterChip`), download progress, expandable description |
| `di/DetailsModule.kt` | ViewModel module with `parametersOf(identifier)` for assisted injection |
| `di/DetailsViewModelModule.kt` | Additional ViewModel bindings |

### Key Behaviors

- ViewModel receives `identifier` as constructor parameter (Koin `parametersOf`)
- Player navigation via `navigateToPlayer: StateFlow<String?>` + `onPlayerNavigated()` reset
- Quality selection: `FilterChip` with `FlowRow`, auto-selects `bestStream` (highest resolution)
- Download: inserts `DownloadedVideoEntity` with `PENDING` status
- Plays local file if download `COMPLETED`, otherwise streams remote URL
- Description: expandable with `animateContentSize()`, 4-line clamp

---

## `:feature:downloads` — Downloads Manager

**Status: Minimal Implementation**

Currently shows a placeholder "Downloads" text. Full implementation pending `core:download` completion.

```kotlin
@Composable
fun DownloadsScreen(
    onPlayVideo: (videoUrl: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Downloads", style = MaterialTheme.typography.titleLarge)
    }
}
```
