# AniSync

AniSync is a modern, fast, and feature-rich Android client for AniList, built with Jetpack Compose and Material Design 3. It provides anime and manga tracking, personalized recommendations, real-time schedule countdowns, and a built-in Gemini AI assistant.

---

## Features

### Library & Tracking
- **Complete AniList Sync**: Manage watching, reading, completed, planning, paused, and dropped lists with custom scoring systems and private notes.
- **Live Airing Countdowns**: Real-time countdown badges on library cards and calendar schedules for episodes airing today.
- **Custom List Sorting & Filters**: Sort by score, progress, airing time, title, or start dates with customizable tab layouts.
- **In My List Filter**: Quick filter in search to find entries already saved in your library.

### AI Assistant & Gemini Integration
- **Universal AI Companion**: Accessible directly from the top search bar in Library and Discover.
- **Context-Aware Discussions**: Open the AI Assistant from any anime or manga details page to discuss plot summaries, character relationships, watch orders, or trivia with full context preloaded.
- **Personal Library Integration**: Optional User Data toggle allows the assistant to reference your ratings, watch progress, personal notes, and dates for tailored recommendations.
- **Web Search Grounding**: Real-time web knowledge powered by Google Search Grounding for up-to-date industry information.
- **Anti-Spoiler Protection**: Strict anti-spoiler controls ensure twists and endings are never revealed when spoilers are turned off.
- **Multi-Session Chat History**: Save, resume, and manage multiple conversation threads with a dedicated History tab.
- **Custom Model Support**: Choose from preset Gemini and Gemma models (Gemini 2.5 Flash, Gemini 3 Flash Preview, Gemma 4, etc.) or input a custom model identifier.

### AI News Radar
- **Live Anime News Feed**: Dedicated tab in the Feed screen delivering breaking anime announcements, trailer drops, release dates, and voice actor/studio news.
- **Category Filters**: Filter news by Trailers and PVs, Release Dates, Cast Announcements, and Industry News.
- **Source Citations**: Every news item includes direct links to official sources and articles.
- **On-Demand Updates**: Fetch news whenever you want, grounded in current real-world dates.

### Private & Hidden Library
- **Ghost Entry Protection**: Items marked with both "Hide from status lists" and "Private" on AniList are separated into a dedicated Hidden tab.
- **PIN Lock & Biometrics**: Access to hidden entries is guarded by a 4-digit numeric keypad and optional biometric authentication.
- **Privacy First**: The hidden list auto-locks and resets to the default tab upon reopening the app, with concealed count indicators.

### Discovery & Community
- **Seasonal & Trending Feeds**: Explore trending shows, upcoming anime seasons, and top-rated media.
- **Airing Calendar**: Daily and weekly release schedule with episode reminders.
- **Social Feed & Forum**: Read community activities, reviews, discussions, and user updates.

---

## Getting Started

### Prerequisites
- Android 8.0 (API Level 26) or higher.
- An AniList account.
- Optional: A free Google Gemini API key from [Google AI Studio](https://aistudio.google.com/app/apikey) for AI features.

### Configuring AI Assistant
1. Open the app and navigate to **Settings > Gemini AI Assistant**.
2. Paste your Google Gemini API key and tap **Save Key**.
3. Select your preferred AI model and customize default preferences (Web Search, User Data, Spoilers).

---

## Building from Source

Clone the repository and build using Gradle:

```bash
# Clone repository
git clone https://github.com/editinghero/AniSync.git
cd AniSync

# Build Release APK
./gradlew assembleStableRelease

# Build Debug APK
./gradlew assembleStableDebug
```

The compiled APK will be located in `app/build/outputs/apk/stable/release/`.

---

## Package Information
- **Application ID**: `com.anisync.android.aq`
- Can be installed alongside the standard AniSync app without conflict.

---

## License

This project is licensed under the GNU General Public License v3.0 (GPLv3).