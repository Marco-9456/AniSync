# AniSync Plus Forensic Analysis (Phase 1)

## Scope and Evidence
This document records the current repository state inspected on 2026-07-13. Phase 1 created documentation only and no production feature code.

## Git / Upstream
- Local commit: `8f1029db05b09dc612d3af061a01b0e0774d1531`.
- Upstream GitHub API `Marco-9456/AniSync/main`: `8f1029db05b09dc612d3af061a01b0e0774d1531` (2026-07-09T00:08:09Z).
- `git remote -v` prints no configured remotes in this checkout.
- Pre-existing dirty file before this analysis: `gradlew` modified. It was not part of this documentation work.

## Build and Project Structure
- `settings.gradle.kts` sets `rootProject.name = "AniSync"` and includes only `:app`.
- Root `build.gradle.kts` declares Android application, Compose compiler, Hilt, Apollo, KSP, and Kotlin serialization plugin aliases.
- `app/build.gradle.kts` uses namespace/applicationId `com.anisync.android`, compileSdk/targetSdk 36, minSdk 26, Java/Kotlin JVM 17, versionCode 19, versionName 3.1.0.
- Gradle wrapper distribution: Gradle 9.3.1. Version catalog: AGP 9.1.0, Kotlin 2.2.21, KSP 2.3.2, Room 2.8.4, Apollo 4.4.3, Jsoup 1.22.2, OkHttp 4.12.0.
- Product flavors: `stable` default and `preview`; debug adds `.debug` application id suffix and app name `AniSync Debug`.
- APK rename task outputs names like `AniSync-v<version>-<abi>-<buildType>.apk` with flavor suffix for non-stable.
- `.github/workflows` contains manual APK and signed-release workflows plus the upstream sponsor workflow. At the implementation baseline the APK workflows build but do not run unit tests or lint, configure Gradle 8.13 despite invoking the 9.3.1 wrapper, and collect APKs through broad searches.

## Calendar Code Map
- `CalendarRepositoryImpl` calls AniList `AiringScheduleQuery`, paginates max 10 pages of 50, filters adult media via `AppSettings.showAdultContent`, maps media/list fields to `AiringEpisode`, and sorts by `airingAt`.
- `AiringEpisode` contains AniList schedule id, episode, `airingAt`, media id, titles, cover, format, averageScore, `isOnList`, optional list status, and adult flag.
- `CalendarViewModel` owns week/month state, `rawEpisodes`, `rawMonthEpisodes`, system default `ZoneId`, and calls `CalendarRepository.getWeekSchedule` for both week and 42-day month grid.
- `buildDayBuckets` filters with `episodes.filter { it.isOnList }` when `followingOnly` is true, groups by `Instant.ofEpochSecond(it.airingAt).atZone(zoneId).toLocalDate()`, and sorts by `airingAt`.
- `CalendarUiState.followingOnly` defaults to false and is not persisted. Toggle is in ViewModel; Compose sends `CalendarAction.ToggleFollowingOnly`.
- Week and month views share the same filter state. Month dots and selected-day list are rebuilt from `rawMonthEpisodes` on toggle.
- Pull-to-refresh triggers `CalendarAction.Refresh` or `RefreshMonth`; the repository performs network fetch directly. No offline calendar cache exists for this screen.
- Calendar UI navigation uses existing media detail navigation by media id for clicked cards.

## Current Calendar Filter Behavior vs Target
Current behavior: filter off shows all AniList schedule entries returned by the query; filter on shows AniList entries where `media.mediaListEntry != null`, regardless of concrete list status. Standard state is off on new ViewModel creation. The state is in memory only and not stored. The filter is applied in `CalendarViewModel.buildDayBuckets`, not inside Compose.

Target AniSync Plus behavior: filter off shows all AniWorld entries; filter on shows only safely matched entries whose AniList status is CURRENT. Unmatched entries are hidden only when the filter is on. Optional remember setting controls whether the last state is restored; otherwise open with filter off.

## Library Release Code Map
- `LibraryRepositoryImpl.refreshLibrary` maps AniList `media.nextAiringEpisode.episode`, `.timeUntilAiring`, and `.airingAt` to `LibraryEntry` and then Room.
- `LibraryEntryEntity` stores `nextAiringEpisode`, `timeUntilAiring`, and `nextAiringEpisodeTime`, with an index on `timeUntilAiring` for Airing Soon.
- `LibraryEntry.dynamicTimeUntilAiring` recalculates remaining seconds from `nextAiringEpisodeTime` when available, else uses the stored snapshot `timeUntilAiring`.
- `LibraryViewModel` observes Room library, applies media type/status/custom filters and search, then sorts. `LibrarySort.AIRING_SOON` sorts by `entry.nextAiringEpisodeTime`; nulls are placed last in ascending mode.
- Grid `LibraryMediaCard` computes episodes behind for CURRENT as `(nextAiringEpisode - 1) - progress`, falling back to total episodes if no next airing exists, and displays `R.string.airing_episode_in` when `dynamicTimeUntilAiring` and `nextAiringEpisode` exist.
- List `LibraryListCard` has the same behind badge and `Episode X in ...` display logic.
- These are both visible text and sorting/logical dependencies on AniList airing data; V1 must replace all target Watching uses with effective AniWorld release data and must not use AniList as fallback.

