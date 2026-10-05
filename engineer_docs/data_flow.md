# Data Flow & State Management

## State Management Pattern

Every feature uses `StateFlow` with sealed interface states:

```mermaid
stateDiagram-v2
    [*] --> Loading: init block
    Loading --> Success: Data loaded
    Loading --> Error: Load failed (no cache)
    Error --> Loading: Retry event
    Success --> Success: Refresh / LoadMore / Search
    Success --> Loading: Search new query
```

## StateFlow Exposure Pattern

```kotlin
// Private mutable - only ViewModel writes
private val _uiState = MutableStateFlow<CatalogUiState>(CatalogUiState.Loading)

// Public read-only - Screen observes
val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()
```

**Why `asStateFlow()`?** Prevents consumers from casting to `MutableStateFlow` and writing directly.

## Event Dispatch Flow

```mermaid
flowchart LR
    A[User Action] --> B[Screen Composable]
    B -->|"onEvent(UiEvent)"| C[ViewModel]
    C -->|"when(event)"| D[Private Handler]
    D -->|"_uiState.value ="| E[New State]
    E -->|"collectAsState()"| B
```

### Catalog Event Flow

| Event | Handler | State Transition |
|-------|---------|-----------------|
| `Refresh` | `refresh()` | `Success(isRefreshing=true)` → network → `Success(isRefreshing=false)` |
| `LoadMore` | `loadMore()` | `Success(isLoadingMore=true)` → network → `Success(videos += newPage)` |
| `Retry` | `retry()` | Any → `Loading` → network → `Success` or `Error` |
| `SearchQueryChanged(q)` | `onSearchQueryChanged(q)` | Debounce 300ms → `Loading` → search → `Success(searchQuery=q)` |
| `ClearSearch` | `clearSearch()` | Any → `Loading` → reload page 1 |

### Detail Event Flow

| Event | Handler | State Transition |
|-------|---------|-----------------|
| `SelectQuality(stream)` | `selectQuality(stream)` | `Success(selectedStream=stream)` |
| `StreamVideo` | `streamVideo()` | Sets `navigateToPlayer` StateFlow → consumed by `LaunchedEffect` |
| `DownloadVideo` | `initiateDownload()` | Checks runtime `POST_NOTIFICATIONS` permission on Android 13+ (API 33+), then inserts `DownloadedVideoEntity(PENDING)` & enqueues WorkManager via `DownloadManagerHelper` → triggers `observeDownloadProgress(identifier)` → `Success(downloadStatus=PENDING)` or `Success(downloadStatus=FAILED, downloadError=...)` |
| `ToggleDescription` | `toggleDescription()` | `Success(isDescriptionExpanded=!current)` |
| `Retry` | `loadDetail()` | Any → `Loading` → network → checks DB for active download (`DOWNLOADING`/`PENDING`) to trigger `observeDownloadProgress(identifier)` → `Success` or `Error` |
| `DismissDownloadError` | `dismissDownloadError()` | `Success(downloadError=null)` |
| `PauseDownload` | `pauseDownload()` | Calls `DownloadManagerHelper.pauseDownload(identifier)` → `Success(downloadStatus=PAUSED)` |
| `ResumeDownload` | `resumeDownload()` | Calls `DownloadManagerHelper.resumeDownload(...)` with `ExistingWorkPolicy.REPLACE` → triggers `observeDownloadProgress(identifier)` → `Success(downloadStatus=PENDING)` |
| `RetryDownload` | `retryDownload()` | Re-enqueues work via `resumeDownload()` → `Success(downloadStatus=PENDING)` |
| `CancelDownload` | `cancelDownload()` | Cancels observation, deletes `.tmp`/final files via `DownloadManagerHelper.cancelDownload(...)`, deletes Room entity via `deleteById(...)` → `Success(downloadStatus=null, downloadProgress=0, downloadError=null)` |
| `ToggleBackgroundPlayback` | `toggleBackgroundPlayback()` | `Success(isBackgroundPlaybackEnabled = !current)` |
| `OnNavigateAway` | `onNavigateAway()` | If `!isBackgroundPlaybackEnabled` -> pauses player; if enabled -> continues playing in background |

### Detail WorkManager Progress Observation Flow

