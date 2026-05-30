# Changelog

All notable changes to AniSync will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

## [1.8.0] - 2026-05-30

### Added

- **Weekly Airing Calendar** - New in-app screen showing the week's airing schedule, with a pill-style day selector, a "today" marker, and a jump-to-today chip.
- **In-App Sponsors Screen** - Dedicated Sponsors screen whose list is synced from GitHub Sponsors via CI.
- **Avatar Shape Customization** - Choose an avatar shape (Clover-8 leaf and others) and toggle a global profile background. A shared `UserAvatar` renders consistently across review cards, profile, and other avatar sites.
- **Library Pull-to-Refresh** - Pull-to-refresh on the Library tabs.
- **More Languages** - Added Spanish and Portuguese (Portugal) translations and refreshed Arabic, German, and Portuguese (Brazil) via Weblate.

### Changed

- **Rate-Limit Resilience** - Added a stale-while-revalidate `ResourceFetcher` primitive, cut profile/search request volume to stop rate-limit stalls, and surfaced rate-limit feedback through the toast system.
- **Detail-Push Enter Animation** - Detail screens pushed onto the stack now animate in.
- **About Screen Refresh** - Reworked About screen and open-source acknowledgements list.
- **Hide Developer Tools** - Developer Tools can be disabled/hidden from settings.

### Fixed

- FAB now stays above content during the media-details enter transition and above the bottom nav bar on Feed and Forum.
- Profile keeps its scroll position when navigating back.
- Custom lists exclude `status:*` entries; studio cover sizes are uniform.
- Clover-8 item-icon shape is preserved when the avatar shape is set to None.
- In-app notification taps route through the `NavController` instead of restarting the activity.
- Voice-actor count badge no longer clips on character screens.
- The in-app App Language sheet now lists every bundled language; it previously only offered English, German, and Arabic.

## [1.7.0] - 2026-05-15

### Added

#### Editing & Composition
- **Edit + Delete Across Posts** - Threads, thread comments, activity replies, and TextActivity status posts gain edit + delete actions, gated by authorship; delete confirms via `AlertDialog`. Backed by `SaveThread` / `SaveThreadComment` / `SaveActivityReply` / `SaveTextActivity` id-aware mutations + `DeleteThread` / `DeleteThreadComment` ops. Closes #9.
- **DM Edit + Delete from Any Profile** - Overflow menu on direct messages no longer gated by `isOwnProfile`. `canDelete` / `canEdit` derive from authorship; mod-authored messages hide delete to prevent 401 logout. `ActivityRepository.saveMessageActivity(id=...)` added. `ActivityFields` / `GetActivity` fetch `textRaw` / `rawMessage` so edits prefill with markdown.
- **RichText Bio Editor** - Profile bio editor swapped from `AlertDialog` to `RichTextInputScreen`; `GetUserProfile` now fetches `aboutRaw` so the editor round-trips markdown rather than corrupting via the rendered HTML.
- **Media Upload in Composers** - New `MediaAttachSheet` uploads images, GIFs, and videos to a configurable host (Catbox / Litterbox 1h/24h/72h / custom multipart) and inserts the AniList `img%(url)` markdown at the cursor. `MediaSizeChoice` (small / medium / large / custom px or %) drives the inserted size. Dedicated 10-minute-timeout OkHttpClient streams from content URIs via `UriRequestBody` so large files don't OOM. Closes #6.
- **IME Sticker / GIF Insert** - `RichTextScaffold` and `RichTextInputSheet` accept stickers and GIFs straight from Gboard via `contentReceiver`; uploads run in the background and surface a compact `ImeUploadStrip` (filename + animated progress + cancel) above the format bar.
- **Discard Draft Confirmation** - Composers warn before discarding unsaved drafts via `AlertDialog`; sheets are partially-expandable.
- **Forum Media Categories** - The thread compose sheet now attaches `mediaCategories` on `SaveThread` instead of inlining a markdown link; selected media render as removable chips next to forum category chips, and re-tapping in the search list removes.

#### Library & Navigation
- **Drag-to-Reorder Library Tabs** - All Library tabs reorderable with per-tab visibility toggling; visibility persists across sessions. Closes #10.
- **Per-Type Last-Tab Memory** - Library remembers the last selected tab per media type (Anime / Manga independently) and restores it on reopen. Falls back to the first visible tab if the saved tab was deleted/hidden. Closes #11.
- **Compact Nav Bar** - New `CompactNavBar` replaces M3 `NavigationBar`: floating-pill or anchored-rounded-top styles, user-toggleable label visibility with height animation, and corner-radius slider (0-36 dp). Floating-bar mode renders as a content overlay so scrollable content shows through its side margins; tab screens consume `LocalMainNavBarInset` as bottom contentPadding. `navigateSafely()` absorbs rapid taps during transitions. Parallax + scale push/pop transitions for detail screens. Closes #20.
- **Collapsing Hero Top Bar App-Wide** - Extracted `CollapsingTopBarScaffold` applied to `ReviewDetail`, `ForumCategory`, `Notifications`, `ActivityDetail`, `ThreadDetail`, `GridsScreen`, `SectionGridScreen`. Synchronous nested-scroll height updates eliminate the bar/content padding desync; collapsed title vertically aligns with action icons; expanded state stops consuming the gesture so inner `PullToRefreshBox` can still trigger refresh.

