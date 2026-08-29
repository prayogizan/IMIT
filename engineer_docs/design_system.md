# Design System

## Theme: `MitOcwTheme`

```kotlin
@Composable
fun MitOcwTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,  // Disabled by default
    content: @Composable () -> Unit
)
```

Provides: `MaterialTheme.colorScheme`, `MaterialTheme.typography`, `MaterialTheme.shapes`, `MaterialTheme.spacing` (custom).

Dynamic color (Material You) supported on Android 12+ but disabled by default.

---

## Color Palette

### MIT Brand Tokens

| Token | Hex | Usage |
|-------|-----|-------|
| `MitRed` | `#A31F34` | Primary (light theme) |
| `MitRedDark` | `#751221` | Primary container (dark theme) |
| `MitRedLight` | `#FFB3B6` | Primary (dark theme) |
| `MitRedContainerLight` | `#FFDAD9` | Primary container (light theme) |
| `MitRedContainerDark` | `#840520` | Primary container (dark theme) |
| `MitSilverGray` | `#8A8B8C` | Neutral accent |
| `MitDarkGray` | `#232528` | Dark surfaces |
| `MitLightGray` | `#F2F4F8` | Light surfaces |

### Light Color Scheme

| Role | Color | Hex |
|------|-------|-----|
| `primary` | MIT Red | `#A31F34` |
| `onPrimary` | White | `#FFFFFF` |
| `background` | Near White | `#FCF8F8` |
| `surface` | Near White | `#FCF8F8` |
| `onSurface` | Dark Brown | `#201A1A` |
| `surfaceVariant` | Light Pink | `#F4DDDD` |
| `secondary` | Muted Brown | `#775656` |
| `tertiary` | Warm Gold | `#755A2F` |
| `error` | Red | `#BA1A1A` |

### Dark Color Scheme

| Role | Color | Hex |
|------|-------|-----|
| `primary` | Light Pink | `#FFB3B6` |
| `onPrimary` | Deep Red | `#680016` |
| `background` | Very Dark | `#181212` |
| `surface` | Very Dark | `#181212` |
| `onSurface` | Light Pink | `#EDE0DF` |
| `surfaceVariant` | Dark Brown | `#524343` |

---

## Typography Scale

Full Material3 Typography with `FontFamily.Default`:

| Style | Weight | Size | Line Height | Letter Spacing |
|-------|--------|------|-------------|---------------|
| `displayLarge` | Normal | 57sp | 64sp | -0.25sp |
| `displayMedium` | Normal | 45sp | 52sp | 0sp |
| `displaySmall` | Normal | 36sp | 44sp | 0sp |
| `headlineLarge` | SemiBold | 32sp | 40sp | 0sp |
| `headlineMedium` | SemiBold | 28sp | 36sp | 0sp |
| `headlineSmall` | SemiBold | 24sp | 32sp | 0sp |
| `titleLarge` | SemiBold | 22sp | 28sp | 0sp |
| `titleMedium` | Medium | 16sp | 24sp | 0.15sp |
| `titleSmall` | Medium | 14sp | 20sp | 0.1sp |
| `bodyLarge` | Normal | 16sp | 24sp | 0.5sp |
| `bodyMedium` | Normal | 14sp | 20sp | 0.25sp |
| `bodySmall` | Normal | 12sp | 16sp | 0.4sp |
| `labelLarge` | Medium | 14sp | 20sp | 0.1sp |
| `labelMedium` | Medium | 12sp | 16sp | 0.5sp |
| `labelSmall` | Medium | 11sp | 16sp | 0.5sp |

### Usage in Composables

```kotlin
Text(text = "Title", style = MaterialTheme.typography.headlineSmall)
Text(text = "Body", style = MaterialTheme.typography.bodyMedium)
Text(text = "Label", style = MaterialTheme.typography.labelSmall)
```

---

## Spacing Tokens

Custom spacing system via `CompositionLocal`:

| Token | Value | Usage |
|-------|-------|-------|
| `none` | 0.dp | Zero spacing |
| `extraSmall` | 4.dp | Tight gaps, chip padding |
| `small` | 8.dp | Grid gaps, badge padding |
| `medium` | 16.dp | Standard content padding |
| `large` | 24.dp | Section spacing |
| `extraLarge` | 32.dp | Page-level padding |
| `huge` | 48.dp | Large separators |

### Access Pattern

```kotlin
val spacing = MaterialTheme.spacing  // Extension property

Modifier.padding(spacing.medium)
Arrangement.spacedBy(spacing.small)
```

### Implementation

```kotlin
data class Spacing(
    val none: Dp = 0.dp,
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val huge: Dp = 48.dp
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }

val MaterialTheme.spacing: Spacing
    @Composable @ReadOnlyComposable
    get() = LocalSpacing.current
```

---

## Reusable Components

### `VideoCard`

Video listing card with thumbnail, title, creator, year badge, download count.

**Parameters:**
| Param | Type | Description |
|-------|------|-------------|
| `video` | `CourseVideo` | Video data to display |
| `onClick` | `() -> Unit` | Click handler |
| `modifier` | `Modifier` | Optional modifier |

**Features:**
- `SubcomposeAsyncImage` with shimmer loading and play icon error state
- Year overlay badge (top-right, semi-transparent black)
- Download count chip (bottom, secondary container)
- Card elevation: 2.dp default, 4.dp pressed

### `VideoCardShimmerItem`

Shimmer placeholder matching `VideoCard` dimensions.

### `LoadingShimmer`

Generic shimmer animation composable.

**Parameters:**
| Param | Type | Description |
|-------|------|-------------|
| `modifier` | `Modifier` | Size and layout |
| `shape` | `Shape` | Shimmer clip shape (default: `MaterialTheme.shapes.medium`) |

### `ErrorMessage`

Error state with message text and retry button.

**Parameters:**
| Param | Type | Description |
|-------|------|-------------|
| `message` | `String` | Error message to display |
| `onRetry` | `() -> Unit` | Retry button callback |
| `modifier` | `Modifier` | Optional modifier |

### `OfflineBanner`

Animated banner indicating offline mode.

**Parameters:**
| Param | Type | Description |
|-------|------|-------------|
| `visible` | `Boolean` | Whether to show the banner |

### `DownloadProgressIndicator`

Shows download progress with status label and file size.

**Parameters:**
| Param | Type | Description |
|-------|------|-------------|
| `progress` | `Int` | Download progress (0-100) |
| `status` | `DownloadStatus` | Current download status |
| `formattedSize` | `String?` | File size display string |
| `modifier` | `Modifier` | Optional modifier |

---

## Preview Conventions

```kotlin
@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ComponentPreview() {
    MitOcwTheme {
        // Component with realistic sample data
    }
}
```

- Always dual preview (light + dark)
- `private` preview functions
- Wrapped in `MitOcwTheme`
- Use realistic sample data
