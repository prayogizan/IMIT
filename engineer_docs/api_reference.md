# API & Network Layer Reference

## Base URL

```
https://archive.org/
```

## Endpoints

### 1. Search MIT OCW Collection

```
GET /advancedsearch.php
```

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `q` | String | `"collection:mit_ocw AND mediatype:movies"` | Solr query string |
| `fl[]` | List\<String\> | `["identifier","title","description","creator","year","publicdate","downloads"]` | Fields to return |
| `sort[]` | String | `"publicdate desc"` | Sort order |
| `rows` | Int | `20` | Results per page |
| `page` | Int | `1` | Page number (1-based) |
| `output` | String | `"json"` | Response format |

**Response DTO:**

```kotlin
@Serializable
data class ArchiveSearchResponseDto(
    @SerialName("response")
    val response: ArchiveSearchResponseBodyDto? = null
)

@Serializable
data class ArchiveSearchResponseBodyDto(
    @SerialName("numFound") val numFound: Int = 0,
    @SerialName("start") val start: Int = 0,
    @SerialName("docs") val docs: List<ArchiveSearchDocDto> = emptyList()
)

@Serializable
data class ArchiveSearchDocDto(
    @SerialName("identifier") val identifier: String,
    @SerialName("title") val title: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("creator") val creator: String? = null,
    @SerialName("year") val year: Int? = null,
    @SerialName("downloads") val downloads: Long? = null
)
```

### 2. Get Item Metadata

```
GET /metadata/{identifier}
```

| Parameter | Type | Location | Description |
|-----------|------|----------|-------------|
| `identifier` | String | Path | Archive.org item identifier |

**Response DTO:**

```kotlin
@Serializable
data class ArchiveMetadataResponseDto(
    @SerialName("metadata") val metadata: ArchiveItemMetadataDto? = null,
    @SerialName("files") val files: List<ArchiveFileDto> = emptyList()
)

@Serializable
data class ArchiveFileDto(
    @SerialName("name") val name: String,
    @SerialName("format") val format: String? = null,
    @SerialName("size") val size: String? = null,
    @SerialName("length") val length: String? = null,
    @SerialName("height") val height: String? = null,
    @SerialName("width") val width: String? = null
)
```

## URL Construction

### Thumbnails

```
https://archive.org/download/{identifier}/{identifier}.png
```

### Stream URLs

```
https://archive.org/download/{identifier}/{filename}
```

Example: `https://archive.org/download/mit-ocw-6.0001-lec01/lecture01_720p.mp4`

## DTO to Domain Mapping

### `ArchiveSearchDocDto` → `CourseVideo`

| DTO Field | Domain Field | Default |
|-----------|-------------|---------|
| `identifier` | `identifier` | Required |
| `title` | `title` | `"Untitled Lecture"` |
| `description` | `description` | `""` |
| `creator` | `creator` | `"MIT OpenCourseWare"` |
| `year` | `year` | `0` |
| (constructed) | `thumbnailUrl` | `"{base}/{id}/{id}.png"` |
| `downloads` | `downloadsCount` | `0L` |

### `ArchiveFileDto` → `PlayableStream` (filtered)

Only MP4 files pass the filter:
```kotlin
fun ArchiveFileDto.isMp4Video(): Boolean {
    return name.endsWith(".mp4", ignoreCase = true) &&
            (format?.contains("MPEG4", ignoreCase = true) == true ||
             format?.contains("h.264", ignoreCase = true) == true ||
             format?.contains("mp4", ignoreCase = true) == true)
}
```

| DTO Field | Domain Field | Conversion |
|-----------|-------------|-----------|
| `name` | `fileName` | Direct |
| `format` | `format` | Default `"Unknown"` |
| `size` | `sizeBytes` | `toLongOrNull() ?: 0L` |
| `length` | `durationSeconds` | `toDoubleOrNull() ?: 0.0` |
| `height` | `height` | `toIntOrNull() ?: 0` |
| `width` | `width` | `toIntOrNull() ?: 0` |
| (constructed) | `streamUrl` | `"{base}/{id}/{name}"` |

Streams sorted by `height` descending (best quality first).

## JSON Configuration

```kotlin
Json {
    ignoreUnknownKeys = true   // API may evolve with new fields
    isLenient = true           // Accept non-strict JSON (unquoted keys, etc.)
    coerceInputValues = true   // JSON null → Kotlin default value
    encodeDefaults = true      // Include default-valued fields in output
}
```

## Retrofit Configuration

```kotlin
Retrofit.Builder()
    .baseUrl("https://archive.org/")
    .client(okHttpClient)      // With interceptor chain
    .addConverterFactory(
        json.asConverterFactory("application/json".toMediaType())
    )
    .build()
```

## OkHttp Interceptor Chain

### 1. ConnectivityInterceptor

Checks device network connectivity before making any HTTP request.

```kotlin
class ConnectivityInterceptor(private val context: Context?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!isConnected()) throw NoConnectivityException()
        return chain.proceed(chain.request())
    }
}
```

`NoConnectivityException` extends `IOException` — Retrofit treats it as a network error.

### 2. RateLimitInterceptor

Prevents excessive API calls. Implementation limits request frequency.

### 3. HttpLoggingInterceptor

```kotlin
HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BASIC
}
```

`BASIC` level logs: request method, URL, response code, response time.

## OkHttp Timeouts

| Timeout | Value |
|---------|-------|
| Connect | 30 seconds |
| Read | 30 seconds |
| Write | 30 seconds |

## Error Handling

| Exception Type | Cause | ViewModel Response |
|---------------|-------|-------------------|
| `NoConnectivityException` | Device offline | "No internet connection." |
| `HttpException` | HTTP 4xx/5xx | "Failed to load videos: {message}" |
| `IOException` | Network timeout/error | "Failed to load videos: {message}" |
| `SerializationException` | Malformed JSON | "Failed to load videos: {message}" |
| `CancellationException` | Coroutine cancelled | RETHROWN (never caught) |

## Search Query Construction

For user-initiated search, the query string is constructed:

```kotlin
val searchQuery = "collection:mit_ocw AND mediatype:movies AND ($query)"
```

This ensures results stay within MIT OCW video collection regardless of user input.
