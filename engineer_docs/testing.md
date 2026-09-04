# Testing Guide

## Test Stack

| Library | Version | Purpose |
|---------|---------|---------|
| JUnit4 | 4.13.2 | Test framework |
| MockK | 1.14.11 | Kotlin mocking |
| Turbine | 1.2.1 | Flow testing |
| coroutines-test | 1.11.0 | Coroutine test utilities |
| Koin Test | 4.2.2 | DI module verification |
| MockWebServer | 5.5.0 | HTTP test server |
| Espresso | 3.7.0 | UI testing |
| Compose UI Test | BOM | Compose testing |

## Test Organization

```
module/
├── src/main/...           # Production code
└── src/test/...           # Unit tests (mirror structure)
    └── java/com/uncaan/imit/...
```

### Test File Inventory

| Module | Test File | Tests |
|--------|-----------|-------|
| `core:model` | `CourseVideoTest.kt` | Data class validation |
| | `DownloadStatusTest.kt` | Enum parsing, `fromString()` |
| | `DownloadTaskTest.kt` | Computed properties |
| | `PlayableStreamTest.kt` | Quality labels, formatting |
| | `VideoDetailTest.kt` | `bestStream`, `sdStream` |
| `core:network` | `VideoRepositoryImplTest.kt` | Repository with cache fallback |
| | `ArchiveApiServiceTest.kt` | Retrofit interface (MockWebServer) |
| | `NetworkMappersTest.kt` | DTO → Domain mapping |
| | `ArchiveDtoSerializationTest.kt` | JSON deserialization |
| | `ConnectivityInterceptorTest.kt` | Offline detection |
| | `ConnectivityObserverTest.kt` | Realtime connectivity Flow emission & callback lifecycle |
| | `RateLimitInterceptorTest.kt` | Rate limiting |
| | `DataModuleTest.kt` | Koin module verification |
| `core:player` | `PipHelperTest.kt` | Picture-in-Picture mode configuration |
| | `PlayerModuleTest.kt` | Koin module verification |
| `core:database` | `DatabaseConvertersTest.kt` | DownloadStatus conversion |
| | `DatabaseMappersTest.kt` | Entity ↔ Domain mapping |
| | `DatabaseModuleTest.kt` | Koin module verification |
| | `DownloadedVideoDaoTest.kt` | Room DAO operations, reactive Flow & status updates |
| `core:designsystem` | `ThemeTokensTest.kt` | Theme token validation |
| `feature:catalog` | `CatalogViewModelTest.kt` | Full ViewModel test suite |
| `feature:details` | `DetailViewModelTest.kt` | Video details & download initiation tests |
| `app` | `CheckModulesTest.kt` | Koin 4 verify() dependency graph validation |
| | `KoinSetupTest.kt` | Full DI module initialization validation |

## ViewModel Test Pattern

### Setup

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val videoRepository: VideoRepository = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
```

**Why `StandardTestDispatcher`?** Gives explicit control over coroutine execution timing. Without it, `init` block coroutines may not complete before assertions.

### Basic Test Structure

```kotlin
@Test
fun `descriptive name with scenario and expected outcome`() = runTest(testDispatcher) {
    // Arrange - setup mocks
    coEvery { repository.getData() } returns flowOf(Result.success(data))

    // Act - create ViewModel (triggers init) and dispatch events
    val viewModel = ViewModel(repository)
    advanceUntilIdle()

    // Assert - verify state via Turbine
    viewModel.uiState.test {
        val state = awaitItem()
        assertTrue(state is UiState.Success)
        assertEquals(expected, (state as UiState.Success).field)
    }
}
```

### Testing Sequence of Events

```kotlin
@Test
fun `refresh replaces data`() = runTest(testDispatcher) {
    // Setup sequential mock responses
    coEvery { repo.getData() } returns
        flowOf(Result.success(initial)) andThen
        flowOf(Result.success(refreshed))

    val viewModel = ViewModel(repo)
    advanceUntilIdle()  // init completes

    viewModel.onEvent(UiEvent.Refresh)
    advanceUntilIdle()  // refresh completes

    viewModel.uiState.test {
        val state = awaitItem() as UiState.Success
        assertEquals(refreshed, state.data)
    }
}
```

### Testing Time-Dependent Logic

```kotlin
@Test
fun `search debounces 300ms`() = runTest(testDispatcher) {
    // Setup
    coEvery { repo.search("final") } returns flowOf(Result.success(results))

    val viewModel = ViewModel(repo)
    advanceUntilIdle()

    // Simulate rapid typing
    viewModel.onEvent(UiEvent.SearchQueryChanged("fi"))
    advanceTimeBy(100)  // < 300ms, previous cancelled
    viewModel.onEvent(UiEvent.SearchQueryChanged("final"))
    advanceTimeBy(350)  // > 300ms, this one executes
    advanceUntilIdle()

    // Only "final" search executed
    viewModel.uiState.test {
        val state = awaitItem() as UiState.Success
        assertEquals("final", state.searchQuery)
    }
}
```

### Testing Error States

```kotlin
@Test
fun `network failure shows offline error`() = runTest(testDispatcher) {
    coEvery { repo.getData() } returns flowOf(
        Result.failure(NoConnectivityException())
    )

    val viewModel = ViewModel(repo)
    advanceUntilIdle()

    viewModel.uiState.test {
        val state = awaitItem()
        assertTrue(state is UiState.Error)
        assertEquals("No internet connection.", (state as UiState.Error).message)
    }
}
```

## Mapper Test Pattern

```kotlin
class NetworkMappersTest {