#### Search & Discover
- **Advanced Search Rebuild** - Full AniList filter parity: unified chip bar + per-filter bottom sheets, Type chip (All / Anime / Manga / Characters / Staff / Users / Studios) that overrides the screen-level Anime/Manga selector, M3-spec year-range grid (1940..current+1) replacing the dual wheel, persisted list/grid view toggle, results header with category chip strip. `IncludeExcludeChip` restyled to M3 filter-chip spec. More filters consolidated into one sheet of inline-expanding groups with a Reset action. Adult genres gated behind a settings toggle. Closes #18.
- **Discover Hero Carousel Rewrite** - From-scratch MD3-polish rewrite of the trending hero carousel.

#### Toasts & Alerts
- **Global TopAlertToast System** - Toast-based alert system (`ToastManager`, `ToastMessage`, `ToastType`, `TopAlertToast`, `TopToastHost`) replaces every Material `Snackbar` site (Feed, Forum, Library, Profile, Settings, Forum subscreens). Swipe-to-dismiss, countdown timers for retry/rate-limit, theme-aware surfaces.
- **Error Code + Countdown on Result.Error** - `Result.Error` now carries HTTP status code and countdown seconds; `NetworkUtil.safeApiCall` populates them from `ApiError` and Apollo exception types so the UI can show contextual error toasts with retry timers.

#### Crash Handling
- **In-App Crash Reporter** - `CrashReportActivity` surfaces uncaught exceptions in a themed screen with Copy-and-Exit; runs in a separate `:crash` process so it survives main app death. `AppInfo` helper formats version / Android / ABI for both bug reports and crashes. About screen gains a "Copy app info" entry. GitHub bug/feature issue templates added.

#### Details
- **Selectable Text + Long-Press Copy** - Titles, synopsis, character/staff name card, attributes wrapped in `SelectionContainer` for partial-text selection. Long-press copies the full string from non-text-selectable items (cast tile, external-link chip, voiced-character row, voice-actor card). Per-content-type `ClipData` labels (Title, Synopsis, Character name, Staff name, External link) so the OS clipboard / Gboard chip shows useful provenance. Closes #8.

#### Settings & Look-and-Feel
- **Cover Image Quality Setting** - Look-and-Feel setting (Extra Large / Large / Medium) applies app-wide without restart. `LocalCoverQuality` `CompositionLocal` flips at the root from `AppSettings.coverQuality`; library and media-details Room entities gain `coverMedium` / `coverLarge` / `coverExtraLarge` columns (auto-migration v12 -> v13).
- **Expressive Settings UI** - Modernized cards and improved search; missing search strings (`search_settings`, `clear_search`, `no_settings_found`) added.
- **Developer Tools Unlock** - Tap the About-screen version label 7x to permanently unlock Developer Tools in release builds; crash-trigger group still gated behind `BuildConfig.DEBUG`.

#### Studio & People
- **Studio Detail Screen** - New `StudioDetailsScreen` with hero, attributes, works list, fav toggle, share. VA / staff / studio stat cards clickable across Statistics, Search, Profile Favorites, MediaDetails. Room schema bump v13 -> v14 (`studioId` column on `media_details`).
- **Stats Cards Redesign** - VoiceActor / Staff cards adopt a tombstone-arch image with uppercase bold name; Studio card text-only with tertiary gradient and fixed height to stop `LazyRow` jitter; release-year histogram and donut chart shrunk so counts/years no longer wrap.

#### Localization
- **Weblate Integration** - Every UI string externalized to `strings.xml`; `app_name` references switched to placeholders to prevent translation; Weblate workflow added.

#### Branding & Typography
- **New App Icon** - Updated launcher icon, monochrome variant, notification icon, and F-Droid metadata. Resolves #12.
- **Google Sans Flex Typography** - Centralized variable-font system swaps Roboto Flex for Google Sans Flex; M3 Expressive scale; ROND 100 (fully rounded) is the new app-wide default. Glance widget layer migrated to shared role tokens.
- **Per-Category Font Axis Playground** - Developer tool: weight / width / optical-size / slant / roundness per M3 role (Display / Headline / Title / Body / Label) with an "All" shortcut. Persisted as a JSON blob in `AppSettings`.
- **Wavy Progress Indicators** - Material 3 wavy circular / linear progress variants across upload, refresh, and other progress surfaces.

