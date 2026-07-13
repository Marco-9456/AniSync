# Upstream Integration Points for AniSync Plus

This table records the implemented changes against the AniSync baseline. New feature code is kept in `:anisyncplus-calendar` or new app files where possible; original files were edited only at verified integration points.

| Original file/surface | Implemented change | Compatibility rationale |
| --- | --- | --- |
| `settings.gradle.kts` | Includes `:anisyncplus-calendar`. | One low-conflict module include; app remains the root product. |
| Root `build.gradle.kts`, version catalog | Enables Android library/KSP dependencies already aligned with app versions. | No parallel toolchain or fixed foreign Gradle version. |
| `app/build.gradle.kts` | App ID/labels/APK base, module and test dependencies, lint baseline, per-variant label values. | Kotlin namespace/packages remain unchanged; flavor structure is preserved. |
| `CalendarRepositoryImpl.kt` | Replaces internal AniList schedule fetch with a cache-only effective AniWorld adapter. | Existing `CalendarRepository` contract and single DI binding remain intact. |
| `AiringEpisode.kt` | Adds defaulted AniWorld source date/time, approximate, kind, language, navigation/time-presence fields. | Existing constructors remain source-compatible because additions have defaults. |
| `CalendarViewModel.kt` | Observes snapshot/sync metadata, invokes explicit refresh coordinator, persists filter policy, bounds navigation, groups by source day. | Existing actions/week/month state pattern retained. |
| `CalendarUiState.kt` | Adds refresh/sync/range/error metadata. | Additive immutable fields with defaults. |
| `CalendarScreen.kt` | Displays range/last refresh/active status/error Retry, disables out-of-range navigation, supplies one shared countdown ticker. | Existing compact and two-pane structure retained. |
| `AiringEpisodeCard.kt` | Shows source 24-hour time, approximate/language/kind, placeholder and safe navigation. | Reuses upstream Material/theme components; no binary asset. |
| `Search.graphql` | Adds synonyms to bounded title-candidate results. | Existing search operation retained; only one returned field added. |
| `LibraryEntry.kt` | Adds defaulted latest released episode and approximate marker. | Upstream persistence/entity schema is unchanged; fields are runtime projection data. |
| `LibraryViewModel.kt` | Combines shared resolver flows only for Anime/CURRENT, clears target fallback fields, fixes null-last sorting. | Manga and all non-CURRENT paths return unchanged upstream entries. |
| `LibraryMediaCard.kt` | Uses latest AniWorld released episode and absolute next time; removes per-card status coroutine. | Grid layout keeps its existing card/config contract and uses a screen-level ticker. |
| `LibraryListCard.kt` | Uses latest AniWorld episode, absolute next time, and approximate marker. | List layout and actions remain unchanged. |
| `LibraryScreen.kt` | Owns one minute ticker passed to grid and list cards. | No sorting or resolver work is performed by the ticker. |
| `Screen.kt`, `NavHost.kt` | Adds the AniSync Plus settings route. | Follows existing sealed route/navigation pattern. |
| `SettingsScreen.kt`, `SettingsListDetail.kt` | Registers AniSync Plus after App Links and before Updates in both layouts. | Existing categories and two-pane behavior remain intact. |
| `values*/strings.xml` | Adds/updates feature and refresh-status strings in supported locales. | No existing keys removed. |
| Existing APK workflows | Uses wrapper, persistent signing requirements, exact current APK selection and provenance names. | Tag/manual entry points preserved while unsafe temporary signing is removed. |
| `gradle.properties` | Raises local/CI daemon heap from 2 GiB to 4 GiB after observed APK packaging OOM. | No dependency/toolchain behavior changes. |
| `gradlew` | Executable bit restored. | Makes documented wrapper commands directly runnable. |

## New Isolated App Files

- `data/anisyncplus/AniSyncPlusSettings.kt`
- `data/anisyncplus/AniWorldAniListAdapters.kt`
- `di/AniWorldCalendarModule.kt`
- `presentation/settings/AniSyncPlusSettingsScreen.kt`
- `presentation/settings/AniSyncPlusSettingsViewModel.kt`
- adapter, calendar policy, and library projection tests

## Deliberately Unchanged Upstream Paths

- `AiringSchedule.graphql` and AniList airing fields remain for non-target widgets, notifications, details, and upstream compatibility.
- `LibraryRepositoryImpl`, `LibraryEntryEntity`, and `anisync.db` still ingest/store upstream data; Anime/CURRENT presentation overwrites target release fields from AniWorld and explicitly clears them when absent.
- Existing provider authorities already use `${applicationId}`, so no manifest provider edit was necessary.
- Existing custom `anisync` callback scheme and HTTPS App Links remain because changing them requires external AniList/app-link registration.
- Kotlin namespace/package `com.anisync.android` is unchanged.
- Widgets, notifications, manga, and non-CURRENT library statuses retain upstream behavior.

## Expected Merge Hotspots

Highest conflict risk remains the calendar screen/ViewModel/card, library ViewModel/cards/screen, settings hubs, and app Gradle configuration. New parser/cache/matcher/repository files have low upstream conflict risk because they live in the isolated module. The implementation commits keep documentation, core, app integration, branding/lint, CI, localization, library ticker, and final report as separate themes.
