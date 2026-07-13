# AniSync Plus Calendar Architecture

This document describes the implementation on branch `codex/anisyncplus-complete-implementation` as verified through 2026-07-14.

## Module Boundary

`:app` depends on the Android library `:anisyncplus-calendar`; the calendar module never depends on app presentation, GraphQL-generated models, account storage, or upstream Room entities.

`:anisyncplus-calendar` owns:

- source/domain contracts and effective release models;
- the one-page OkHttp client for `https://aniworld.to/animekalender`;
- Jsoup parsing, Berlin time resolution, diagnostics, and consolidation;
- title normalization and conservative matching;
- a separate Room snapshot/mapping/sync database;
- atomic refresh orchestration;
- the shared effective snapshot, next-release, and latest-episode resolver flows.

`:app` owns:

- bounded AniList candidate lookup through the existing Apollo client;
- active-account library state through existing account/Room infrastructure;
- Hilt construction and bindings;
- settings, calendar presentation, and Watching-library projection;
- compatibility mapping into upstream `AiringEpisode` and `LibraryEntry` models.

This direction keeps new source parsing and persistence isolated while minimizing edits to upstream surfaces.

## Stable Contracts

The module exposes interfaces in `calendar/api/Contracts.kt`:

- `AniWorldCalendarClient.fetch()`
- `AniWorldCalendarParser.parse(html, fetchedAt)`
- `AniWorldCalendarRepository.observeSnapshot()`, `observeSyncState()`, and `refresh()`
- `AniWorldRefreshCoordinator.refresh()`
- `AniWorldTitleMatcher.match(release, candidates)`
- `AniWorldMatchCandidateProvider.candidatesFor(rawTitle)`
- `AniListLibraryStateProvider.observeStates()`
- `EffectiveReleaseRepository.observeSnapshot()`
- `EffectiveReleaseRepository.observeNextGermanReleases(mediaIds)`
- `EffectiveReleaseRepository.observeLatestReleasedGermanEpisodes(mediaIds, now)`

One `RoomAniWorldCalendarRepository` instance implements the calendar repository, refresh coordinator, and effective resolver, so calendar and library cannot diverge in release calculation.

## Refresh Sequence

1. Record a sync attempt in the separate database.
2. Fetch exactly the AniWorld calendar page with the dedicated OkHttp client; validate HTTP status, content type, bounded size, and non-empty body.
3. Parse the HTML off the main thread; when required calendar structure is incomplete, classify only strong challenge structures.
4. Validate day sections, range, SHA-256, parser version, snapshot IDs, release range, and unique local IDs.
5. Atomically activate a new snapshot and releases, or update `fetchedAt` for an unchanged document.
6. Resolve new/unconfirmed source keys through stored mappings and bounded AniList candidates.
7. Persist mappings and aggregate matching diagnostics.
8. Emit the effective snapshot combined with active-account library status.

HTTP, parser, validation, and database failures return `Failure`, record bounded diagnostics where possible, and leave the active snapshot untouched. A matching failure occurs after accepted snapshot persistence; it records `MatchingError` but returns refresh success because valid AniWorld data remains usable as unmatched content.

A structurally valid document with day sections and zero releases is accepted. A page without the required calendar structure is rejected.

## Database

Database file: `anisync_plus_calendar.db`, schema version 1.

| Table | Purpose |
| --- | --- |
| `aniworld_snapshots` | Active document identity, fetch time, range, hash, parser version, section count |
| `aniworld_release_entries` | Parsed source release facts and diagnostics keyed by snapshot/local ID |
| `aniworld_media_mappings` | Persistent source-series to AniList match state and candidate diagnostics |
| `aniworld_sync_state` | Attempt/success/error, HTTP status, counts, versions, active snapshot and range |

Room transaction `activateSnapshot` inserts the validated replacement, inserts its releases, changes active sync state, and removes stale snapshot rows. Mapping rows are outside snapshot ownership and survive replacement. DAO queries use indices on snapshot/source/date/instant/mapping fields recorded in the exported schema.

