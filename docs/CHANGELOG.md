# Changelog

All notable changes to AniSync will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

## [1.3.1] - 2026-04-06

### Fixed
- **Score Slider Color Mapping** - Normalized score color thresholds against each active score format (`POINT_3`, `POINT_5`, `POINT_10`, `POINT_10_DECIMAL`, `POINT_100`) so visual feedback now scales consistently.

## [1.3.0] - 2026-04-06

### Added

#### Library & Tracking
- **Notes and Custom Lists Editing** - Added notes and AniList custom list support across library and media details editing flows.
- **Score Format Awareness** - `EditLibraryEntrySheet` now receives and applies score format from the library context.

#### Discovery & Search
- **Enhanced Discover Search** - Improved Discover with better integrated search behavior.
- **SearchAll Integration** - Added `SearchAll` GraphQL query and unified search result modeling.

#### Media Details & Rich Content
- **Character & Staff Expansion** - Added and enhanced staff details plus richer character details with voice actor filtering.
- **Recommendations & Reviews UI** - Added recommendations and reviews sections with voting support.
- **Richer Link Cards** - AniList links now render with cover images and titles in rich text.

### Changed
- **Update Dialog Redesign** - Reworked update UX with a `ModalBottomSheet` implementation.
- **Details UI Refactors** - Simplified section structure and extracted reusable grid/item components.
- **Library Data Pipeline** - Extended GraphQL, data, and repository layers to carry notes and custom list metadata end-to-end.
- **Database Schema** - Added Room schema exports for database version 8.

### Fixed
- **Custom List Visibility Logic** - Standard lists now correctly hide `hiddenFromStatusLists` entries while preserving visibility in custom lists.
- **Entry Editing from Details FAB** - Fixed missing custom list context when opening `EditLibraryEntrySheet` from media details.
- **Staff/Character Navigation & Links** - Fixed deep links, visibility, sharing, and related navigation issues.
- **Rich Text Image Width Parsing** - Correctly handles AniList hash-prefixed width image syntax.

---

## [1.2.0] - 2026-03-30

### Added

#### Library & Custom Lists
- **Custom Lists Integration** - Full support for creating, syncing, and managing custom AniList library lists.
- **Type-Specific Preferences** - Separate list ordering and visibility preferences for your Anime and Manga lists.

#### Media Viewing
- **Immersive Image Viewer** - New fullscreen image viewer supporting pinch-to-zoom, double-tap-to-zoom, panning, and swiping through galleries.
- **Image Downloads** - Added ability to download images directly to the device's Downloads folder.
- **Custom Video Player** - Built a custom Material 3 Expressive UI over ExoPlayer with smooth seeking and full playback controls.
- **ExoPlayer Caching** - Reuses ExoPlayer instances during scrolling for buttery smooth performance.

#### Navigation & Integration
- **In-App Deep Linking** - AniList URLs for anime, manga, characters, and forum threads now open natively in the app instead of an external browser.

#### Social & Forums
- **Social Notifications** - Push notifications for forum replies, mentions, comment/thread likes, and thread subscriptions.
- **Granular Settings** - Redesigned settings to exactly control which types of social notifications you receive.
- **Performance Improvements** - Re-engineered complex thread rendering to flatten comment trees, drastically improving scrolling performance.
- **Enhanced Thread Parsing** - Parses native `@User` mentions to properly reconstruct "flat" AniList threads into a Reddit-style comment tree.
- **Improved UI & Animations** - Added organic curved lines for comment tracking, animated scroll-to highlighted comments.

### Changed
- **Centralized Errors & Rate Limiting** - A robust networking system that properly handles 429 Rate Limits, 401 Unauthorized errors (with re-auth prompts), network drops, and GraphQL-level structural errors across all repositories.
- **Html/Markdown Parsing** - Overhauled the underlying `RichTextParser` to fully support recursive structures like nested blockquotes, custom tables, inline images, code blocks, and spoilers without visual glitches.
- **Media Thumbnails** - Explicit cache policies logic for better UI performance out-of-the-box.

## [1.1.0] - 2026-03-01

### Added

