# AniSync (Fork)

A modified fork of AniSync, a native Android client for AniList.

## Key Additions & Differences from Original

### 1. Hidden / Ghost List System
- **How the Hidden Logic Works:**
  - When an anime or manga entry is marked with **"Hidden from status lists"** or set to **"Private"** on AniList (or in-app), AniSync classifies it as a ghost/hidden entry.
  - Ghost entries are filtered out from all regular library tabs (Watching, Completed, Planning, Paused, Dropped, Repeating) and general search results.
  - All hidden entries are collected into a dedicated **Hidden** tab in the Library.
- **Protection & Security:**
  - Protected with a built-in 4-digit numeric keypad with auto-unlock upon entering the correct PIN.
  - The hidden item count badge on the tab is hidden for privacy.
  - Whenever the app is reopened or restarted, the selected tab resets automatically to "All" so the hidden list is never left open or exposed.

### 2. Google Gemini AI Assistant
- Integrated AI assistant powered by Google Gemini and Gemma models.
- **Supported Models:**
  - gemini-2.5-flash
  - gemini-2.5-flash-lite
  - gemini-3-flash-preview
  - gemini-3.1-flash-lite
  - gemini-3.5-flash
  - gemini-3.5-flash-lite
  - gemini-3.6-flash
  - gemini-3.7-flash
  - gemma-4-31b-it
  - gemma-4-26b-a4b-it
  - Custom model ID support
- **Features:**
  - Contextual In-Anime Chat: Opening AI Chat from an anime or manga details screen pre-loads full media context (synopsis, studios, scores, format, genres).
  - User Data Toggle: Allows the AI to read your personal library data (progress, personal scores, notes, dates) when enabled.
  - Spoiler Control: Toggle to strictly prevent or allow spoilers in answers.
  - Google Search Grounding: Toggle for real-time web search grounding.
  - Chat History: Dedicated History tab in AI Chat to view and review past conversations.
  - Placement: AI Assistant button integrated into the top search bar across Home and Discover screens.

### 3. AI News Radar (Feed Screen)
- Live anime and manga news feed tab powered by Google Gemini with real-time web search grounding.
- Topic filters: All, Trailers & PVs, Release Dates, Cast & Announcements, and Industry News.
- Persistent state: News results stay loaded across tab switching and only refresh when requested by the user.

### 4. Real-time Airing Countdown Badges
- Live episode countdown badges on Library "Watching" cards and Calendar view for anime airing today.

### 5. Search Filters
- Added "On my list only" filter in Discover search to restrict results to entries already present in the user's library.

### 6. Package Identity & Fast Release CI
- Application ID `com.anisync.android.aq` to allow installing alongside the official upstream app.
- Streamlined GitHub Actions workflow focused on building release APKs directly on push.

## Build and Installation

Build the release APK:
```bash
./gradlew assembleStableRelease
```

Build the debug APK:
```bash
./gradlew assembleStableDebug
```

## License

This project is licensed under the GNU General Public License v3.0 (GPLv3).