## Parsing and Time

- Required container: `#seriesContainer`; required day nodes: `section.calendarList`.
- Complete required calendar structure takes precedence over generic security words and ordinary CDN URLs. Strong challenge titles, forms/widgets, scripts, Ray IDs, or human-verification text are classified only when required structure is incomplete.
- Each `h3.seriesTitle` anchors one source card.
- Dates are parsed from German headings as `dd.MM.uuuu`.
- Time accepts optional `~` plus `HH:mm Uhr`.
- Episode accepts `SxxExx`; film and special tokens remain distinct release kinds.
- Only explicit image/source/alt/title markers classify language.
- DE-Sub and DE-Dub consolidate only when source identity, day, time, kind, season, and installment are equal.
- The source zone is the fixed product policy `Europe/Berlin`.
- DST gaps fail parsing; overlaps choose the earlier valid offset and retain a diagnostic.
- Missing time remains null. No instant or countdown is invented.

The detailed observable DOM contract is in `docs/aniworld-parser-contract.md`. Tests use only checked-in HTML fixtures.

## Matching

The normalizer lowercases with `Locale.ROOT`, decomposes Unicode, removes combining marks/punctuation, normalizes apostrophes/dashes, and collapses whitespace. Candidate title variants include user-preferred, English, Romaji, native, and synonyms.

A unique exact normalized title is accepted, with season used to disambiguate duplicate exact titles. Similarity uses normalized Levenshtein distance with:

- automatic match threshold 0.92;
- ambiguity threshold 0.85;
- minimum best/second-best margin 0.08;
- season bonus 0.02 and mismatch penalty 0.12.

Persisted MATCHED or manually confirmed mappings take precedence. Ambiguous and unmatched entries never receive AniList identity, cover, or navigation.

## Calendar Projection

`CalendarRepositoryImpl` remains the single upstream `CalendarRepository` binding, but now maps only the local effective AniWorld snapshot. It performs no network request and returns an empty success when disabled, no snapshot exists, or the request has no overlap. It filters to the intersection of request and snapshot range.

The ViewModel:

- observes cache and sync flows on startup;
- invokes the refresh coordinator only for explicit Refresh/Retry actions;
- groups by AniWorld source epoch day in Berlin;
- applies following-only to effective releases marked CURRENT;
- persists filter state only when configured;
- bounds week/month actions to snapshot overlap;
- shares one minute ticker from the screen for all visible countdown cards.

## Watching Projection

`LibraryViewModel` observes the same effective repository. The pure projection updates only Anime/CURRENT entries:

- next episode/time/approximate from earliest future timed German release;
- latest released numerical episode for the behind badge;
- explicit nulls when AniWorld data is unavailable, preventing AniList fallback.

Manga and non-CURRENT entries are returned unchanged. Airing Soon compares AniWorld timestamps and forces nulls last in both directions.

## Dependency Injection and Settings

`AniWorldCalendarModule` provides the separate Room database/DAO, dedicated source client, parser, matcher, repository, and all three public repository interfaces. App adapters bridge Apollo and active-account Room state. Stable-Debug Hilt compilation proves there is one upstream `CalendarRepository` binding and no duplicate AniWorld binding. Privacy-bounded request and refresh diagnostics use the `AniWorldCalendar` log tag; they omit response bodies, cookies, URL query values, and tokens.

`AniSyncPlusSettings` uses its own `anisync_plus_settings` SharedPreferences namespace. The settings category is registered after App Links and before Updates in compact and two-pane navigation and exposes enable/filter-memory toggles, manual refresh, range/status/error/count diagnostics, timezone, and parser/matcher versions.

## Test Strategy

- 41 module tests: parser fixtures, client behavior and diagnostic privacy, normalization/matching, Room transactions, refresh preservation, resolver and account-flow behavior.
- 184 app tests: existing suite plus adapter, filter/persistence/range, library targeting/no-fallback, and sorting tests.
- Robolectric substitutes for unavailable device instrumentation for database/settings/policy paths.
- CI and local gates never make live AniWorld requests.
