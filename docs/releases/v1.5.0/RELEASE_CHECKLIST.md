# Release v1.5.0 Checklist (No Publish Yet)

This checklist intentionally stops before publishing/tagging.

## 1) Versioning & Changelog
- [x] Bump `versionCode` to `9` in `app/build.gradle.kts`.
- [x] Bump `versionName` to `1.5.0` in `app/build.gradle.kts`.
- [x] Add `1.5.0` section to `docs/CHANGELOG.md`.

## 2) Validate Build Quality
- [x] Run tests: `./gradlew test`
- [x] Build release APKs: `./gradlew assembleStableRelease`
- [x] Confirm build success.

## 3) Prepare Manual Release Notes (Your Flow)
- [x] Draft GitHub manual release notes:
  - `docs/releases/v1.5.0/GitHubReleaseNote.md`
- [x] Draft AniList thread release comment (HTML):
  - `docs/releases/v1.5.0/AniListReleaseNote.html`

## 4) Pending Your Approval (Not Done)
- [ ] Review and approve the version/changelog edits.
- [ ] Review and approve both release note drafts.
- [ ] Only after approval: create release commit.
- [ ] Only after approval: create tag `v1.5.0`.
- [ ] Only after approval: push commit + tag.
- [ ] Only after approval: create GitHub release with manual notes.
- [ ] Only after approval: post AniList release note comment to your launch thread.

## 5) Commands I Will Run After Approval
```bash
git add app/build.gradle.kts docs/CHANGELOG.md docs/releases/v1.5.0/*
git commit -m "chore(release): prepare 1.5.0"
git tag v1.5.0
git push origin main
git push origin v1.5.0
gh release create v1.5.0 --title "v1.5.0" --notes-file docs/releases/v1.5.0/GitHubReleaseNote.md
```

## 6) Manual Step You Usually Do (or I can do with thread info)
Post `docs/releases/v1.5.0/AniListReleaseNote.html` as a comment under your existing AniList launch thread.