```mermaid
flowchart LR
    A[DownloadManagerHelper.getWorkInfoFlow] --> B[observeDownloadProgress]
    B --> C{WorkInfo.State}
    C -->|ENQUEUED / BLOCKED| D[PENDING]
    C -->|RUNNING| E["DOWNLOADING (0..100%)"]
    C -->|SUCCEEDED| F["COMPLETED (100%)"]
    C -->|CANCELLED| G["PAUSED"]
    C -->|FAILED| H["FAILED (downloadError)"]
    D --> I[_uiState.value = Success]
    E --> I
    F --> I
    G --> I
    H --> I
```

### Downloads WorkManager Progress Observation & Storage Synchronization Flow

```mermaid
flowchart TD
    subgraph Sources["Reactive Flow Sources"]
        A["downloadedVideoDao.getAllDownloads()<br/>(Room Flow)"]
        B["downloadManager.getAllDownloadsWorkInfoFlow()<br/>(WorkManager TAG_ALL_DOWNLOADS Flow)"]
    end

    subgraph Combine["DownloadsViewModel.observeDownloads()"]
        C["combine(RoomFlow, WorkInfoFlow)"]
        D{"Entities Empty?"}
        E["DownloadsUiState.Empty"]
        F["Build liveProgressMap<br/>(extract identifier + WorkInfo.State / KEY_PROGRESS)"]
        G["Recalculate Storage Metrics<br/>(getTotalDownloadedSize + getAvailableStorageMb)"]
        H["DownloadsUiState.Success<br/>(downloads, totalSize, availableMb, liveProgressMap)"]
    end

    subgraph UI["DownloadsScreen"]
        I["DownloadItemCard<br/>(prefers liveProgress over task.progress)"]
        J["StorageTelemetryCard<br/>(real-time gauge sync)"]
    end

    A --> C
    B --> C
    C --> D
    D -->|Yes| E
    D -->|No| F
    F --> G
    G --> H
    H --> I
    H --> J
```

## Media Playback & Background Service Data Flow

### Media3 Client-Service Playback Sequence

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Screen as DetailScreen
    participant VM as DetailViewModel
    participant VPM as VideoPlayerManager
    participant MC as MediaController
    participant VPS as VideoPlaybackService
    participant Exo as ExoPlayer
    participant MS as MediaSession
    participant Notif as IMITMediaNotificationProvider
    participant SysUI as Android System UI Notification

    User->>Screen: Tap Play / Quality Chip
    Screen->>VM: onEvent(DetailUiEvent.StreamVideo)
    VM->>VPM: playVideo(uri, title, subtitle, artworkUri)
    VPM->>MC: getController() (bind via SessionToken)
    MC->>VPS: onGetSession()
    VPS-->>MC: return MediaSession
    VPM->>MC: setMediaItem(item) & prepare() & play()
    MC->>Exo: setMediaItem() & play()
    Exo->>MS: stateChanged(STATE_READY, playWhenReady=true)
    MS->>Notif: onNotificationRequired()
    Notif->>SysUI: postNotification(id=2001, MediaStyle)
```

### Audio Focus Handling Flow

```mermaid
flowchart TD
    A[Incoming Audio Focus Request / Loss] --> B{Focus Event}
    B -->|AUDIOFOCUS_LOSS| C[ExoPlayer pauses playback]
    B -->|AUDIOFOCUS_LOSS_TRANSIENT| D[ExoPlayer pauses temporarily]
    B -->|AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK| E[ExoPlayer ducks volume to 0.2]
    B -->|AUDIOFOCUS_GAIN| F[ExoPlayer resumes playback / restores volume]
    B -->|ACTION_AUDIO_BECOMING_NOISY| G[Headset disconnected: pause playback immediately]
```

### Background Playback Lifecycle Flow

```mermaid
flowchart TD
    A[User navigates away from DetailScreen] --> B{isBackgroundPlaybackEnabled?}
    B -->|false (Default)| C[DetailViewModel calls videoPlayerManager.pause()]
    C --> D[Playback stops, notification dismissible]
    B -->|true| E[DetailViewModel leaves playback running]
    E --> F[VideoPlaybackService stays alive as foreground service]
    F --> G[Notification displays active playback + chronometer]
    G --> H{Task removed from Recents?}
    H -->|Playing| I[Service continues playing background audio]
    H -->|Paused / Idle| J[Service invokes stopSelf() and releases player]