### Changed
- **Unified RichText Composer Family** - Consolidated `MarkdownComposeSheet`, `MarkdownFullEditor`, `MessageComposerSheet`, and `CreateThreadScreen` into `RichText{Scaffold,InputSheet,InputScreen,FormatBar,Ops}` under `presentation/components/richtext/`. `RichTextInsertController` exposes a cursor-aware insert handle. Migrated to `TextFieldState` + `BasicTextField` for in-place mutations via `TextFieldState.edit`.
- **Composer IME Docking** - Toolbar docks to the IME via `imePadding()` (mirrored in `RichTextInputSheet` via `navigationBarsPadding() + imePadding()`); each modifier consumes only its own inset.
- **Input Max Length** - Title and body fields enforce AniList max length via `MaxLengthInputTransformation`; `canSubmit` gates on `withinMaxLength`; `ForumThreadInputScreen` title clamped via `take(TitleBounds.max)`.
- **Litterbox Duration Selector** - `LITTERBOX_1H/24H/72H` consolidated into a single `LITTERBOX` host with an expiry duration setting. Imgur uploader removed.
- **Profile Header Polish** - `RainbowDonatorBadge` / `RegularDonatorBadge` extracted to isolate animation invalidation; `joinedDate` and gradient brushes cached via `remember`; profile action buttons switched to `IconButton` variants.
- **Notifications Filter Snapshots** - `NotificationsViewModel` caches per-filter snapshots so chip switches are instant. Worker passes `typeIn` so it actually receives the configured notification types.
- **Toast Performance** - Pre-calculated `surfaceColor` / `codeBackgroundColor` on `ToastType`; `codeToTypeMap` for O(1) `fromCode`; `CountdownTimerText` extracted so only the timer recomposes; swipe animation defers state reads to draw via `graphicsLayer`; `AtomicInteger` IDs replace `UUID`; `collectAsStateWithLifecycle` for safer state collection.
- **Memory Footprint** - Coil bitmaps enforced as `RGB_565` (halves cover-art memory); ExoPlayer instances use a strict `DefaultLoadControl` capping per-video buffer at 2 MB (down from 32 MB); `CoverImage` strings deduplicated and marked `@Immutable`; `ExoPlayer.prepare()` deferred until the player attaches to the UI.
- **Main Thread Offload** - Notification scheduling and update checks moved off the main thread to `Dispatchers.IO`; `MainActivity` refactored to extract update handling into `AppUpdateHandler`; auth token parsing optimized.
- **Upload Progress Smoothing** - `UriRequestBody` chunk bumped to 256 KB with a 50 ms minimum gap between `onProgress` fires plus a guaranteed final fire on EOF; response reads bounded via `peekBody` (4 KB for URL responses, 64 KB for custom-host JSON) so a misconfigured host can't OOM the parser; `retryOnConnectionFailure` disabled on the upload client.
- **`Menu` Primitive** - M3 Expressive dropdown menu wrapper with item / divider / gap DSL covering segmented shapes, group containers, partial-width dividers, refined motion. Adopted across CharacterDetails sort field, activity-card overflow, PaletteStyle selector.

### Fixed
- **Emoji Truncation in Posts** - 4-byte UTF-8 codepoints silently truncated posts in AniList's utf8 (3-byte) MySQL column. `AniListTextEncoder.encodeForAniList` now replaces every codepoint above U+FFFF with its decimal HTML entity (e.g. `🤔` -> `&#129300;`) at the wire boundary across all seven mutation sites (forum body/title/comments, activity text/messages, profile messages); composer keeps showing raw emoji.
- **Catbox Upload Reliability** - HTTP/2 edge intermittently RST'd upload streams ("stream was reset: PROTOCOL_ERROR") and the default OkHttp UA tripped Catbox's bot filter → 412 "Invalid uploader". Pinned `Protocol.HTTP_1_1` and `AniSync/<ver> (Android <sdk>)` UA on the shared media-upload OkHttpClient; residual 412 errors map to a "switch to Litterbox" hint instead of the raw response.
- **Auth Storage Recovery** - `EncryptedSharedPreferences` AEADBadTagException (corrupt keystore on some OEMs) used to lock the user out; auth flow now detects it and recovers cleanly.
- **Deleted-Activity Deep Links** - Opening a deleted activity via notification deep link no longer hits a dead-end error screen. `ActivityRepositoryImpl` throws `ApiError.GraphQLError(404)`; `ActivityDetailViewModel` detects 404 in `load()`, shows a `TopAlertToast`, and pops via `finishedEvents`.
- **Mod-Authored DM Delete** - Deleting a moderator's message activity used to trigger a 401 logout. `ActivityFields` and `GetActivity` now fetch `moderatorRoles` on the messenger; `isAuthorMod` mapped onto `UserActivity` / `ActivityDetail`; `canDelete` guarded across `ActivityDetailScreen` / `ProfileActivitySection` / `RecentUpdatesSection`. `AuthorizationInterceptor` also refactored to extract operation name from the request body (Apollo 4.x) and detect permission errors in HTTP 200 GraphQL responses.
- **Duplicate Forum Threads** - `ForumViewModel` deduplicates threads by id when appending pages.
- **Rich-Text Image Width Crash** - Malformed AniList markdown (e.g. `img<width>(url)` with an enormous width) overflowed Compose `Constraints` (~262143 px max) and crashed the layout pass. Absolute width clamped to `MAX_RICH_IMAGE_WIDTH_DP` (3000dp); percent width clamped to `0f..1f`.
- **Memory Caches Bounded + Glance Leak** - Bounded internal caches, fixed a Glance leak, added OOM crash protection.
- **Compact Nav Bar Label Clipping** - 20 dp rounded-corner clip wrapping the bottom-nav item Column nibbled bold-Discover and other wide labels; clip dropped (no ripple to contain). Nav labels and `AnimatedTab` get `maxLines = 1, softWrap = false, overflow = Ellipsis` so long locale strings ellipsize cleanly. Arabic `nav_profile` shortened (`الملف`); missing `nav_feed` entries added for `ar` (`النشاط`) and `de` (`Feed`).
- **Compact Nav Bar M3 Alignment** - Reserve label-height padding when labels hidden; active indicator pill to 32 dp tall; item padding 12 dp top / 16 dp bottom across both nav-bar styles.
- **Discover Bottom Inset** - Dropped leftover `bottom = 80.dp` hardcoded on `DiscoverContent`'s padding; the inner `LazyColumn` already reserves space via `LocalMainNavBarInset`. Added 24 dp on top of the inset so the last row gets breathing room above the floating pill.
- **Settings Search Strings + Shape Clip** - Added missing `search_settings` / `clear_search` / `no_settings_found` strings; `MaterialShapes.Clover8Leaf` converted via `toShape()` so it works with `clip()`.
- **Search Polish** - View-mode toggle collapsed to a single icon button; year filter rebuilt to M3 year-grid spec; adult genres gated; wheel-picker edges softened.
- **Toolbar Polish** - `STATUS` filter uses `AutoMirrored` icon; `TopAppBar` colors deprecation fixed; redundant `else` branch in `ThreadDetailViewModel` removed; `onBackPressed` deprecated on `CrashReportActivity`.

