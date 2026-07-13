# AniSync Plus Project Context

## Project Goal
AniSync Plus is a private fork of AniSync that should keep the upstream app mostly unchanged and add a modular AniWorld calendar provider for release-related anime data.

## Confirmed Scope for Version 1
- Replace release-related AniList data in the existing airing calendar and Anime/CURRENT Watching library surfaces with AniWorld calendar data.
- Keep AniList as the authority for media id, titles, cover/banner, user list status, account, normal progress, score, and existing detail navigation.
- Add manual runtime refresh, offline Room cache, German release languages, fixed 24-hour release display for affected surfaces, AniWorld-based library countdowns and Airing Soon sorting, AniSync Plus settings, separate application id, browser-based CI/APK workflow.

## Non-Goals
No MAL support, manga support, widgets/notifications/background refresh, cloud/server scraping, streaming/video/player/hoster work, GitHub Actions scraping, manual title linking in V1, redesign, or full AniSync restructuring.

## Current Upstream Baseline
- Local repository commit at analysis start: `8f1029db05b09dc612d3af061a01b0e0774d1531`.
- GitHub `Marco-9456/AniSync` `main` commit observed via GitHub API on 2026-07-13: `8f1029db05b09dc612d3af061a01b0e0774d1531` dated 2026-07-09T00:08:09Z.
- No git remotes are configured in this Codex checkout; `git ls-remote` returned HTTP 403, while the GitHub API request succeeded.
- Pre-existing uncommitted change observed before Phase 1 edits: `gradlew` modified. Phase 1 did not intentionally edit it.

## Branding / Application ID
- Current app name: AniSync / AniSync Debug / AniSync Preview through Gradle `resValue`.
- Current namespace and base applicationId: `com.anisync.android`.
- Requested future private app name: AniSync Plus.
- Requested future application id from the concrete first prompt: `de.mrxxxxx.anisyncplus`. The repeated prompt also contains `<APPLICATION_ID>`; this is an open confirmation item before implementation.
- Do not change branding or package names in Phase 1.

## Data Ownership
AniWorld owns visible release day/date/time, Staffel/Episode, DE-Sub/DE-Dub, approximate marker, calendar order/grouping, Watching countdown, and Airing Soon sorting. AniList owns identity, titles, covers, list status, account, normal user progress, score, and detail navigation.

## Calendar Rules
- Display cached AniWorld data immediately when available.
- Manual refresh parses one AniWorld calendar document and updates the local snapshot atomically only after successful parse/validation/matching.
- On errors, keep the last cache visible, show a visible error, and do not fall back to AniList airing data.
- Store exactly the currently delivered AniWorld calendar range, not unlimited history.

## Filter Rules
- Current upstream filter is named `followingOnly` and filters on AniList `isOnList` in `CalendarViewModel`.
- AniSync Plus target behavior: filter off = all AniWorld entries; filter on = only safely matched AniList entries with concrete list status CURRENT/Watching.
- Future setting: remember last calendar filter. If disabled, open calendar with filter off.

## Language Rules
- Visible languages: `DE-Sub`, `DE-Dub`, `DE-Sub / DE-Dub` only.
- Recognize internally at least GERMAN_DUB, GERMAN_SUB, ENGLISH_DUB, ENGLISH_SUB, UNKNOWN.
- Do not infer language from duplicate counts. Prefer specific markers before generic markers.

## Library Rules
- Scope: Anime media type and CURRENT/Watching tab only.
- Calendar and library must share a central effective release resolver/repository; no duplicate matching logic inside library UI.
- Library countdown uses the earliest future German AniWorld release. If none exists, show no AniList airing fallback and preserve normal progress/actions.
- Airing Soon sorting must use AniWorld timestamps for Watching, placing entries without AniWorld release after valid timed entries.

## Cache / Database Rules
- Prefer a separate Room database named `anisync_plus_calendar.db` instead of changing the upstream `anisync.db`.
- Proposed tables: `aniworld_release_entries`, `aniworld_media_mappings`, `aniworld_sync_state`.
- Cache writes must be snapshot/transaction based and must not clear existing data before a successful replacement is ready.

## Matching Rules
Matching order: stored mapping, exact normalized userPreferred, English, Romaji, native, synonyms, season notation, then cautious unambiguous similarity. Status values: MATCHED, AMBIGUOUS, UNMATCHED, MANUALLY_CONFIRMED. Manual linking is not in V1 but the schema must not block it.

## Module Decision
No module has been added yet. The Phase 1 recommendation is to add `:anisyncplus-calendar` with API/data/parser/local/repository/domain/settings packages, while keeping integration with existing app surfaces minimal and explicit.

## Screenshots Still Needed
Calendar filter off/on, calendar card with time/episode/status/score, Watching library grid/list, library card with `Episode X in ...`, lower settings menu showing Updates/About, and desired DE-Sub/DE-Dub placement.

## Open Questions
1. Confirm final application id: `de.mrxxxxx.anisyncplus` or another value?
2. AniWorld timezone: page timestamps look like German local calendar times, but no explicit timezone marker was found in the DOM. Should V1 assume `Europe/Berlin` once validated by user screenshots, or keep it configurable/diagnostic?
3. Should `AniWorld-Kalender aktivieren` default to enabled for AniSync Plus once implemented?
4. Should unmatched calendar entries use a generated placeholder color/icon, or a specific bundled placeholder asset?
5. How detailed should the matching diagnostics UI be in V1 versus documentation/log-only diagnostics?

## Decision Log
- 2026-07-13: Phase 1 is documentation-only forensic analysis; no production feature code.
- 2026-07-13: Prefer separate AniSync Plus calendar module and separate Room DB, pending user approval.
- 2026-07-13: No visible or sorting fallback to AniList airing fields in target surfaces.

## Change Log
- 2026-07-13: Created initial project context and forensic documentation set.
- 2026-07-13: Fixed GitHub Actions release signing preparation to validate decoded keystores, use absolute temporary keystore paths, and generate a temporary CI key for manual artifacts when secrets are missing or invalid. Tag-triggered releases still require valid signing secrets.
- 2026-07-13: Changed the manual APK workflow to default to a debug APK for browser-only testing, keep release as an explicit option, and collect APK artifacts from `app/build` instead of assuming one AGP output directory.