#### Forum
- **Forum Hub** - New "Forum" tab in main navigation with three feeds: Overview, Recent, and New
- **Thread Browsing** - Browse threads by category with horizontally scrollable category chips
- **Thread Detail** - Full thread view with nested comment rendering (up to 3 levels deep), Reddit-style colored nesting lines, and OP badges
- **Thread Creation** - Compose new threads with category selection, markdown preview toggle, character counter, and unsaved-changes confirmation dialog
- **Comment Replies** - Reply to threads and comments via bottom sheet with markdown formatting toolbar (bold, italic, strikethrough, code, links, spoilers) and preview toggle
- **Thread Search** - Debounced search across forum threads with immersive full-screen search bar
- **Thread Saving (Bookmarks)** - Save threads locally for offline access; "Saved" feed displays bookmarked threads from local database
- **Thread Subscriptions** - Subscribe/unsubscribe to threads via the AniList API; "Subscribed" feed for followed threads
- **Comment Sorting** - Sort comments by Newest or Oldest
- **Thread Sharing** - Share thread URLs directly from the detail screen
- **Comment Collapsing** - Collapse/expand comment sub-trees with descendant count
- **Inline Images** - Parse and render `img###(url)`, `![alt](url)`, and `<img>` syntaxes with fullscreen pinch-to-zoom viewer
- **Flat Reply Parsing** - Reconstruct comment trees from `@Username` mention-based replies
- **Rich Markdown/HTML Parser** - Comprehensive parser supporting headers, lists, blockquotes, horizontal rules, inline code, spoilers, alignment wrappers, and HTML entities (named and numeric)
- **Empty States** - Dedicated empty state screens for no threads, no comments, and no search results, each with contextual actions
- **Pull-to-Refresh** - Pull-to-refresh on Forum Hub, Category, and Thread Detail screens
- **Staggered Animations** - Animated entry for headers, chips, and list items across all forum screens
- **Optimistic Updates** - Instant UI feedback for liking, saving, and subscribing actions

#### In-App Updates
- **Update Manager** - New `UpdateManager` singleton centralizing update logic (check, download, install) with `StateFlow`-based state management
- **Background Update Checks** - Periodic checks every 6 hours via `UpdateCheckWorker` with notification when a new version is available
- **Automatic Launch Check** - App checks for updates on startup
- **Update Dialog** - Reusable composable showing release notes, download progress, and install prompts
- **Atomic Downloads** - Downloads write to `.tmp` file and rename on success to prevent partial installs
- **Unknown Sources Flow** - Handles "Install from Unknown Sources" permission on Android O+

#### Developer Tools (Debug Builds Only)
- **Developer Tools Screen** - Consolidated debug utilities accessible from Settings
- **Update Debug** - Force update check or simulate available update
- **Build Information** - Display version, build type, flavor, and other build details

### Changed
- **Haptic Feedback** - Replaced custom `HapticFeedbackHelper` with Compose's `HapticFeedback` API, then added a robust 4-tier fallback chain for reliable haptics across devices and API levels (including Samsung One UI Core)
- **`HtmlText` Component** - Moved from `forum` package to shared `components` package; migrated to modern `Text` with `LinkAnnotation`
- **Hilt ViewModels** - Switched to `hiltViewModel()` from deprecated `hiltNavigation.compose.hiltViewModel()`
- **RTL Support** - Use `Icons.AutoMirrored.Filled.Reply` for proper LTR/RTL layout
- **Forum Thread Cards** - Redesigned with modern layout, media thumbnails, last reply info, and pinned/locked indicators
- **Timestamp Formatting** - More granular relative timestamps (e.g., "2w ago")
- **Database Schema** - Squashed intermediate migrations (v4/v5/v6) into a single v3 -> v4 auto-migration for clean upgrade path
- **CI Workflow** - Use PAT for release notes generation

### Fixed
- **`EditLibraryEntrySheet`** - Sheet now automatically closes after saving an entry
- **Animated Favorite Button** - Fixed size to prevent layout shifts during sparkle animation
- **VIBRATE Permission** - Removed; haptic feedback now delegated to Android system

### Removed
- **`UpdateUtil.kt`** - Replaced by the new `UpdateManager`
- **`ForumCategoryScreen`** (legacy) - Replaced by the new integrated forum UI
- **Notification Debug Section** - Moved from `NotificationsScreen` to `DeveloperToolsScreen`

---

## [1.0.1] - 2026-02-22

### Added
- Initial documentation suite with Mermaid diagrams
- Room database migration infrastructure
- Schema export for migration testing