## [1.6.0] - 2026-05-01

### Added

#### Feed
- **Feed Tab** - New top-level Feed tab between Discover and Forum with a Global/Following scope toggle, All/Status/List filters, Anime/Manga sub-filter, and a FAB status composer backed by `SaveTextActivity`.
- **Pinned Activities** - Activity queries now sort by `[PINNED, ID_DESC]` so pinned text activities float to the top of feeds.

#### Notifications
- **Notifications Inbox** - Dedicated inbox screen reachable from the Profile bell, with All / Airing / Status / Messages / Forum / Follows / Media filters, like-grouping per target, and inline rich-text previews.
- **Unread Badge** - Profile bell and bottom-nav Profile destination share a single `NotificationBadgeStore` driven by `Viewer.unreadNotificationCount`; nav badge collapses to a small dot when Profile is the selected destination, counts above 999 render "999+" with a pluralized TalkBack label.
- **Streaming Availability Delay** - New 0-180 minute slider that holds back airing-episode alerts until `now >= createdAt + delay`, lining notifications up with when episodes are actually watchable on slower streaming sites.

#### Social Interactions
- **"Liked by" Sheet** - Tap any like-count on an activity, activity reply, forum thread, or forum comment to open a bottom sheet listing the users who liked it. New `GetActivityLikes`, `GetActivityReplyLikes`, `GetThreadLikes`, and `GetThreadCommentLikes` queries back the four target types.
- **Tap-to-Like and Delete on Activity Cards** - Like and comment counts on feed/profile activity cards become 48dp pill buttons; tapping the heart toggles the like with optimistic flip, and an owner-only 3-dot overflow exposes Delete behind an `AlertDialog`. `ToggleActivityLike` now covers `ListActivity` (previously only `TextActivity` / `MessageActivity` were handled).
- **Following Section on Media Details** - New horizontal section listing the viewer's followed users' status, score, and progress for the current media (via `Page.mediaList(isFollowing: true)`), with color-coded status pills and a "See All" pagination sheet. Hidden when nobody followed has the media on their list.

#### Forum
- **Bidirectional Comment Paging** - `ForumRepository.findCommentPage` runs a client-side binary search over the page space (with an in-memory cache) so deep-linked comments anchor to the page that actually contains them. UI tracks `loadedPageRange` / `lastPage` / `totalComments` and exposes Load earlier (prepend), Load more (append), and Jump-to-page actions via a new `PageProgressPill` and `PageJumperBottomSheet`.
- **Client-Side Comment Sort** - AniList ignores `Page.threadComments(sort:)`, so Newest/Oldest is now applied in the repository (always fetch ascending, reverse page order and within-page rows for Newest).
- **Last-Reply Pill Targets** - "Last by USERNAME" pills on forum threads and activity cards now open the thread/activity scrolled to the latest reply via `replyCommentId` / `lastReplyId` rather than navigating to the replier's profile.

#### Tooling
- **Manual Release Workflow** - Added a manually triggered signed-release APK GitHub Actions workflow that uploads each APK as a separate versioned artifact.

### Changed
- **Activity Card Redesign** - `RecentUpdateCard` rebuilt with cover-left layout, author header, annotated status text, and a footer with last-reply pill plus reply/like stats. `ActivityPreviewCard` and `ForumThreadCard` share a new `AuthorRow` and `StatBadge` for reuse in `ProfileSocialSection`. `ListActivity` GraphQL fragment now selects `user`, so MEDIA_LIST entries carry author info.
- **Profile Refresh Parallelism** - Viewer-name lookup is skipped when the cache already has a username, and the activities query runs in parallel with favourites pagination, cutting pull-to-refresh latency.
- **Data-Layer Performance Overhaul** - `java.util.Calendar` replaced with `java.time` (5-10x faster date mapping); `LibraryDao` "smart merge" via single-pass partition + batched SQLite deletes; cached `ZoneId.systemDefault()` and `ThreadSort` lookups; reduced network round-trips in `LibraryRepository` via reused `Viewer` data and `CacheAndNetwork` policies.
- **Parser Performance** - Hoisted/compiled regex constants in `RichTextNormalizer`, `RichTextHtmlParser`, and `CharacterDescriptionParser`; chained `String.replace` calls replaced with single-pass `StringBuilder` rewriter for HTML entities and setext underlines (O(n) vs O(n²)); manual integer-based `formatScore` fast path bypasses `String.format` locale lookups.
- **Presentation Performance** - `LibraryViewModel` pre-caches sort titles and uses a direction-aware comparator to avoid full-list reversals; custom-list membership tests and sort lookups switched to `HashSet`/`HashMap` for O(1) lookups; hand-rolled `stripHtml` scan replaces regex-based stripping.
- **Markdown Composer** - New shared `MarkdownComposeSheet` replaces `ReplyBottomSheetContent` across Feed, Forum threads, and Activity detail; uses `imePadding` + `verticalScroll` so the keyboard no longer hides the input.
- **Forum Card Surface** - `ForumThreadCard` container bumped to `surfaceContainerLow` for clearer separation; `distinctBy` on thread id when concatenating paginated results fixes the "Key already used" `LazyColumn` crash.
- **Dependency Bumps** - Compose BOM (2026.03.01 -> 2026.04.01), Navigation Compose (2.9.7 -> 2.9.8), Material 3 (1.5.0-alpha17 -> alpha18), kotlinx.serialization (1.10.0 -> 1.11.0), JSoup (1.22.1 -> 1.22.2); Compose animation/runtime graduated 1.11.0-rc01 -> 1.11.0.
- **README & Screenshots** - Refreshed README with new screenshots, F-Droid version badge, and `flat`-style shields; replaced `fastlane phoneScreenshots` with the new set.

