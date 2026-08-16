# AniSync (Fork)

A modified fork of AniSync, a native Android client for AniList.

## Key Additions in this Fork

### 1. Google Gemini AI Assistant & History
- Integrated AI assistant powered by Google Gemini and Gemma models.
- Supported models:
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
- Features:
  - Multi-session Chat History with dedicated History tab to review, resume, and manage past AI conversations.
  - Google Search Grounding toggle for real-time web knowledge.
  - User Data context toggle to provide the assistant with your full library entries, personal scores, progress, notes, and dates.
  - Spoilers toggle to allow or restrict spoilers in answers.
  - In-Anime details contextual focus: "Ask AI Assistant" from any media page pre-loads the anime's synopsis, score, format, studio, and genres.
  - Clean top search bar integration next to the calendar button in Library and Discover.

### 2. AI News Radar in Feed
- Added dedicated "AI News Radar" tab in the Feed screen.
- Generates real-time anime news, trailer drops, release dates, cast/studio announcements, and industry updates powered by Gemini and Google Search Grounding.
- Includes topic filters (All, Trailers & PVs, Release Dates, Cast & Announcements, Industry News) with clickable source citations.

### 3. Live Airing Countdowns & Urgent Badges
- Prominent live countdown badges on Library "Watching" cards for shows airing today.
- Highlighted urgent countdown badges in the Calendar view for episodes airing soon.

### 4. Protected Hidden / Ghost List
- Dedicated "Hidden" tab placed after custom lists in the Library screen.
- Automatically captures items marked with "Hide from status lists" or set to private.
- Protected by a custom 4-digit PIN number pad and optional biometric authentication.
- Automatically resets the active tab to "All" when reopening the app, ensuring the hidden list never remains exposed.
- Hidden list item count badge is concealed on the tab header.

### 5. Search Filter: "In My List Only"
- Added filter option in media search to display only entries already present in the user's library.

### 6. Custom Package Identity
- Configured with package application ID `com.anisync.android.aq` to enable side-by-side installation with the official upstream app.

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