### Changed
- Reset database version to 1 for clean migration path
- Enabled Room schema export for version tracking

### Fixed
- Custom seed color `Color(0x00000000)` (transparent black) was incorrectly treated as "no color set" due to 0-value collision in `AppSettings`
- Orphaned palette state when clearing custom seed color while `selectedPaletteId` is `"custom"` -- now auto-resets to `"dynamic"`
- `PaletteStyleSelector` could be interacted with in disabled contexts -- added `enabled` parameter gating expand and input
- Dynamic palette preview in `ColorSchemeSelector` ignored dark mode -- now uses `dynamicDarkColorScheme`/`dynamicLightColorScheme` on Android 12+
- Color picker hue slider state lost on configuration change -- changed from `remember` to `rememberSaveable`
- Dynamic palette option was shown on pre-Android 12 devices where it is unsupported -- now filtered out
- Orphaned palette state when device conditions change (e.g., restored backup from Android 12+ device to pre-12) -- added `LaunchedEffect` auto-reset
- `PaletteStyleSelector` was interactive when Dynamic palette was selected (palette style has no effect on dynamic colors) -- now disabled
- `getTitleLanguageLabel()` and `getTitleLanguageExample()` in `LookAndFeelScreen` were unnecessarily public -- made `private`

### Removed
- Legacy migration code (MIGRATION_12_13)

---

## [1.0.0] - YYYY-MM-DD

### Added

#### Core Features
- **Library Management** - Track anime/manga with progress, scores, notes
- **Status Tracking** - Watching, Planning, Completed, Dropped, Paused
- **AniList Sync** - Full synchronization with AniList.co account
- **Offline Support** - Full functionality without internet connection

#### Discovery
- **Trending** - Browse currently trending anime
- **Popular This Season** - See what's popular this season
- **Upcoming Releases** - Discover upcoming anime
- **Advanced Search** - Search with genre, year, format filters

#### Media Details
- **Comprehensive Info** - Synopsis, genres, studios, airing info
- **Characters & Voice Actors** - Character details with VA information
- **Related Media** - Sequels, prequels, side stories
- **Streaming Links** - Direct links to streaming platforms

#### Home Screen Widgets
- **Up Next Widget** - Upcoming episodes with countdown timers
- **Airing Today Widget** - Timeline view of today's episodes
- **Weekly Calendar Widget** - 7-day anime schedule overview
- **Trending Widget** - Top trending anime grid

#### Notifications
- **Watching Alerts** - Notifications for new episodes
- **Planning Alerts** - Know when planned shows premiere
- **Upcoming Alerts** - Two-tier system (12h and 2h before)
- **Configurable** - Enable/disable per notification type

#### User Experience
- **Material 3 Design** - Modern Material You theming
- **Dynamic Colors** - Adapts to system wallpaper colors
- **Dark Mode** - Full dark theme support
- **Smooth Animations** - Polished transitions and effects

### Technical
- Kotlin 2.2 with Jetpack Compose
- MVVM + Clean Architecture
- Hilt dependency injection
- Apollo GraphQL 4.x
- Room database with KSP
- WorkManager for background tasks
- Jetpack Glance for widgets

---

## Version History Template

When releasing a new version, copy this template:

```markdown
## [X.Y.Z] - YYYY-MM-DD

### Added
- New features

### Changed
- Changes to existing functionality

### Deprecated
- Features to be removed in future versions

### Removed
- Features removed in this release

### Fixed
- Bug fixes

### Security
- Security-related changes
```

---

## Categories Explained

- **Added** - New features
- **Changed** - Changes to existing functionality
- **Deprecated** - Features that will be removed in future versions
- **Removed** - Features removed in this release
- **Fixed** - Bug fixes
- **Security** - Vulnerability fixes

---

## Release Checklist

Before tagging a release:

- [ ] Update version in `app/build.gradle.kts`
- [ ] Update this CHANGELOG
- [ ] Run full test suite
- [ ] Build release APK/AAB
- [ ] Test on multiple devices
- [ ] Update screenshots (if UI changed)
- [ ] Create GitHub release with notes

---

## Links

- [GitHub Releases](https://github.com/Marco-9456/AniSync/releases)
- [Issue Tracker](https://github.com/Marco-9456/AniSync/issues)
- [Contributing Guide](CONTRIBUTING.md)