### Fixed
- **Favourite Toggle Snap-Back** - `MediaDetails` favourite heart now flips optimistically on tap and trusts the `ToggleFavourite` mutation result; the previous attempt derived state from paged `MediaConnection` membership, which mis-reported "not favourited" when the viewer had more than 25 favourites of that media type. Mutation success is now the authority; UI rolls back only on error.
- **Activity Like on Other Profiles** - Tap-to-like was gated behind an own-profile check; ungated since like is a viewer-level action against any activity (delete remains owner-only).
- **Search Overlay Navigation Loop** - Discover, Library, and user-result search Popups stayed on top of detail destinations on Android 16 (predictive back, API 36), causing tap/back events to re-push the route in a loop. Search bar state now collapses before navigation across all three entry points; `launchSingleTop` added to detail navigation.
- **Debug-Bumped Unread Count** - Server-side and debug-only unread counts now tracked in separate slots; profile resume only refreshes the server slot, so a debug bump survives until the inbox sends `resetNotificationCount=true`.
- **Inbox Subtitle Rendering** - Notification subtitles strip HTML to a plaintext two-line excerpt; embedded videos and AniList media-link cards no longer overflow the card, and empty/whitespace bodies collapse cleanly.
- **List Activity Verb** - `ListActivity` now reads `ANIME_LIST` / `MANGA_LIST` from the activity's own type field, so the verb no longer says "anime list update" when the content is a manga read.
- **Animated Avatars** - User avatars in the Following section, "Liked by" sheet, and elsewhere use `avatar.large` with `allowHardware(false)` to keep animated GIF/WebP avatars from freezing on the first frame.
- **Activity Detail for List Updates** - `GetActivity` now handles `ListActivity`, so card taps no longer hit "Unsupported activity type".

## [1.5.4] - 2026-04-24

### Changed
- Disable AGP-injected `dependenciesInfo` block (`includeInApk = false`, `includeInBundle = false`) so the upstream-signed APK on GitHub Releases passes F-Droid's reproducible-build scanner. The block embeds Play Store dependency-reporting metadata that F-Droid's scanner flags as an extra signing block.

## [1.5.3] - 2026-04-23

### Changed
- Downgrade `compileSdk` and `targetSdk` from 37 to 36 so the F-Droid buildserver can build the app. SDK Platform 37 is still in preview and is not yet in F-Droid's signed transparency log; AGP installs it to `android-37.0` but R8 looks up `android-37`, which fails. Will re-bump once SDK 37 is final.

## [1.5.2] - 2026-04-23

### Changed
- Remove `org.gradle.toolchains.foojay-resolver-convention` plugin from `settings.gradle.kts` to satisfy the F-Droid build scanner (no `jvmToolchain` calls used the resolver).

## [1.5.1] - 2026-04-22

### Added
- Fastlane metadata for F-Droid submission (title, descriptions, icon, screenshots, changelogs).

### Changed
- Release builds disable VCS info embedding for reproducibility on F-Droid.
- Remove unused `androidx.ui-text-google-fonts` dependency.

### Security
- Add `keystore.properties.example` to document the signing config without exposing credentials.

## [1.5.0] - 2026-04-20

### Added

#### Activity & Profile
- **Activity Detail Screen** - Added a dedicated activity detail route with richer activity content rendering.
- **Profile Messaging** - Added a message composer sheet for public profiles backed by AniList `SaveMessageActivity`.
- **Profile Tab Pagination** - Added pagination support for Social and Reviews tabs.
- **Profile Content Navigation** - Profile social items and reviews now open thread/comment and review detail surfaces directly.
- **Profile Pull-to-Refresh** - Added pull-to-refresh support and expanded localized profile strings.

#### Navigation & Notifications
- **Expanded Deep Link Coverage** - Added deep-link routing for user profiles, reviews, and forum comments from AniList URLs.
- **Social Activity Notifications** - Added social activity notification channel, settings toggles, and worker dispatch integration.

#### Widgets
- **Material 3 Widget Redesign** - Reworked all Glance widgets with modern card-based layouts.
- **Weekly Calendar Day Selection** - Added day-picker behavior and improved worker fetching for weekly widget data.
- **Up Next Streaming Links** - Added streaming link support in the airing schedule pipeline and Up Next widget surfaces.
- **Widget Preview Refresh** - Updated launcher/widget preview layouts to match the redesigned UI.

### Changed
- **Shared Element Coverage** - Extended shared elements across detail and grid flows and stabilized return transitions in Library/Discover.
- **Card Design Refresh** - Redesigned activity, forum, and related content cards for denser, clearer presentation.
- **Build Tooling Upgrade** - Migrated to AGP 9.1.0 with SDK 37 targets, updated KSP/Hilt toolchain, and restored AGP 9 compatible APK naming.
- **Database Schema** - Bumped Room database to version 12 with auto-migration (11 -> 12) for updated schedule data.

