# Contributing to AniSync

Thank you for your interest in contributing to AniSync! This document provides guidelines and best practices for contributing to the project.

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [Translations](#translations)
3. [Development Setup](#development-setup)
4. [Code Style Guide](#code-style-guide)
5. [Architecture Guidelines](#architecture-guidelines)
6. [Git Workflow](#git-workflow)
7. [Pull Request Process](#pull-request-process)
8. [Testing Guidelines](#testing-guidelines)
9. [Documentation](#documentation)

---

## Getting Started

### Prerequisites

Before contributing, ensure you have:

- **Android Studio** Ladybug (2024.2.1) or newer
- **JDK 17** (Android Studio bundles this)
- **Git** for version control
- An **AniList account** for testing authenticated features

### First-Time Setup

```bash
# 1. Fork the repository on GitHub

# 2. Clone your fork
git clone https://github.com/YOUR_FORK/AniSync.git
cd AniSync

# 3. Add upstream remote
git remote add upstream https://github.com/Marco-9456/AniSync.git

# 4. Create a feature branch
git checkout -b feature/your-feature-name
```

---

## Translations

AniSync uses [Weblate](https://hosted.weblate.org/engage/anisync/) for community-driven translations. You don't need any programming knowledge to contribute.

### Contributing via Weblate

1. Go to [hosted.weblate.org/projects/anisync/app](https://hosted.weblate.org/projects/anisync/app/)
2. Select the language you want to translate, or request a new one
3. Click on any untranslated string and enter your translation
4. Save — your contribution will be reviewed and merged automatically via Pull Request

### Translation Guidelines

- **Placeholders**: Keep placeholders like `%s`, `%d`, `%1$s`, `%2$s` exactly as they appear. They are replaced by the app dynamically.
- **Special characters**: Keep `&amp;` and `\'` as they are unless your language uses different conventions.
- **Accessibility strings**: Strings starting with `a11y_`, `cd_`, or `content_description` are read by screen readers. Keep them concise and descriptive.
- **App name**: `AniSync` should remain unchanged across all languages.
- **Consistency**: If "Library" is translated as "Bibliothek" in German, use "Bibliothek" everywhere, not "Sammlung" in some places.

### Adding a New Language

If your language is not listed on Weblate:
1. Open a [GitHub Discussion](https://github.com/Marco-9456/AniSync/discussions) requesting the language
2. A maintainer will add it to Weblate
3. You can then start translating immediately

---

## Development Setup

### Project Structure

```
AniSync/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/anisync/android/
│   │   │   │   ├── data/          # Data layer
│   │   │   │   ├── di/            # Dependency injection
│   │   │   │   ├── domain/        # Domain layer
│   │   │   │   ├── presentation/  # UI layer
│   │   │   │   │   ├── components/    # Shared UI components
│   │   │   │   │   ├── details/       # Media details screen
│   │   │   │   │   ├── discover/      # Discover/browse screen
│   │   │   │   │   ├── library/       # Library management screen
│   │   │   │   │   ├── login/         # Authentication screen
│   │   │   │   │   ├── navigation/    # Nav graph & routes
│   │   │   │   │   ├── profile/       # User profile screen
│   │   │   │   │   ├── settings/      # Settings screens & components
│   │   │   │   │   ├── statistics/    # User statistics screen
│   │   │   │   │   └── util/          # Presentation utilities
│   │   │   │   ├── ui/theme/      # Design system (MaterialKolor)
│   │   │   │   ├── widget/        # Glance widgets
│   │   │   │   └── worker/        # Background workers
│   │   │   ├── graphql/           # GraphQL operations
│   │   │   └── res/               # Resources
│   │   ├── androidTest/           # Instrumented tests
│   │   └── test/                  # Unit tests
│   └── schemas/                   # Room schema exports
├── docs/                          # Documentation
└── gradle/                        # Gradle configuration
```

### Build Variants

| Variant | Package ID | Use Case |
|---------|------------|----------|
| `debug` | `com.anisync.android.debug` | Development and testing |
| `release` | `com.anisync.android` | Production builds |

Both variants can be installed simultaneously for testing.

### Running the App

1. Open the project in Android Studio
2. Wait for Gradle sync to complete
3. Select a device/emulator (API 26+)
4. Click **Run** or press `Shift + F10`

---

## Code Style Guide

### Kotlin Conventions

We follow the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) with additional project-specific guidelines.

#### Naming

```kotlin
// Classes: PascalCase
class MediaDetailsViewModel

// Functions: camelCase
fun loadMediaDetails()

// Properties: camelCase
val isLoading: Boolean

// Constants: SCREAMING_SNAKE_CASE
const val MAX_RETRY_COUNT = 3

// Backing properties: underscore prefix
private val _uiState = MutableStateFlow(UiState())
val uiState: StateFlow<UiState> = _uiState.asStateFlow()
```

#### File Organization

```kotlin
// Order of declarations in a class:
// 1. Companion object
// 2. Properties (public, then private)
// 3. Init blocks
// 4. Constructors
// 5. Public functions
// 6. Internal functions
// 7. Private functions
// 8. Nested classes

class MyViewModel @Inject constructor(
    private val repository: MyRepository
) : ViewModel() {

    companion object {
        private const val TAG = "MyViewModel"
    }

    // Public state
    val uiState: StateFlow<UiState> = ...

    // Private state
    private val _actions = Channel<Action>()

    init {
        loadData()
    }

    // Public functions
    fun onAction(action: Action) { ... }

    // Private functions
    private fun loadData() { ... }
}
```

#### Compose Guidelines

```kotlin
// Composable naming: PascalCase, noun or noun phrase
@Composable
fun MediaCard(media: Media, onClick: () -> Unit) { ... }

// Composable modifiers: Always last parameter with default
@Composable
fun MediaCard(
    media: Media,
    onClick: () -> Unit,
    modifier: Modifier = Modifier   // Last, with default
) { ... }

// State hoisting: Prefer stateless composables
@Composable
fun MediaList(
    media: List<Media>,         // State passed down
    onItemClick: (Media) -> Unit // Actions passed up
) { ... }

// Preview annotations
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MediaCardPreview() { ... }
```

### GraphQL Conventions

```graphql
# Query naming: PascalCase with descriptive name
query GetMediaDetails($id: Int!) {
    Media(id: $id) {
        id
        title {
            userPreferred
            english
            native
        }
        # ... fields
    }
}

# Fragment usage for reusable field sets
fragment MediaBasicInfo on Media {
    id
    title {
        userPreferred
    }
    coverImage {
        large
    }
}
```

### Resource Naming

```
# Drawables: type_description
ic_notification.xml
bg_card_gradient.xml
img_placeholder.png

# Strings: screen_description
<string name="library_empty_watching">No anime in your watching list</string>
<string name="details_add_to_list">Add to List</string>

# Colors: purpose_variant
<color name="primary_light">...</color>
<color name="surface_container">...</color>
```

---

## Architecture Guidelines

### Layer Responsibilities

```
┌─────────────────────────────────────────────┐
│             Presentation Layer              │
│  • Screens (Composables)                    │
│  • ViewModels (UI State + Actions)          │
│  • Navigation                               │
├─────────────────────────────────────────────┤
│               Domain Layer                  │
│  • Use Cases (Business Logic)               │
│  • Domain Models                            │
│  • Repository Interfaces                    │
├─────────────────────────────────────────────┤
│                Data Layer                   │
│  • Repository Implementations               │
│  • Data Sources (Local + Remote)            │
│  • Data Mappers                             │
└─────────────────────────────────────────────┘
```

### ViewModel Pattern

```kotlin
@HiltViewModel
class MediaListViewModel @Inject constructor(
    private val getMediaListUseCase: GetMediaListUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaListUiState())
    val uiState: StateFlow<MediaListUiState> = _uiState.asStateFlow()

    init {
        loadMedia()
    }

    fun onAction(action: MediaListAction) {
        when (action) {
            is MediaListAction.Refresh -> loadMedia()
            is MediaListAction.ChangeFilter -> updateFilter(action.filter)
        }
    }

    private fun loadMedia() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            when (val result = getMediaListUseCase()) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            media = result.data
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }
}

// UI State: Immutable data class
data class MediaListUiState(
    val isLoading: Boolean = false,
    val media: List<Media> = emptyList(),
    val error: String? = null
)

// Actions: Sealed interface for type safety
sealed interface MediaListAction {
    data object Refresh : MediaListAction
    data class ChangeFilter(val filter: Filter) : MediaListAction
}
```

### Repository Pattern

```kotlin
// Domain layer: Interface
interface MediaRepository {
    suspend fun getMedia(id: Int): Result<Media>
    suspend fun updateProgress(id: Int, progress: Int): Result<Unit>
    fun observeLibrary(): Flow<List<LibraryEntry>>
}

// Data layer: Implementation
class MediaRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient,
    private val mediaDao: MediaDao,
    private val mapper: MediaMapper
) : MediaRepository {

    override suspend fun getMedia(id: Int): Result<Media> {
        // Try cache first
        mediaDao.getById(id)?.let { cached ->
            return Result.Success(mapper.toDomain(cached))
        }

        // Fetch from network
        return try {
            val response = apolloClient.query(GetMediaQuery(id)).execute()
            response.data?.Media?.let { dto ->
                val entity = mapper.toEntity(dto)
                mediaDao.insert(entity)
                Result.Success(mapper.toDomain(entity))
            } ?: Result.Error("Media not found")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error", e)
        }
    }
}
```

---

## Git Workflow

### Branch Naming

```
feature/add-character-details    # New features
fix/library-sync-crash           # Bug fixes
refactor/improve-caching         # Code improvements
docs/update-readme               # Documentation
chore/update-dependencies        # Maintenance
```

### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
type(scope): description

[optional body]

[optional footer]
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `style`: Formatting (no code change)
- `refactor`: Code refactoring
- `test`: Adding tests
- `chore`: Maintenance

**Examples:**
```bash
feat(widgets): add weekly calendar widget

fix(library): resolve sync issue when offline

docs(readme): update installation instructions

refactor(database): migrate to Room KSP
```

### Keeping Your Fork Updated

```bash
# Fetch upstream changes
git fetch upstream

# Rebase your branch on main
git checkout main
git rebase upstream/main

# Update your feature branch
git checkout feature/your-feature
git rebase main
```

---

## Pull Request Process

### Before Submitting

1. **Run all checks locally:**
   ```bash
   ./gradlew check
   ./gradlew lint
   ```

2. **Ensure your code compiles:**
   ```bash
   ./gradlew assembleDebug
   ```

3. **Test your changes:**
   - Manual testing on device/emulator
   - Run existing tests
   - Add tests for new functionality

4. **Update documentation** if needed

### PR Template

```markdown
## Summary
Brief description of changes

## Changes
- Added X feature
- Fixed Y bug
- Refactored Z component

## Testing
- [ ] Tested on emulator (API XX)
- [ ] Tested on physical device
- [ ] Added unit tests
- [ ] Added UI tests

## Screenshots (if UI changes)
| Before | After |
|--------|-------|
| img    | img   |

## Related Issues
Closes #123
```

### Review Checklist

Reviewers will check:

- [ ] Code follows style guidelines
- [ ] Architecture patterns are followed
- [ ] No hardcoded strings (use resources)
- [ ] Error handling is appropriate
- [ ] Performance considerations
- [ ] Accessibility support
- [ ] No sensitive data exposed

---

## Testing Guidelines

### Test Structure

```
app/src/
├── test/                           # Unit tests (JVM)
│   └── java/com/anisync/android/
│       ├── data/                   # Repository tests
│       ├── domain/                 # Use case tests
│       └── presentation/           # ViewModel tests
│
└── androidTest/                    # Instrumented tests
    └── java/com/anisync/android/
        ├── data/local/             # Room tests
        └── ui/                     # Compose UI tests
```

### Unit Test Example

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MediaListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: MediaListViewModel
    private lateinit var useCase: FakeGetMediaListUseCase

    @Before
    fun setup() {
        useCase = FakeGetMediaListUseCase()
        viewModel = MediaListViewModel(useCase)
    }

    @Test
    fun `initial state is loading`() = runTest {
        val state = viewModel.uiState.first()
        assertTrue(state.isLoading)
    }

    @Test
    fun `successful load updates state with media`() = runTest {
        val testMedia = listOf(createTestMedia())
        useCase.setResult(Result.Success(testMedia))

        viewModel.onAction(MediaListAction.Refresh)

        val state = viewModel.uiState.first { !it.isLoading }
        assertEquals(testMedia, state.media)
        assertNull(state.error)
    }
}
```

### Room Migration Test

```kotlin
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate1To2() {
        // Create database at version 1
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL("INSERT INTO library_entries ...")
            close()
        }

        // Run migration
        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        // Verify data integrity
        val db = helper.openNewDatabase()
        val cursor = db.query("SELECT * FROM library_entries")
        // Assert data is preserved
    }
}
```

---

## Documentation

### When to Update Docs

- Adding new features
- Changing API/architecture
- Modifying database schema
- Adding new screens/widgets
- Updating build configuration

### Documentation Files

| File | Purpose |
|------|---------|
| `README.md` | Project overview and quick start |
| `docs/ARCHITECTURE.md` | System architecture and patterns |
| `docs/DATABASE.md` | Database schema and migrations |
| `docs/API.md` | GraphQL API integration |
| `docs/NAVIGATION.md` | Screen flows and navigation |
| `docs/WIDGETS.md` | Widget architecture |
| `docs/CONTRIBUTING.md` | This file |
| `docs/CHANGELOG.md` | Version history |

### Code Comments

```kotlin
/**
 * Fetches media details from the network and caches locally.
 *
 * @param id The AniList media ID
 * @return [Result.Success] with [Media] if successful,
 *         [Result.Error] with message if failed
 *
 * @throws IllegalArgumentException if id is negative
 *
 * @sample com.anisync.android.samples.MediaSamples.fetchMediaDetails
 */
suspend fun getMedia(id: Int): Result<Media>

// Use TODO comments for known issues
// TODO: Implement retry logic for network failures

// Use FIXME for bugs that need fixing
// FIXME: This causes a memory leak on configuration change
```

---

## Questions?

If you have questions not covered here:

1. Check existing [Issues](https://github.com/Marco-9456/AniSync/issues)
2. Search [Discussions](https://github.com/Marco-9456/AniSync/discussions)
3. Open a new Discussion for questions
4. Open an Issue for bugs/features

---

Thank you for contributing to AniSync! 🎉
