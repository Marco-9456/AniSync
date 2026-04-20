# Release Title: v1.5.0

# Release Notes

This release focuses on connected social workflows: activity details, stronger profile interactions, broader deep-link coverage, and a full widget visual refresh so app surfaces feel more consistent and actionable.

### Added
#### Activity & Profile
- Added a dedicated Activity Detail screen with richer activity content presentation.
- Added profile messaging with a composer sheet backed by AniList `SaveMessageActivity`.
- Added pagination for Profile Social and Reviews tabs.
- Added direct navigation from profile content to related forum threads/comments and review details.
- Added pull-to-refresh support in profile surfaces.

#### Navigation & Notifications
- Added deep-link handling for user profiles, review pages, and forum comments.
- Added a social activity notification channel with new preference toggles and worker dispatch paths.

#### Widgets
- Redesigned all Glance widgets with modern Material 3 card-style layouts.
- Added day selection to the Weekly Calendar widget and improved schedule fetching behavior.
- Added streaming link support to airing data used by the Up Next widget.
- Refreshed launcher/widget preview layouts to match the new widget design.

### Changed
- Extended shared-element transitions across detail/grid flows and improved return transition stability in Library/Discover.
- Refreshed activity/forum/content card design for denser and cleaner presentation.
- Upgraded build tooling to AGP 9.1.0 with SDK 37 targets and AGP 9-compatible APK naming flow.
- Advanced Room schema to version 12 with auto-migration (11 -> 12).

### Fixed
- Improved rich text parser handling for mixed markdown/HTML links and inline grouping edge cases.
- Reduced transition return glitches across several navigation paths.

### What's Next
1. Continue polishing Activity detail interactions and cross-surface navigation consistency.
2. Expand social notification controls and behavior tuning.
3. Improve widget customization and data freshness behavior.