### Fixed
- **Rich Text Parsing** - Improved inline grouping and mixed markdown/HTML link handling in the parser.
- **Transition Stability** - Reduced shared-transition return glitches between Library/Discover and detail screens.

## [1.4.0] - 2026-04-11

### Added

#### Profile & Navigation
- **Profile System Redesign** - Expanded profile support from self-profile viewing to full public user profile support.
- **Cross-Surface User Navigation** - User entities are now tappable across Forum, Reviews, and Discover user results, and open the redesigned profile screen.
- **Public Profile Actions** - Added follow/unfollow and share actions for public profiles.
- **Profile Identity Metadata** - Added donator tier/badge, moderator roles, and account creation date in profile UI/data models.

#### Profile Content
- **Favorites Expansion** - Replaced cast-only flow with unified Favorites sections for Anime, Manga, Characters, Staff, and Studios.
- **Studios Favorites Support** - Added studio favorites through GraphQL, domain models, mappers, converters, and Room schema.
- **Status-Based Media Tabs** - Anime/Manga profile tabs now load real user lists by AniList statuses with refresh-aware caching behavior.

#### Forum
- **Deep Thread Drill-Down Navigation** - Added per-branch drill-down for deeply nested comment trees.
- **Ancestor Breadcrumb Strip** - Added folded ancestor navigation to move through focused deep-thread levels.
- **Adaptive Thread Depth Window** - Visible depth now adapts by device width while preserving drill-down state.

#### Rich Text & Parsing
- **Parser Architecture Refactor** - Split RichText parser into modular components (normalizer, HTML parser, inline parser, post-processor, models/context/utilities).
- **Parser Test Coverage** - Added dedicated parser regression test suite for markdown/HTML edge cases.
- **Typed AniList Link Preview Keys** - Migrated preview lookup to typed keys and expanded link preview fetching to include staff.

### Changed
- **Profile About Rendering** - Switched to richer HTML-based about retrieval/update flow.
- **Review Presentation** - Review cards now prefer media banners (when available) and expose user navigation interactions.
- **Statistics Score Handling** - Histogram display now aligns with AniList score format variants.
- **Database Schema** - Bumped Room database to version 11 with auto-migrations (9->10, 10->11) and schema exports.
- **SVG Media Pipeline** - Added SVG decoder support in image loading stack and related UI surfaces.
- **Localization Coverage** - Expanded string coverage in German and Arabic resources.

### Fixed
- **Profile Resume Reloading** - Prevented unnecessary profile refresh when app resumes.
- **Follow-State Consistency** - Improved correctness and refresh behavior for follow state in profile flows.
- **Markdown Recovery** - Fixed mangled markdown links and nested markdown image parsing issues.
- **Rich Text Line Breaks** - Preserved expected line breaks and tightened inline grouping behavior.
- **SVG Rendering Issues** - Fixed SVG icon/image rendering in details external-link chips and rich content contexts.
- **Layout Stability** - Reduced visual layout shifts during image/video/rich-content loading.

## [1.3.2] - 2026-04-06

### Fixed
- **Custom List Classification** - Library grouping now uses AniList `isCustomList` metadata (with a safe fallback) instead of name matching, preventing status lists like Rewatching/Rereading from being misclassified as custom lists.
- **Custom List Order Sync** - Custom list order now reconciles against API results even when the API list is empty, removing stale local ghost custom lists.

## [1.3.1] - 2026-04-06

### Fixed
- **Score Slider Color Mapping** - Normalized score color thresholds against each active score format (`POINT_3`, `POINT_5`, `POINT_10`, `POINT_10_DECIMAL`, `POINT_100`) so visual feedback now scales consistently.

## [1.3.0] - 2026-04-06

### Added

#### Library & Tracking
- **Notes and Custom Lists Editing** - Added notes and AniList custom list support across library and media details editing flows.
- **Score Format Awareness** - `EditLibraryEntrySheet` now receives and applies score format from the library context.

#### Discovery & Search
- **Enhanced Discover Search** - Improved Discover with better integrated search behavior.
- **SearchAll Integration** - Added `SearchAll` GraphQL query and unified search result modeling.

#### Media Details & Rich Content
- **Character & Staff Expansion** - Added and enhanced staff details plus richer character details with voice actor filtering.
- **Recommendations & Reviews UI** - Added recommendations and reviews sections with voting support.
- **Richer Link Cards** - AniList links now render with cover images and titles in rich text.

### Changed
- **Update Dialog Redesign** - Reworked update UX with a `ModalBottomSheet` implementation.
- **Details UI Refactors** - Simplified section structure and extracted reusable grid/item components.
- **Library Data Pipeline** - Extended GraphQL, data, and repository layers to carry notes and custom list metadata end-to-end.
- **Database Schema** - Added Room schema exports for database version 8.

### Fixed
- **Custom List Visibility Logic** - Standard lists now correctly hide `hiddenFromStatusLists` entries while preserving visibility in custom lists.
- **Entry Editing from Details FAB** - Fixed missing custom list context when opening `EditLibraryEntrySheet` from media details.
- **Staff/Character Navigation & Links** - Fixed deep links, visibility, sharing, and related navigation issues.
- **Rich Text Image Width Parsing** - Correctly handles AniList hash-prefixed width image syntax.

