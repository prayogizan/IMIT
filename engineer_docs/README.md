# IMIT Engineering Documentation

> **IMIT** (Internet Archive MIT OCW Viewer) — Android app for browsing, streaming, and downloading MIT OpenCourseWare lecture videos from the Internet Archive.

![IMIT Clean Architecture & Layer Boundaries](architecture_diagram.jpg)

## Documentation Index

| Document | Description |
|----------|-------------|
| [Architecture Overview](architecture.md) | System architecture, module graph, UDF pattern, dependency rules |
| [Module Reference](modules.md) | Detailed breakdown of every module with file inventory |
| [Data Flow & State Management](data_flow.md) | UDF lifecycle, StateFlow patterns, event dispatch, offline strategy |
| [Design System](design_system.md) | Theme tokens, color palette, typography, spacing, reusable components |
| [API & Network Layer](api_reference.md) | Archive.org API endpoints, DTOs, interceptors, Retrofit config |
| [Database Schema](database.md) | Room 3.x tables, DAOs, entities, converters, caching strategy |
| [Navigation](navigation.md) | NavGraph, routes, bottom bar, deep linking, parameter passing |
| [Dependency Injection](dependency_injection.md) | Koin modules, wiring, Compose integration, module composition |
| [Testing Guide](testing.md) | Test stack, patterns, conventions, coverage targets |

## Quick Start

```bash
# Clone and build
git clone <repo-url>
cd IMIT

# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run specific module tests
./gradlew :feature:catalog:test
./gradlew :core:network:test
```

## Tech Stack Summary

| Category | Technology | Version |
|----------|-----------|---------|
| Language | Kotlin | 2.2.10 |
| Build System | AGP | 9.3.2 |
| UI Framework | Jetpack Compose (BOM) | 2026.02.01 |
| DI Framework | Koin (BOM) | 4.2.2 |
| Networking | Retrofit + OkHttp | 3.0.0 / 5.5.0 |
| Serialization | kotlinx-serialization | 1.11.0 |
| Database | Room 3.x | 3.0.2 |
| Media Player | Media3 ExoPlayer | 1.11.0 |
| Image Loading | Coil 3.x | 3.4.0 |
| Navigation | Navigation Compose | 2.9.8 |
| Background Work | WorkManager | 2.11.2 |
| Testing | JUnit4, MockK, Turbine | 4.13.2, 1.14.11, 1.2.1 |
| Build Config | Version Catalog (TOML) | — |
| Min SDK | 24 (Android 7.0) | — |
| Target/Compile SDK | 37 | — |

## Project Conventions

- **Package root**: `com.uncaan.imit`
- **Module naming**: `core:{name}` for infrastructure, `feature:{name}` for UI features
- **Source sets**: `src/main/java` for Android modules, `src/main/kotlin` for pure Kotlin modules (`core:model`)
- **Architecture**: Unidirectional Data Flow (UDF) with sealed interface state/event
- **DI**: Constructor injection via Koin (no annotations)
- **State**: `MutableStateFlow` / `StateFlow` with `asStateFlow()`
- **Network errors**: `Flow<Result<T>>` with `CancellationException` rethrow
