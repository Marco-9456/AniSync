# Architecture

This document describes AniSync's system architecture, patterns, and design decisions.

---

## Table of Contents

1. [Overview](#overview)
2. [Layered Architecture](#layered-architecture)
3. [Package Structure](#package-structure)
4. [Data Flow](#data-flow)
5. [State Management](#state-management)
6. [Dependency Injection](#dependency-injection)
7. [Design Patterns](#design-patterns)

---

## Overview

AniSync follows **MVVM + Clean Architecture** with clear separation of concerns:

```mermaid
mindmap
  root((AniSync Architecture))
    Presentation
      Screens
      ViewModels
      Navigation
      UI State
    Domain
      Use Cases
      Models
      Repository Interfaces
    Data
      Repositories
      Local Database
      Remote API
      Mappers
    Infrastructure
      DI Modules
      Workers
      Widgets
```

---

## Layered Architecture

```mermaid
flowchart TB
    subgraph "Presentation Layer"
        UI[Compose UI]
        VM[ViewModels]
        NAV[Navigation]
    end

    subgraph "Domain Layer"
        UC[Use Cases]
        DM[Domain Models]
        RI[Repository Interfaces]
    end

    subgraph "Data Layer"
        REPO[Repository Impl]
        LOCAL[Local Data Source]
        REMOTE[Remote Data Source]
        MAPPER[Data Mappers]
    end

    subgraph "External"
        ROOM[(Room Database)]
        APOLLO[Apollo GraphQL]
        PREFS[SharedPreferences]
    end

    UI --> VM
    VM --> UC
    VM --> NAV
    UC --> RI
    RI -.-> REPO
    REPO --> LOCAL
    REPO --> REMOTE
    REPO --> MAPPER
    LOCAL --> ROOM
    LOCAL --> PREFS
    REMOTE --> APOLLO
```

### Layer Responsibilities

| Layer | Responsibility | Dependencies |
|-------|----------------|--------------|
| **Presentation** | UI rendering, user interaction, navigation | Domain layer only |
| **Domain** | Business logic, use cases, domain models | None (pure Kotlin) |
| **Data** | Data access, caching, network calls | Domain interfaces |

---

## Package Structure

```mermaid
flowchart LR
    subgraph "com.anisync.android"
        DATA[data/]
        DI[di/]
        DOMAIN[domain/]
        PRES[presentation/]
        UI[ui/theme/]
        UTIL[util/]
        WIDGET[widget/]
        WORKER[worker/]
    end

    DATA --> |implements| DOMAIN
    PRES --> |uses| DOMAIN
    DI --> |provides| DATA
    DI --> |provides| PRES
    WIDGET --> |uses| DATA
    WORKER --> |uses| DATA
```

### Package Details

```
com.anisync.android/
├── data/
│   ├── local/              # Room database
│   │   ├── dao/            # Data Access Objects
│   │   ├── entity/         # Room entities
│   │   └── AppDatabase.kt
│   ├── remote/             # API clients (Apollo)
│   ├── repository/         # Repository implementations
│   └── mapper/             # Entity ↔ Domain mappers
├── di/                     # Hilt modules
├── domain/
│   ├── model/              # Domain models
│   ├── repository/         # Repository interfaces
│   └── usecase/            # Business logic
├── presentation/
│   ├── components/         # Shared UI components
│   ├── details/            # Media details and related logic/screens
│   ├── discover/           # Discover/browse and related logic/screens
│   ├── library/            # Library management and related logic/screens
│   ├── login/              # Authentication screen
│   ├── navigation/         # Nav graph & routes
│   ├── profile/            # User profile screen
│   ├── settings/           # Settings screens & components
│   ├── statistics/         # User statistics screen
│   └── util/               # Presentation utilities
├── ui/theme/               # Material 3 theming (MaterialKolor)
├── util/                   # Extensions & helpers
├── widget/                 # Glance widgets
└── worker/                 # WorkManager jobs
```

---

## Data Flow

### Read Operation Flow

```mermaid
sequenceDiagram
    participant UI as Screen
    participant VM as ViewModel
    participant UC as UseCase
    participant Repo as Repository
    participant Local as LocalDataSource
    participant Remote as RemoteDataSource
    participant Cache as Room DB

    UI->>VM: Observe State
    VM->>UC: Execute
    UC->>Repo: getData()
    
    Repo->>Local: getCached()
    Local->>Cache: Query
    Cache-->>Local: Cached Data
    
    alt Cache Hit
        Local-->>Repo: Data
        Repo-->>UC: Result.Success
        UC-->>VM: Data
        VM-->>UI: UI State Update
    else Cache Miss
        Local-->>Repo: null
        Repo->>Remote: fetchFromNetwork()
        Remote-->>Repo: Network Data
        Repo->>Local: cache(data)
        Local->>Cache: Insert
        Repo-->>UC: Result.Success
        UC-->>VM: Data
        VM-->>UI: UI State Update
    end
```

### Write Operation Flow

```mermaid
sequenceDiagram
    participant UI as Screen
    participant VM as ViewModel
    participant UC as UseCase
    participant Repo as Repository
    participant Remote as AniList API
    participant Local as Room DB

    UI->>VM: User Action
    VM->>UC: Execute Mutation
    UC->>Repo: updateData()
    
    Repo->>Remote: GraphQL Mutation
    Remote-->>Repo: Response
    
    alt Success
        Repo->>Local: Update Cache
        Repo-->>UC: Result.Success
        UC-->>VM: Success
        VM-->>UI: Show Success
    else Error
        Repo-->>UC: Result.Error
        UC-->>VM: Error
        VM-->>UI: Show Error
    end
```

---

## State Management

### ViewModel State Pattern

```mermaid
stateDiagram-v2
    [*] --> Initial

    state Initial {
        [*] --> Idle
    }

    state Loading {
        [*] --> Fetching
        Fetching --> Processing
    }

    state Success {
        [*] --> DataLoaded
        DataLoaded --> Refreshing: Pull to Refresh
    }

    state Error {
        [*] --> DisplayError
        DisplayError --> Retrying: Retry
    }

    Initial --> Loading: Load Data
    Loading --> Success: Data Received
    Loading --> Error: Error Occurred
    Success --> Loading: Refresh
    Error --> Loading: Retry
    Refreshing --> Success: Refresh Complete
    Retrying --> Loading
```

### UI State Structure

```kotlin
data class MediaListUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val media: List<Media> = emptyList(),
    val selectedFilter: Filter = Filter.ALL,
    val error: String? = null
)

sealed interface MediaListAction {
    data object Refresh : MediaListAction
    data object Retry : MediaListAction
    data class SelectFilter(val filter: Filter) : MediaListAction
    data class SelectMedia(val media: Media) : MediaListAction
}
```

AniSync enforces a strict **Unidirectional Data Flow (UDF)** pattern:
1. **State (`*UiState`)**: ViewModels expose a single cohesive `StateFlow<UiState>` representing the entire screen state.
2. **Actions (`*Action`)**: User intents and events are sent from the UI to the ViewModel through a single unified `onAction(action: *Action)` function. (Note: We use the `Action` suffix exclusively, avoiding the `Event` suffix for UI-to-ViewModel communication).

---

## Dependency Injection

### Hilt Module Structure

```mermaid
flowchart TB
    subgraph "SingletonComponent"
        AM[ApolloModule]
        DBM[DatabaseModule]
        RM[RepositoryModule]
        ILM[ImageLoaderModule]
    end

    subgraph "Provided Dependencies"
        AC[ApolloClient<br/>+ Normalized Cache<br/>+ AuthorizationInterceptor]
        DB[(AppDatabase)]
        DAOs[5 DAOs<br/>Library · MediaDetails<br/>UserProfile · AiringSchedule · Trending]
        REPOS[8 Repositories<br/>Library · Discover · Profile<br/>Details · Search · Notification<br/>Preferences · Statistics]
        IL[Coil ImageLoader<br/>50 MB disk · 25% heap]
    end

    AM --> AC
    DBM --> DB
    DBM --> DAOs
    RM --> REPOS
    ILM --> IL

    REPOS --> DAOs
    REPOS --> AC
```

### Module Details

| Module | Scope | Provides |
|--------|-------|----------|
| `ApolloModule` | Singleton | `ApolloClient` with two-tier normalized cache (Memory 10 MB + SQLite) and `AuthorizationInterceptor` |
| `DatabaseModule` | Singleton | `AppDatabase` (Room) + 5 DAOs: `LibraryDao`, `MediaDetailsDao`, `UserProfileDao`, `AiringScheduleDao`, `TrendingDao` |
| `RepositoryModule` | Singleton | 8 repository bindings: Library, Discover, Profile, Details, Search, Notification, Preferences, Statistics |
| `ImageLoaderModule` | Singleton | Coil `ImageLoader` with 50 MB disk cache, 25% heap memory cache, 200ms crossfade |

### Module Examples

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "anisync.db")
            .addMigrations(*Migrations.ALL_MIGRATIONS)
            .fallbackToDestructiveMigration(dropAllTables = true) // ⚠️ Remove before production!
            .build()
    }

    @Provides
    fun provideLibraryDao(db: AppDatabase): LibraryDao = db.libraryDao()
    // + MediaDetailsDao, UserProfileDao, AiringScheduleDao, TrendingDao
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository
    @Binds @Singleton
    abstract fun bindDiscoverRepository(impl: DiscoverRepositoryImpl): DiscoverRepository
    // + Profile, Details, Search, Notification, Preferences, Statistics
}
```

---

## Design Patterns

### Repository Pattern

```mermaid
classDiagram
    class MediaRepository {
        <<interface>>
        +getMedia(id: Int): Result~Media~
        +searchMedia(query: String): Result~List~Media~~
        +observeLibrary(): Flow~List~LibraryEntry~~
    }

    class MediaRepositoryImpl {
        -apolloClient: ApolloClient
        -mediaDao: MediaDao
        -mapper: MediaMapper
        +getMedia(id: Int): Result~Media~
        +searchMedia(query: String): Result~List~Media~~
        +observeLibrary(): Flow~List~LibraryEntry~~
    }

    class MediaDao {
        <<interface>>
        +getById(id: Int): MediaEntity?
        +insert(entity: MediaEntity)
        +observeAll(): Flow~List~MediaEntity~~
    }

    class ApolloClient {
        +query(query: Query): ApolloResponse
        +mutation(mutation: Mutation): ApolloResponse
    }

    MediaRepository <|.. MediaRepositoryImpl
    MediaRepositoryImpl --> MediaDao
    MediaRepositoryImpl --> ApolloClient
```

### Use Case Pattern

```kotlin
class GetMediaDetailsUseCase @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val characterRepository: CharacterRepository
) {
    suspend operator fun invoke(mediaId: Int): Result<MediaDetails> {
        return when (val mediaResult = mediaRepository.getMedia(mediaId)) {
            is Result.Success -> {
                val characters = characterRepository.getCharacters(mediaId)
                Result.Success(
                    MediaDetails(
                        media = mediaResult.data,
                        characters = characters.getOrDefault(emptyList())
                    )
                )
            }
            is Result.Error -> mediaResult
        }
    }
}
```

### Mapper Pattern

```kotlin
class MediaMapper @Inject constructor() {
    fun toDomain(entity: MediaEntity): Media {
        return Media(
            id = entity.id,
            title = entity.titleUserPreferred,
            coverUrl = entity.coverUrl,
            // ... other mappings
        )
    }

    fun toEntity(dto: MediaFragment): MediaEntity {
        return MediaEntity(
            id = dto.id,
            titleUserPreferred = dto.title?.userPreferred ?: "",
            coverUrl = dto.coverImage?.large,
            // ... other mappings
        )
    }
}
```

---

## Error Handling

### Result Wrapper

```kotlin
sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : Result<Nothing>
}

// Extension functions available in domain/Result.kt:
// - fold(), map(), getOrNull(), getOrDefault()
// - onSuccess(), onError(), flatMap()
// - isSuccess(), isError()
```

### Error Propagation

```mermaid
flowchart TB
    subgraph "Data Layer"
        NET[Network Error]
        DB[Database Error]
        MAP[Mapping Error]
    end

    subgraph "Domain Layer"
        RES[Result.Error]
    end

    subgraph "Presentation Layer"
        STATE[UI State Error]
        SNACK[Snackbar/Dialog]
    end

    NET --> |catch & wrap| RES
    DB --> |catch & wrap| RES
    MAP --> |catch & wrap| RES
    RES --> STATE
    STATE --> SNACK
```

---

## Related Documentation

- [DATABASE.md](DATABASE.md) - Database schema and migrations
- [API.md](API.md) - GraphQL API integration
- [NAVIGATION.md](NAVIGATION.md) - Navigation architecture
- [WIDGETS.md](WIDGETS.md) - Widget architecture
