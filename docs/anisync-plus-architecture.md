# AniSync Plus Target Architecture Proposal

## Recommended Module
Add `:anisyncplus-calendar` after approval. Do not implement in Phase 1.

Suggested packages:
- `api/`: stable interfaces consumed by `:app`.
- `data/network/`: one AniWorld calendar page client using existing OkHttp patterns where practical.
- `data/parser/`: Jsoup parser, language parser, consolidation, local fixtures for tests.
- `data/local/`: separate Room DB, entities, DAO, migrations/schema.
- `data/repository/`: cache-backed repository and refresh orchestration.
- `domain/model/`: release, language, matching status, sync state models.
- `domain/matcher/`: title normalization and AniList matching.
- `domain/resolver/`: effective release resolver for calendar and library.
- `settings/`: AniSync Plus settings store or feature settings interface.

## Benefits
- Keeps most new code outside upstream `:app`, improving upstream mergeability.
- Allows parser/matcher/local database unit tests without Compose/UI dependencies.
- Makes a single effective release resolver shareable by calendar and library.
- Avoids changing current `AppDatabase` schema for AniWorld cache.

## Costs / Risks
- Android library module setup adds Gradle and Hilt/KSP configuration.
- Hilt bindings across module boundaries require explicit `@Module` placement and exported interfaces.
- Room in a second module needs its own schema location and migrations; care is needed to avoid duplicate generated class/package conflicts.
- The module must not depend on `:app` presentation classes, or a dependency cycle will form.

## Dependency Direction
Preferred: `:app -> :anisyncplus-calendar`. The new module should expose domain/API interfaces and implementation bindings. It may depend on AndroidX Room, Hilt, OkHttp/Jsoup, coroutines, and carefully selected AniList domain DTO interfaces. If it needs full `LibraryEntry`, prefer passing small matcher input models from `:app` to avoid depending on app UI or generated GraphQL classes.

## Proposed Database
Database name: `anisync_plus_calendar.db`.

Tables:
1. `aniworld_release_entries`
   - local stable key, snapshot id/version, AniWorld title, normalized title, slug/link, source link, source date/time text, calendar date, source local time, source timezone, optional instant, season, episode, language booleans/internal flags, approximate flag, AniList media id nullable, matching status, fetchedAt, parserVersion.
2. `aniworld_media_mappings`
   - normalized AniWorld title/slug, AniList media id nullable, status, confidence/reason, manually confirmed flag reserved for future, timestamps.
3. `aniworld_sync_state`
   - last attempt/success/error, parsed/matched/unmatched/ambiguous counts, document hash, parser version, stored range start/end.

Snapshot replacement should run in one Room transaction. Never delete the previous snapshot until the new parse validates as non-empty or intentionally empty by a documented rule.

## Repository Interfaces (Proposed)
- `AniWorldCalendarRepository.observeCalendar(): Flow<AniWorldCalendarSnapshot>`
- `AniWorldCalendarRepository.refreshCalendar(): Flow<RefreshProgress>` or suspend result with rich error details.
- `EffectiveReleaseRepository.observeCalendar(filter: CalendarFilter): Flow<List<EffectiveCalendarDay>>`
- `EffectiveReleaseRepository.observeNextGermanRelease(mediaIds: Set<Int>): Flow<Map<Int, EffectiveRelease>>`
- `LibraryReleaseResolver.resolve(entry, now): EffectiveLibraryRelease?`

## Integration Strategy
1. Add module and parser/local tests only.
2. Add Hilt bindings and repository API without changing visible UI behavior.
3. Switch calendar data source behind a minimal adapter; preserve existing screen layout and navigation.
4. Switch Watching library release text and Airing Soon sort to the resolver.
5. Add settings category and debug diagnostics.
6. Apply branding/application id/CI APK artifact in separate commits.

## Test Matrix
- Parser fixtures for DE-Sub, DE-Dub, same-time merge, different-time split, multiple episodes same time, reference case, unknown language, missing time/episode, empty DOM.
- Title normalizer/matcher unit tests for punctuation, apostrophes, unicode, season notation, ambiguity.
- Room DAO transaction tests for snapshot replace and failed refresh preservation.
- Resolver tests for no cache/no match/no future German release and earliest German release selection.
- Library sorting tests for AniWorld Airing Soon ordering and untimed entries after timed entries.
- Existing app checks: `testDebugUnitTest`, `lintDebug`, `assembleDebug` when environment allows.
