# AniSync Plus Project Context

## Project Goal

AniSync Plus is an upstream-compatible fork of AniSync that adds a modular AniWorld calendar provider for release-related anime data. The GitHub repository was verified as public on 2026-07-13; visibility remains an external repository setting.

## Implemented V1 Scope

- The existing calendar and Anime/CURRENT Watching library use the shared AniWorld effective-release resolver for release day, time, episode/kind, countdown, behind badge, and Airing Soon ordering.
- AniList remains authoritative for identity, titles, synonyms, cover, score, active account, list status, normal progress, and detail navigation.
- AniWorld data is fetched only by an explicit runtime refresh on the device. Startup, week/month navigation, tests, and CI read local data only.
- The cache is a separate Room database, `anisync_plus_calendar.db`, owned by `:anisyncplus-calendar`.
- App identity is `de.mrxxxxx.anisyncplus`; Kotlin namespace and packages remain `com.anisync.android`.
- App labels are `AniSync Plus`, `AniSync Plus Debug`, and `AniSync Plus Preview`; APK names start with `AniSyncPlus`.

## Non-Goals

No MAL support, manga release replacement, widgets/notifications/background AniWorld refresh, cloud/server scraping, streaming/video/player/hoster work, GitHub Actions scraping, authentication/bypass behavior, manual title linking UI, redesign, or broad AniSync restructuring.

## Upstream Baseline

- Implementation start: `1c69ce5ce5ed6fd6c4ebfdd213245c5dabc38e93`.
- Verified upstream merge-base: `8f1029db05b09dc612d3af061a01b0e0774d1531`.
- At implementation start the fork was 5 commits ahead and 0 behind `upstream/main`.
- `origin` points to `xxxxxxxxxxssxx/AniSyncPlus`; `upstream` points to `Marco-9456/AniSync`.
- Working branch: `codex/anisyncplus-complete-implementation`.

## Data Ownership

AniWorld owns visible release day/date/time, season/episode or film/special kind, German release language, approximate marker, calendar grouping/order, Watching countdown, latest released numerical episode, behind badge, and Watching Airing Soon sorting.

AniList owns media identity, title variants/synonyms, cover, score, account, list status, normal progress/actions, and detail navigation. AniList search is bounded to 10 anime candidates and runs only during manual AniWorld refresh. AniList airing data is not a fallback on target surfaces.

## Runtime and Cache Rules

- The calendar displays the active local snapshot immediately.
- Pull-to-refresh, Retry, and the AniSync Plus settings refresh action are the only V1 network triggers.
- HTTP, block-page, parse, validation, and database failures preserve the previous active snapshot.
- A valid structurally empty calendar can replace the previous snapshot; an invalid empty document cannot.
- Snapshot activation is transactional. Old releases are removed only after a valid replacement is ready; title mappings survive snapshot replacement.
- Matching follows accepted snapshot persistence. Matching/candidate failures are diagnostic and never destroy an accepted calendar snapshot.
- Re-fetching the same document updates its fetched timestamp and sync diagnostics without replacing releases.
- Sync state records attempt/success/error, HTTP status, range, parser/matcher versions, and parsed/matched/ambiguous/unmatched counts.

## Calendar Rules

- Visible languages are `DE-Sub`, `DE-Dub`, and `DE-Sub / DE-Dub`.
- Filter off shows every visible German AniWorld release, including unmatched releases.
- Filter on shows only safely matched releases whose active-account AniList status is `CURRENT`.
- AniWorld calendar is enabled by default. Filter memory is disabled by default; disabling memory clears the stored following-only value.
- Source dates/times use `Europe/Berlin`. Grouping uses the AniWorld source day, not a converted AniList airing day.
- Missing times remain untimed; approximate times keep their marker. Film, special, and unknown kinds never receive invented episode numbers.
- Covers and detail navigation are available only for safe matches; unmatched entries use a themed Material placeholder.
- Week/month views share one local snapshot flow and never independently fetch network data.
- Navigation is disabled when a week/month has no snapshot overlap. Partial overlap exposes only releases inside the snapshot range.
- The UI shows the available range, last successful refresh, active refresh state, and retained error with Retry.

## Watching Library Rules

- Only `MediaType.ANIME` entries in `LibraryStatus.CURRENT` receive AniWorld release fields.
- Manga and every other anime status remain on the unchanged upstream path.
- The earliest future timed German AniWorld release supplies countdown and Airing Soon time.
- The latest already released numerical AniWorld episode supplies the behind badge.
- Film/special/unknown releases do not create an episode behind badge.
- Missing AniWorld data explicitly clears target Anime/CURRENT AniList airing fields; there is no target fallback.
- Airing Soon always places null values last in both sort directions.
- Calendar and library observe the same `EffectiveReleaseRepository`; active-account state is supplied as a Flow.

## Matching Rules

Order: persisted safe/manual mapping, exact normalized title variant (user preferred, English, Romaji, native, synonym), season disambiguation, then cautious similarity. Similarity auto-match requires score >= 0.92 and margin >= 0.08; scores >= 0.85 without a decisive margin remain ambiguous. Status values are `MATCHED`, `AMBIGUOUS`, `UNMATCHED`, and reserved `MANUALLY_CONFIRMED`.

## Build, CI, and Branding

- PR/push CI uses the repository wrapper and runs module tests, `testStableDebugUnitTest`, `lintStableDebug`, and `assembleStableDebug`.
- CI removes prior outputs and selects exactly one universal APK from the current variant directory.
- Debug artifacts are marked `non-upgrade-channel` and include SHA/run provenance.
- Tag release and manual release builds require persistent signing secrets and fail hard without them. No temporary release key is generated.
- Release jobs print the keystore SHA-256 certificate fingerprint and use `github.token`.
- The upstream lint backlog is recorded in `app/lint-baseline.xml`; the gate fails on every new issue.

## External Constraints

- Package IDs and labels support parallel installation with upstream.
- The existing custom callback scheme `anisync` and existing HTTPS App Links are retained because the registered AniList client/callback is external. A separately registered callback/client would be required to guarantee deterministic OAuth routing when upstream and fork are installed together.
- No emulator/device was available for visual or installation testing. Robolectric covers database, settings, adapter, and policy behavior; the Stable-Debug APK was built and inspected with `aapt`.
- Persistent signing secrets were not available locally, so no signed Stable Release was produced. The required hard-fail behavior is encoded in release workflows.

## Decision Log

- 2026-07-13: Preserve upstream packages; change only application ID, labels, and APK base name.
- 2026-07-13: Use the separate `:anisyncplus-calendar` Android library and separate Room database.
- 2026-07-13: No visible or logical AniList airing fallback on calendar or Anime/CURRENT Watching.
- 2026-07-13: Treat `Europe/Berlin`, snapshot-bounded navigation, explicit manual refresh, valid-empty acceptance, and approximate-time semantics as fixed V1 policy.
- 2026-07-13: Preserve old snapshots through refresh failure; matching failure remains diagnostic after accepted persistence.
- 2026-07-13: Keep the upstream lint debt in a generated baseline while enforcing zero new lint issues.