## Settings Structure
- `AppSettings` is a singleton backed by `SharedPreferences` file `anisync_settings`, exposing many `StateFlow`s. It also imports DataStore preference keys only for Glance widget state updates; app settings are not generally DataStore-based.
- Settings hub is `SettingsScreen` with card-based categories and search. Current order in the hub: Look & Feel, AniList, Notifications, Storage, Media Upload, App Links, Updates, Sponsors, About, and Developer Tools when unlocked.
- `SettingsCategory` is declared in `SettingsListDetail.kt`, not in a standalone `SettingsCategory.kt`. The AniSync Plus category belongs immediately after App Links and before Updates.

## Room / Local Database
- Current Room DB is `AppDatabase` version 22 named `anisync.db` in `DatabaseModule`.
- Entities include `library_entries`, `media_details`, `user_profile`, `airing_schedule`, `trending_media`, and `saved_forum_threads`.
- `DatabaseModule` provides singleton `AppDatabase` and DAOs and currently calls `fallbackToDestructiveMigration(dropAllTables = true)` with a development warning.
- A second Room database is technically feasible: add a separate `@Database`, Hilt provider, DAOs, and KSP schema. This avoids modifying upstream `AppDatabase` and its migrations.

## Branding / Parallel Install Audit
- Current base `applicationId` and namespace: `com.anisync.android`.
- Debug suffix `.debug`; preview suffix `.preview`.
- App names are Gradle `resValue` strings: AniSync, AniSync Debug, AniSync Preview.
- Future parallel install requires changing base application id, app name, APK rename appName, and checking manifest provider authorities/deep links/OAuth callback routes. Do not rename Kotlin packages unless technically required.

## AniWorld DOM Findings
See `docs/aniworld-parser-contract.md` for the detailed DOM contract. Key verified markers: day sections `section.calendarList`, title `h3.seriesTitle`, compact `S01E03`, time `~ 15:40 Uhr`, German sub flag `/public/img/japanese-german.svg`, German dub flag `/public/img/german.svg`, English sub flag `/public/img/japanese-english.svg`. No explicit timezone marker was found.

## Required Repository Search Findings
The requested search terms were run with `rg`. Relevant findings:
- `airingAt`: AniList schedule query/mapping, Room `AiringScheduleEntity`, notification workers, widgets, schema files.
- `timeUntilAiring`: library entity/domain/mappers and `LibraryRepositoryImpl`; used as stale fallback when no absolute next time is available.
- `nextAiringEpisode` / `nextAiringEpisodeTime`: GraphQL fragments/user library/media details, Room entities/mappers, details UI, library cards, library sorting.
- `AIRING_SOON` / `Airing Soon`: `LibraryUiState` enum and strings; sort implemented in `LibraryViewModel`.
- `followingOnly` / `isOnList`: calendar state and ViewModel filter, `CalendarRepositoryImpl` AniList mapping.
- `CURRENT` / `MediaListStatus` / `LibraryStatus`: list status mapping, library filters, add-to-watching, notifications, profile/library repositories.
- `DateFormat` / `DateTimeFormatter`: notifications and calendar/detail formatting utilities; calendar uses `java.time` and system zone.
- `CalendarRepository`, `CalendarViewModel`, `LibraryMediaCard`, `LibraryListCard`, `SettingsCategory`, `AppSettings`, `applicationId`, `namespace`, `app_name`: all located and mapped in this analysis.

## CI / Tests
- Existing tests are Android/JUnit sources under `app/src/test` and `app/src/androidTest`. `MigrationTest.kt` is an instrumentation test whose actual migration assertions are commented templates, so it does not verify all migrations. No APK workflow currently runs unit tests, lint, or instrumentation tests.
- Later CI should run `./gradlew testDebugUnitTest`, `./gradlew lintDebug`, and `./gradlew assembleDebug`, adjusted for actual flavor task names if AGP exposes stableDebug variants. It must upload the universal debug APK as `AniSyncPlus-debug-apk` and must not call AniWorld.

## Recommended Implementation Phases
1. Approve Phase 1 docs and answer open questions.
2. Add `:anisyncplus-calendar` skeleton, parser contracts, local fixtures, parser/consolidation tests.
3. Add separate Room DB and repository snapshot cache tests.
4. Add title normalization/matching and resolver tests using existing library data.
5. Integrate calendar screen with effective AniWorld cache while preserving UI.
6. Integrate Watching library countdown/behind badge/Airing Soon sorting.
7. Add AniSync Plus settings category and diagnostics.
8. Apply branding/application id and GitHub Actions APK workflow.
9. Iterate from user screenshots.

## Superseding V1 Decisions
The implementation master prompt fixes the former open questions: application id `de.mrxxxxx.anisyncplus`, source timezone and grouping `Europe/Berlin`, category placement immediately before Updates, existing Material/theme placeholder visuals, and AniWorld enabled by default. See `ProjectContext.md` for the complete decision log.