    @Test
    fun `toCourseVideo maps all fields`() {
        val dto = ArchiveSearchDocDto(
            identifier = "test-id",
            title = "Test Title",
            // ...
        )
        val result = dto.toCourseVideo()
        assertEquals("test-id", result.identifier)
        assertEquals("Test Title", result.title)
    }

    @Test
    fun `toCourseVideo uses defaults for null fields`() {
        val dto = ArchiveSearchDocDto(identifier = "id", title = null, ...)
        val result = dto.toCourseVideo()
        assertEquals("Untitled Lecture", result.title)
    }
}
```

## Koin Module Test Pattern

```kotlin
@OptIn(KoinExperimentalAPI::class)
class CheckModulesTest : KoinTest {

    @Test
    fun `verify all Koin modules dependency graph`() {
        networkModule.verify(extraTypes = listOf(Context::class))
        dataModule.verify(extraTypes = listOf(ArchiveApiService::class, VideoCacheDao::class))
        playerModule.verify(extraTypes = listOf(Context::class))
        catalogViewModelModule.verify(extraTypes = listOf(VideoRepository::class))
        detailsViewModelModule.verify(
            extraTypes = listOf(
                VideoRepository::class,
                DownloadedVideoDao::class,
                String::class
            )
        )
        downloadsViewModelModule.verify()
    }
}
```

## Database Converter Test Pattern

```kotlin
class DatabaseConvertersTest {
    private val converter = DatabaseConverters()

    @Test
    fun `fromDownloadStatus converts enum to string`() {
        assertEquals("COMPLETED", converter.fromDownloadStatus(DownloadStatus.COMPLETED))
    }

    @Test
    fun `toDownloadStatus converts string to enum`() {
        assertEquals(DownloadStatus.FAILED, converter.toDownloadStatus("FAILED"))
    }

    @Test
    fun `null handling`() {
        assertNull(converter.fromDownloadStatus(null))
        assertNull(converter.toDownloadStatus(null))
    }
}
```

## Sample Data Factories

```kotlin
private fun createSampleVideos(count: Int, startId: Int = 1): List<CourseVideo> {
    return (startId until startId + count).map { id ->
        CourseVideo(
            identifier = "video-$id",
            title = "MIT Lecture $id",
            description = "Description $id",
            creator = "MIT OCW",
            year = 2024,
            thumbnailUrl = "https://archive.org/img/$id",
            downloadsCount = 100L * id
        )
    }
}
```

- Unique identifiers for list key stability
- Configurable count and offset for pagination tests
- Realistic field values

## Test Naming Convention

Backtick descriptive format: `` `scenario updates state to expected outcome` ``

Examples:
```kotlin
`initial load success updates state to Success with videos`
`loadMore on error retains existing videos and marks offline`
`searchQueryChanged debounces and executes search after 300ms`
`toCourseVideo uses defaults for null fields`
`databaseModule declares all expected definitions`
```

## Running Tests

```bash
# All unit tests
./gradlew test

# Specific module
./gradlew :feature:catalog:test
./gradlew :core:network:test
./gradlew :core:database:test
./gradlew :core:model:test

# With coverage report
./gradlew testDebugUnitTest

# Compose UI tests (requires device/emulator)
./gradlew connectedAndroidTest
```

## Test Dependencies per Module

```kotlin
// Unit testing (all modules)
testImplementation(libs.junit)
testImplementation(libs.mockk)
testImplementation(libs.coroutines.test)
testImplementation(libs.turbine)

// Koin testing
testImplementation(libs.koin.test)
testImplementation(libs.koin.test.junit4)

// Network testing (core:network only)
testImplementation(libs.mockwebserver)

// Compose UI testing (app module)
androidTestImplementation(platform(libs.compose.bom))
androidTestImplementation(libs.compose.ui.test.junit4)
androidTestImplementation(libs.espresso.core)
androidTestImplementation(libs.junit.ext)
debugImplementation(libs.compose.ui.test.manifest)
debugImplementation(libs.compose.tooling)
```