```


## Repository Data Flow

### Network-First with Cache Fallback (getMitOcwVideos)

```mermaid
flowchart TD
    A[getMitOcwVideos page] --> B{Network Request}
    B -->|Success| C[Map DTOs to Domain]
    C --> D{Page 1?}
    D -->|Yes| E[Delete expired cache >7 days]
    D -->|No| F[Skip cleanup]
    E --> G[Insert into Room cache]
    F --> G
    G --> H[emit Result.success]

    B -->|Failure| I{CancellationException?}
    I -->|Yes| J[RETHROW - never catch]
    I -->|No| K[Read Room cache]
    K --> L{Cache not empty?}
    L -->|Yes| M[Map entities to Domain]
    M --> N[emit Result.success - cached]
    L -->|No| O[emit Result.failure]
```

### Network-Only (getVideoDetail, searchVideos)

```mermaid
flowchart TD
    A[Request] --> B{Network Call}
    B -->|Success| C[Map DTO to Domain]
    C --> D[emit Result.success]
    B -->|CancellationException| E[RETHROW]
    B -->|Other Exception| F[emit Result.failure]
```

## CancellationException Handling

**Critical Rule**: Every `catch (e: Exception)` block must be preceded by `catch (e: CancellationException) { throw e }`.

```kotlin
try {
    // suspend operation
} catch (e: CancellationException) {
    throw e  // MANDATORY - coroutine cancellation must propagate
} catch (e: Exception) {
    // Handle other errors
}
```

This prevents coroutine cancellation from being swallowed, which would cause:
- Memory leaks (coroutine continues running after scope cancelled)
- `viewModelScope` cleanup failures
- Structured concurrency violations

## Pagination Strategy

```kotlin
private val allVideos = mutableListOf<CourseVideo>()
private var currentPage = 1

// On success:
if (isRefresh || page == 1) allVideos.clear()
allVideos.addAll(videos)
currentPage = page

_uiState.value = CatalogUiState.Success(
    videos = allVideos.toList(),  // Defensive copy
    canLoadMore = videos.size >= 20,  // Page size threshold
    currentPage = currentPage
)
```

**Key details:**
- `allVideos` is accumulated in ViewModel (not in UiState)
- `toList()` creates defensive copy for immutable state
- `canLoadMore = videos.size >= 20` — if page returns fewer than 20, no more pages
- `LoadMore` only fires when `canLoadMore && !isLoadingMore` (prevents duplicate loads)

## Search Debounce Implementation

```kotlin
private var searchJob: Job? = null

private fun onSearchQueryChanged(query: String) {
    searchJob?.cancel()                    // Cancel previous search
    searchJob = viewModelScope.launch {
        delay(300)                         // 300ms debounce
        if (query.isBlank()) {
            clearSearch()
            return@launch
        }
        _uiState.value = CatalogUiState.Loading
        allVideos.clear()
        videoRepository.searchVideos(query, page = 1).first().fold(/* ... */)
    }
}
```

- Previous search job cancelled on each keystroke
- 300ms delay before executing API call
- Blank query triggers `clearSearch()` (returns to browsing mode)

## Offline Detection

Two levels of offline awareness:

### 1. Network Layer (ConnectivityInterceptor)
Throws `NoConnectivityException` before HTTP request if device is offline.

### 2. ViewModel Layer (Error Classification)
```kotlin
onFailure = { error ->
    if (allVideos.isNotEmpty()) {
        // Data exists: show it with offline banner
        _uiState.value = CatalogUiState.Success(
            videos = allVideos.toList(),
            isOffline = error is NoConnectivityException,
            canLoadMore = false
        )
    } else {
        // No data: show error screen
        _uiState.value = CatalogUiState.Error(
            message = when (error) {
                is NoConnectivityException -> "No internet connection."
                else -> "Failed to load videos: ${error.localizedMessage}"
            }
        )
    }
}
```

## Navigation One-Shot Events

Detail screen uses a secondary StateFlow for navigation:

```kotlin
// ViewModel
private val _navigateToPlayer = MutableStateFlow<String?>(null)
val navigateToPlayer: StateFlow<String?> = _navigateToPlayer.asStateFlow()

fun onPlayerNavigated() {
    _navigateToPlayer.value = null  // Reset after consumption
}

// Screen
LaunchedEffect(navigateToPlayer) {
    navigateToPlayer?.let { videoUrl ->
        onPlayVideo(videoUrl)
        viewModel.onPlayerNavigated()  // Prevent re-navigation on recomposition
    }
}
```

Pattern: emit URL → `LaunchedEffect` consumes → reset to `null`.
