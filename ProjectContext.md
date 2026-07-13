# AniSync Plus Project Context

## Project Goal
AniSync Plus is a fork of AniSync that should keep the upstream app mostly unchanged and add a modular AniWorld calendar provider for release-related anime data. The GitHub repository was verified as public on 2026-07-13; repository visibility is an external project setting, not an application feature.

## Confirmed Scope for Version 1
- Replace release-related AniList data in the existing airing calendar and Anime/CURRENT Watching library surfaces with AniWorld calendar data.
- Keep AniList as the authority for media id, titles, cover/banner, user list status, account, normal progress, score, and existing detail navigation.
- Add manual runtime refresh, offline Room cache, German release languages, fixed 24-hour release display for affected surfaces, AniWorld-based library countdowns and Airing Soon sorting, AniSync Plus settings, separate application id, browser-based CI/APK workflow.

## Non-Goals
No MAL support, manga support, widgets/notifications/background refresh, cloud/server scraping, streaming/video/player/hoster work, GitHub Actions scraping, manual title linking in V1, redesign, or full AniSync restructuring.

## Current Upstream Baseline
- Implementation start commit: `1c69ce5ce5ed6fd6c4ebfdd213245c5dabc38e93`.
- Verified upstream merge-base: `8f1029db05b09dc612d3af061a01b0e0774d1531`.
- At implementation start the fork was 5 commits ahead and 0 behind `upstream/main`.
- `origin` points to `xxxxxxxxxxssxx/AniSyncPlus`; `upstream` points to `Marco-9456/AniSync`.
- The worktree was clean before the required historical audit file was decoded. The repository records `gradlew` without executable permission, so direct `./gradlew` invocation initially fails with exit code 126.

## Branding / Application ID
- Current app name: AniSync / AniSync Debug / AniSync Preview through Gradle `resValue`.
- Current namespace and base applicationId: `com.anisync.android`.
- Final app names: `AniSync Plus`, `AniSync Plus Debug`, and `AniSync Plus Preview`.
- Final base application id: `de.mrxxxxx.anisyncplus`.
- APK base name: `AniSyncPlus`.
- Keep the existing Kotlin namespace and packages unless a concrete technical requirement forces a change.

## Data Ownership
AniWorld owns visible release day/date/time, Staffel/Episode, DE-Sub/DE-Dub, approximate marker, calendar order/grouping, Watching countdown, and Airing Soon sorting. AniList owns identity, titles, covers, list status, account, normal user progress, score, and detail navigation.

## Calendar Rules
- Display cached AniWorld data immediately when available.
- Manual refresh parses one AniWorld calendar document and updates the local snapshot atomically after successful HTTP, parse, and structural/range validation. Matching runs separately and ambiguous or unmatched entries do not invalidate the snapshot.
- On errors, keep the last cache visible, show a visible error, and do not fall back to AniList airing data.
- Store exactly the currently delivered AniWorld calendar range, not unlimited history.
- Disable week/month navigation when the requested period has no overlap with the active snapshot range. For partial overlap, show only days inside that range.

## Filter Rules
- Current upstream filter is named `followingOnly` and filters on AniList `isOnList` in `CalendarViewModel`.
- AniSync Plus target behavior: filter off = all AniWorld entries; filter on = only safely matched AniList entries with concrete list status CURRENT/Watching.
- Future setting: remember last calendar filter. If disabled, open calendar with filter off.

## Language Rules
- Visible languages: `DE-Sub`, `DE-Dub`, `DE-Sub / DE-Dub` only.
- Recognize internally at least GERMAN_DUB, GERMAN_SUB, ENGLISH_DUB, ENGLISH_SUB, UNKNOWN.
- Do not infer language from duplicate counts. Prefer specific markers before generic markers.
- Interpret source dates and times in `Europe/Berlin`; group by the AniWorld source day and use the resolved instant for countdown/sorting.
- A DST gap is a validation error. A DST overlap uses the earlier valid offset and records a diagnostic.
- `~ HH:mm Uhr` stays approximate in display, uses the resolved instant for sorting/countdown, and has no invented tolerance window.

## Library Rules
- Scope: Anime media type and CURRENT/Watching tab only.
- Calendar and library must share a central effective release resolver/repository; no duplicate matching logic inside library UI.
- Library countdown uses the earliest future German AniWorld release. If none exists, show no AniList airing fallback and preserve normal progress/actions.
- Airing Soon sorting must use AniWorld timestamps for Watching, placing entries without AniWorld release after valid timed entries.

## Cache / Database Rules
- Prefer a separate Room database named `anisync_plus_calendar.db` instead of changing the upstream `anisync.db`.
- Required tables: `aniworld_snapshots`, `aniworld_release_entries`, `aniworld_media_mappings`, `aniworld_sync_state`.
- Cache writes must be snapshot/transaction based and must not clear existing data before a successful replacement is ready.

## Matching Rules
Matching order: stored mapping, exact normalized userPreferred, English, Romaji, native, synonyms, season notation, then cautious unambiguous similarity. Status values: MATCHED, AMBIGUOUS, UNMATCHED, MANUALLY_CONFIRMED. Manual linking is not in V1 but the schema must not block it.

## Module Decision
Use `:anisyncplus-calendar` if the verified AGP/Kotlin/KSP/Room/Hilt toolchain supports a stable Android library module. The module must not depend on `:app`; app-specific AniList adapters remain in `:app`.

## Screenshots Still Needed
Calendar filter off/on, calendar card with time/episode/status/score, Watching library grid/list, library card with `Episode X in ...`, lower settings menu showing Updates/About, and desired DE-Sub/DE-Dub placement.

## Fixed V1 Decisions
- AniWorld calendar defaults to enabled.
- Remember-calendar-filter defaults to disabled; when disabled, the calendar opens unfiltered.
- Unmatched entries use existing Material icons and theme colors, with no binary placeholder asset.
- Matching diagnostics are visible as aggregate settings/status data; manual title linking is not part of V1.
- Supported release kinds are episode, film, special, and unknown. Film/special entries never receive an invented episode or behind badge.
- AniList search is permitted only as a bounded identity/title/cover candidate source during manual refresh; AniList release dates, times, episode numbers, countdowns, behind calculations, and target-surface airing sorting are forbidden.

## Decision Log
- 2026-07-13: Phase 1 is documentation-only forensic analysis; no production feature code.
- 2026-07-13: Prefer separate AniSync Plus calendar module and separate Room DB, pending user approval.
- 2026-07-13: No visible or sorting fallback to AniList airing fields in target surfaces.
- 2026-07-13: Master implementation prompt fixed the final application id, Berlin timezone/DST policy, snapshot navigation limits, approximate-time semantics, release kinds, feature defaults, matching candidate policy, and placeholder behavior.

## Change Log
- 2026-07-13: Created initial project context and forensic documentation set.
- 2026-07-13: Fixed GitHub Actions release signing preparation to validate decoded keystores, use absolute temporary keystore paths, and generate a temporary CI key for manual artifacts when secrets are missing or invalid. Tag-triggered releases still require valid signing secrets.
- 2026-07-13: Changed the manual APK workflow to default to a debug APK for browser-only testing, keep release as an explicit option, and collect APK artifacts from `app/build` instead of assuming one AGP output directory.
