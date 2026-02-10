<p align="center">
  <img src="docs/assets/logo.svg" alt="AniSync Logo" width="120" height="120">
</p>

<h1 align="center">AniSync</h1>

<p align="center">
  <strong>A modern Android client for AniList - Track your anime and manga journey</strong>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#screenshots">Screenshots</a> •
  <a href="#tech-stack">Tech Stack</a> •
  <a href="#getting-started">Getting Started</a> •
  <a href="#documentation">Documentation</a> •
  <a href="#contributing">Contributing</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" alt="Platform">
  <img src="https://img.shields.io/badge/Min%20SDK-26-blue" alt="Min SDK">
  <img src="https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white" alt="Compose">
  <img src="https://img.shields.io/badge/License-MIT-green" alt="License">
</p>

---

## Overview

**AniSync** is a native Android application for [AniList.co](https://anilist.co) - the popular anime and manga tracking platform. Built with modern Android development practices, AniSync provides a seamless experience for tracking your watching/reading progress, discovering new content, and staying updated with episode releases.

### Why AniSync?

- **Offline-First**: Full functionality even without internet connection
- **Beautiful UI**: Modern Material 3 design with smooth animations
- **Smart Notifications**: Know when your favorite shows air
- **Home Screen Widgets**: Quick access to your anime schedule
- **Privacy-Focused**: Your credentials are encrypted locally

---

## Features

```mermaid
mindmap
  root((AniSync))
    Library Management
      Track Progress
      Score & Review
      Custom Notes
      Multiple Statuses
      Sort & Filter
    Discovery
      Trending Anime
      Popular This Season
      Upcoming Releases
      Advanced Search
      Genre Filters
    Media Details
      Full Information
      Characters & VA
      Related Media
      Streaming Links
      Favorites
    Profile
      Statistics
      Recent Activity
      Favorites
      Settings
    Widgets
      Up Next
      Airing Today
      Weekly Calendar
      Trending
    Notifications
      Episode Alerts
      Premiere Alerts
      Custom Timing
```

### Core Features

| Feature | Description |
|---------|-------------|
| **Library Management** | Track anime/manga with progress, scores, notes, and custom statuses (Watching, Planning, Completed, Dropped, Paused) |
| **Smart Discovery** | Browse trending, popular, upcoming, and TBA anime/manga with powerful search and filters |
| **Media Details** | View comprehensive information including characters, voice actors, relations, and streaming links |
| **User Profile** | View your stats, recent activity, favorites, and manage app settings |
| **Character Browser** | Explore character details and their appearances across different media |
| **Statistics** | Detailed breakdown of your watching/reading habits by genre, score, format, and more |

### Home Screen Widgets

| Widget | Description |
|--------|-------------|
| **Up Next** | Shows upcoming episodes from your watching list with countdown timers |
| **Airing Today** | Timeline view of all episodes airing today |
| **Weekly Calendar** | 7-day calendar view of your anime schedule |
| **Trending** | Top 10 trending anime at a glance |

### Notification System

- **Watching Notifications**: Get notified when episodes from your watching list air
- **Planning Notifications**: Know when shows in your planning list premiere
- **Upcoming Notifications**: Discover popular upcoming shows
- **Customizable Timing**: Set notification lead time (15min to 1 day before)

---

## Screenshots

<p align="center">
  <img src="docs/assets/screenshot-library.png" width="200" alt="Library">
  <img src="docs/assets/screenshot-discover.png" width="200" alt="Discover">
  <img src="docs/assets/screenshot-details.png" width="200" alt="Details">
  <img src="docs/assets/screenshot-profile.png" width="200" alt="Profile">
</p>

---

## Tech Stack

| Category | Technology |
|----------|------------|
| **Language** | Kotlin 2.2 |
| **UI Framework** | Jetpack Compose with Material 3 Expressive |
| **Architecture** | MVVM + Clean Architecture (Use Cases) |
| **Dependency Injection** | Hilt / Dagger |
| **Navigation** | Navigation Compose (Type-safe routes) |
| **Networking** | Apollo GraphQL 4.x |
| **Local Database** | Room with KSP |
| **Theming** | MaterialKolor (dynamic palette styles & seed colors) |
| **Image Loading** | Coil |
| **Background Work** | WorkManager |
| **Widgets** | Jetpack Glance |
| **Serialization** | Kotlinx Serialization |
| **Security** | EncryptedSharedPreferences (AES-256-GCM) |
| **Min SDK** | 26 (Android 8.0 Oreo) |
| **Target SDK** | 35 (Android 15) |
| **Compile SDK** | 36 |

---

## Getting Started

### Prerequisites

- Android Studio Ladybug (2024.2.1) or newer
- JDK 17
- Android SDK with API 26+

### Building the Project

1. **Clone the repository**
   ```bash
   git clone https://github.com/Marco-9456/AniSync.git
   cd AniSync
   ```

2. **Open in Android Studio**
   - File → Open → Select the project directory
   - Wait for Gradle sync to complete

3. **Run the app**
   - Select a device/emulator (API 26+)
   - Click Run (▶) or press `Shift + F10`

### Configuration

#### AniList API Setup

The app uses AniList's public GraphQL API. For authenticated features (library management, profile), users log in with their AniList account via OAuth.

No additional API configuration is required - the app is pre-configured for AniList.

#### Build Variants

| Variant | Package ID | Description |
|---------|------------|-------------|
| `debug` | `com.anisync.android.debug` | Development build with debug features |
| `release` | `com.anisync.android` | Production build with ProGuard |

Both variants can be installed side-by-side for testing.

---

## Documentation

Comprehensive documentation is available in the `docs/` folder:

| Document | Description |
|----------|-------------|
| [**ARCHITECTURE.md**](docs/ARCHITECTURE.md) | System architecture, patterns, and layer responsibilities |
| [**DATABASE.md**](docs/DATABASE.md) | Room database schema, migrations, and caching strategy |
| [**API.md**](docs/API.md) | GraphQL integration, authentication, and API reference |
| [**NAVIGATION.md**](docs/NAVIGATION.md) | Screen flows, navigation graph, and deep links |
| [**WIDGETS.md**](docs/WIDGETS.md) | Widget architecture and notification system |
| [**CONTRIBUTING.md**](docs/CONTRIBUTING.md) | Contribution guidelines and code style |
| [**CHANGELOG.md**](docs/CHANGELOG.md) | Version history and release notes |

### Quick Links

- **Adding a new screen?** → See [NAVIGATION.md](docs/NAVIGATION.md)
- **Changing database schema?** → See [DATABASE.md](docs/DATABASE.md) ⚠️
- **Understanding data flow?** → See [ARCHITECTURE.md](docs/ARCHITECTURE.md)
- **Working with widgets?** → See [WIDGETS.md](docs/WIDGETS.md)

---

## Project Structure

```
AniSync/
├── app/
│   ├── src/main/
│   │   ├── java/com/anisync/android/
│   │   │   ├── data/           # Data layer (repositories, local DB)
│   │   │   ├── di/             # Hilt dependency injection modules
│   │   │   ├── domain/         # Domain layer (models, interfaces, use cases)
│   │   │   ├── presentation/   # UI layer (screens, ViewModels)
│   │   │   ├── ui/theme/       # Compose theme (colors, typography)
│   │   │   ├── util/           # Utility functions
│   │   │   ├── widget/         # Glance widgets
│   │   │   └── worker/         # WorkManager jobs
│   │   ├── graphql/            # GraphQL queries and mutations
│   │   └── res/                # Resources (layouts, strings, drawables)
│   └── schemas/                # Room schema exports (for migrations)
├── docs/                       # Documentation
└── gradle/                     # Gradle configuration
```

---

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](docs/CONTRIBUTING.md) for guidelines.

### Quick Start for Contributors

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Run tests and lint (`./gradlew check`)
5. Commit with a descriptive message
6. Push to your fork and create a Pull Request

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Acknowledgments

- [AniList](https://anilist.co) for providing the excellent GraphQL API
- [Material Design 3](https://m3.material.io) for the design system
- The Android and Kotlin communities for amazing tools and libraries

---

<p align="center">
  Made with ❤️ for anime fans
</p>