---

## [1.2.0] - 2026-03-30

### Added

#### Library & Custom Lists
- **Custom Lists Integration** - Full support for creating, syncing, and managing custom AniList library lists.
- **Type-Specific Preferences** - Separate list ordering and visibility preferences for your Anime and Manga lists.

#### Media Viewing
- **Immersive Image Viewer** - New fullscreen image viewer supporting pinch-to-zoom, double-tap-to-zoom, panning, and swiping through galleries.
- **Image Downloads** - Added ability to download images directly to the device's Downloads folder.
- **Custom Video Player** - Built a custom Material 3 Expressive UI over ExoPlayer with smooth seeking and full playback controls.
- **ExoPlayer Caching** - Reuses ExoPlayer instances during scrolling for buttery smooth performance.

#### Navigation & Integration
- **In-App Deep Linking** - AniList URLs for anime, manga, characters, and forum threads now open natively in the app instead of an external browser.

#### Social & Forums
- **Social Notifications** - Push notifications for forum replies, mentions, comment/thread likes, and thread subscriptions.
- **Granular Settings** - Redesigned settings to exactly control which types of social notifications you receive.
- **Performance Improvements** - Re-engineered complex thread rendering to flatten comment trees, drastically improving scrolling performance.
- **Enhanced Thread Parsing** - Parses native `@User` mentions to properly reconstruct "flat" AniList threads into a Reddit-style comment tree.
- **Improved UI & Animations** - Added organic curved lines for comment tracking, animated scroll-to highlighted comments.

### Changed
- **Centralized Errors & Rate Limiting** - A robust networking system that properly handles 429 Rate Limits, 401 Unauthorized errors (with re-auth prompts), network drops, and GraphQL-level structural errors across all repositories.
- **Html/Markdown Parsing** - Overhauled the underlying `RichTextParser` to fully support recursive structures like nested blockquotes, custom tables, inline images, code blocks, and spoilers without visual glitches.
- **Media Thumbnails** - Explicit cache policies logic for better UI performance out-of-the-box.

## [1.1.0] - 2026-03-01

### Added

#### Forum
- **Forum Hub** - New "Forum" tab in main navigation with three feeds: Overview, Recent, and New
- **Thread Browsing** - Browse threads by category with horizontally scrollable category chips
- **Thread Detail** - Full thread view with nested comment rendering (up to 3 levels deep), Reddit-style colored nesting lines, and OP badges
- **Thread Creation** - Compose new threads with category selection, markdown preview toggle, character counter, and unsaved-changes confirmation dialog
- **Comment Replies** - Reply to threads and comments via bottom sheet with markdown formatting toolbar (bold, italic, strikethrough, code, links, spoilers) and preview toggle
- **Thread Search** - Debounced search across forum threads with immersive full-screen search bar
- **Thread Saving (Bookmarks)** - Save threads locally for offline access; "Saved" feed displays bookmarked threads from local database
- **Thread Subscriptions** - Subscribe/unsubscribe to threads via the AniList API; "Subscribed" feed for followed threads
- **Comment Sorting** - Sort comments by Newest or Oldest
- **Thread Sharing** - Share thread URLs directly from the detail screen
- **Comment Collapsing** - Collapse/expand comment sub-trees with descendant count
- **Inline Images** - Parse and render `img###(url)`, `![alt](url)`, and `<img>` syntaxes with fullscreen pinch-to-zoom viewer
- **Flat Reply Parsing** - Reconstruct comment trees from `@Username` mention-based replies
- **Rich Markdown/HTML Parser** - Comprehensive parser supporting headers, lists, blockquotes, horizontal rules, inline code, spoilers, alignment wrappers, and HTML entities (named and numeric)
- **Empty States** - Dedicated empty state screens for no threads, no comments, and no search results, each with contextual actions
- **Pull-to-Refresh** - Pull-to-refresh on Forum Hub, Category, and Thread Detail screens
- **Staggered Animations** - Animated entry for headers, chips, and list items across all forum screens
- **Optimistic Updates** - Instant UI feedback for liking, saving, and subscribing actions

#### In-App Updates
- **Update Manager** - New `UpdateManager` singleton centralizing update logic (check, download, install) with `StateFlow`-based state management
- **Background Update Checks** - Periodic checks every 6 hours via `UpdateCheckWorker` with notification when a new version is available
- **Automatic Launch Check** - App checks for updates on startup
- **Update Dialog** - Reusable composable showing release notes, download progress, and install prompts
- **Atomic Downloads** - Downloads write to `.tmp` file and rename on success to prevent partial installs
- **Unknown Sources Flow** - Handles "Install from Unknown Sources" permission on Android O+

#### Developer Tools (Debug Builds Only)
- **Developer Tools Screen** - Consolidated debug utilities accessible from Settings
- **Update Debug** - Force update check or simulate available update
- **Build Information** - Display version, build type, flavor, and other build details

### Changed
- **Haptic Feedback** - Replaced custom `HapticFeedbackHelper` with Compose's `HapticFeedback` API, then added a robust 4-tier fallback chain for reliable haptics across devices and API levels (including Samsung One UI Core)
- **`HtmlText` Component** - Moved from `forum` package to shared `components` package; migrated to modern `Text` with `LinkAnnotation`
- **Hilt ViewModels** - Switched to `hiltViewModel()` from deprecated `hiltNavigation.compose.hiltViewModel()`
- **RTL Support** - Use `Icons.AutoMirrored.Filled.Reply` for proper LTR/RTL layout
- **Forum Thread Cards** - Redesigned with modern layout, media thumbnails, last reply info, and pinned/locked indicators
- **Timestamp Formatting** - More granular relative timestamps (e.g., "2w ago")
- **Database Schema** - Squashed intermediate migrations (v4/v5/v6) into a single v3 -> v4 auto-migration for clean upgrade path
- **CI Workflow** - Use PAT for release notes generation

