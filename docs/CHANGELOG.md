# Changelog

All notable changes to AniSync will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

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