### Fixed
- **`EditLibraryEntrySheet`** - Sheet now automatically closes after saving an entry
- **Animated Favorite Button** - Fixed size to prevent layout shifts during sparkle animation
- **VIBRATE Permission** - Removed; haptic feedback now delegated to Android system

### Removed
- **`UpdateUtil.kt`** - Replaced by the new `UpdateManager`
- **`ForumCategoryScreen`** (legacy) - Replaced by the new integrated forum UI
- **Notification Debug Section** - Moved from `NotificationsScreen` to `DeveloperToolsScreen`

---

## [1.0.1] - 2026-02-22

### Added
- Initial documentation suite with Mermaid diagrams
- Room database migration infrastructure
- Schema export for migration testing

### Changed
- Reset database version to 1 for clean migration path
- Enabled Room schema export for version tracking

### Fixed
- Custom seed color `Color(0x00000000)` (transparent black) was incorrectly treated as "no color set" due to 0-value collision in `AppSettings`
- Orphaned palette state when clearing custom seed color while `selectedPaletteId` is `"custom"` -- now auto-resets to `"dynamic"`
- `PaletteStyleSelector` could be interacted with in disabled contexts -- added `enabled` parameter gating expand and input
- Dynamic palette preview in `ColorSchemeSelector` ignored dark mode -- now uses `dynamicDarkColorScheme`/`dynamicLightColorScheme` on Android 12+
- Color picker hue slider state lost on configuration change -- changed from `remember` to `rememberSaveable`
- Dynamic palette option was shown on pre-Android 12 devices where it is unsupported -- now filtered out
- Orphaned palette state when device conditions change (e.g., restored backup from Android 12+ device to pre-12) -- added `LaunchedEffect` auto-reset
- `PaletteStyleSelector` was interactive when Dynamic palette was selected (palette style has no effect on dynamic colors) -- now disabled
- `getTitleLanguageLabel()` and `getTitleLanguageExample()` in `LookAndFeelScreen` were unnecessarily public -- made `private`

### Removed
- Legacy migration code (MIGRATION_12_13)

---

## [1.0.0] - YYYY-MM-DD

### Added

#### Core Features
- **Library Management** - Track anime/manga with progress, scores, notes
- **Status Tracking** - Watching, Planning, Completed, Dropped, Paused
- **AniList Sync** - Full synchronization with AniList.co account
- **Offline Support** - Full functionality without internet connection

#### Discovery
- **Trending** - Browse currently trending anime
- **Popular This Season** - See what's popular this season
- **Upcoming Releases** - Discover upcoming anime
- **Advanced Search** - Search with genre, year, format filters

#### Media Details
- **Comprehensive Info** - Synopsis, genres, studios, airing info
- **Characters & Voice Actors** - Character details with VA information
- **Related Media** - Sequels, prequels, side stories
- **Streaming Links** - Direct links to streaming platforms

#### Home Screen Widgets
- **Up Next Widget** - Upcoming episodes with countdown timers
- **Airing Today Widget** - Timeline view of today's episodes
- **Weekly Calendar Widget** - 7-day anime schedule overview
- **Trending Widget** - Top trending anime grid

#### Notifications
- **Watching Alerts** - Notifications for new episodes
- **Planning Alerts** - Know when planned shows premiere
- **Upcoming Alerts** - Two-tier system (12h and 2h before)
- **Configurable** - Enable/disable per notification type

#### User Experience
- **Material 3 Design** - Modern Material You theming
- **Dynamic Colors** - Adapts to system wallpaper colors
- **Dark Mode** - Full dark theme support
- **Smooth Animations** - Polished transitions and effects

### Technical
- Kotlin 2.2 with Jetpack Compose
- MVVM + Clean Architecture
- Hilt dependency injection
- Apollo GraphQL 4.x
- Room database with KSP
- WorkManager for background tasks
- Jetpack Glance for widgets

---

## Version History Template

When releasing a new version, copy this template:

```markdown
## [X.Y.Z] - YYYY-MM-DD

### Added
- New features

### Changed
- Changes to existing functionality

### Deprecated
- Features to be removed in future versions

### Removed
- Features removed in this release

### Fixed
- Bug fixes

### Security
- Security-related changes
```

---

## Categories Explained

- **Added** - New features
- **Changed** - Changes to existing functionality
- **Deprecated** - Features that will be removed in future versions
- **Removed** - Features removed in this release
- **Fixed** - Bug fixes
- **Security** - Vulnerability fixes

---

## Release Checklist

Before tagging a release:

- [ ] Update version in `app/build.gradle.kts`
- [ ] Update this CHANGELOG
- [ ] Run full test suite
- [ ] Build release APK/AAB
- [ ] Test on multiple devices
- [ ] Update screenshots (if UI changed)
- [ ] Create GitHub release with notes

---

## Links

- [GitHub Releases](https://github.com/Marco-9456/AniSync/releases)
- [Issue Tracker](https://github.com/Marco-9456/AniSync/issues)
- [Contributing Guide](CONTRIBUTING.md)
