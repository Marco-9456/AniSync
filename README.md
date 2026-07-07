# AniSync

<p align="center">
  <img src=".github/logoWide.svg" alt="AniSync Logo" width="300" height="100">
</p>

<p align="center">
  <strong>A native Android client for AniList — track your anime and manga the way you want</strong>
</p>

<p align="center">
  <a href="https://www.android.com/"><img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=flat&logo=android&logoColor=white" alt="Platform"></a>
  <a href="https://developer.android.com/about/versions/oreo/"><img src="https://img.shields.io/badge/Min%20SDK-26-blue?style=flat" alt="Min SDK"></a>
  <a href="https://kotlinlang.org/"><img src="https://img.shields.io/badge/Kotlin-2.2.21-7F52FF?style=flat&logo=kotlin&logoColor=white" alt="Kotlin"></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat&logo=jetpackcompose&logoColor=white" alt="Compose"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPLv3-blue?style=flat" alt="License"></a>
</p>

<p align="center">
  <a href="https://github.com/Marco-9456/AniSync/releases"><img src="https://img.shields.io/github/downloads/Marco-9456/AniSync/total?style=flat&logo=github&color=brightgreen&label=Downloads" alt="Total Downloads"></a>
  <a href="https://github.com/Marco-9456/AniSync/releases/latest"><img src="https://img.shields.io/github/v/release/Marco-9456/AniSync?style=flat&logo=github&label=Latest" alt="Latest Release"></a>
  <a href="https://f-droid.org/packages/com.anisync.android/"><img alt="F-Droid Version" src="https://img.shields.io/f-droid/v/com.anisync.android?style=flat&logo=f-droid&logoColor=%239BB9CC&color=%23CC8F91"></a>
  <a href="https://github.com/Marco-9456/AniSync/stargazers"><img src="https://img.shields.io/github/stars/Marco-9456/AniSync?style=flat&logo=github&color=yellow" alt="Stars"></a>
  <a href="https://hosted.weblate.org/engage/anisync/"><img src="https://hosted.weblate.org/widget/anisync/app/svg-badge.svg" alt="Translation status"></a>
</p>

<p align="center">
  <a href="https://ko-fi.com/marco_9456"><img src="https://img.shields.io/badge/Ko--fi-Support-FF5E5B?style=flat&logo=ko-fi&logoColor=white" alt="Ko-Fi"></a>
  <a href="https://github.com/sponsors/Marco-9456"><img src="https://img.shields.io/badge/GitHub-Sponsor-EA4AAA?style=flat&logo=githubsponsors&logoColor=white" alt="GitHub Sponsors"></a>
</p>

AniSync is a native Android app for [AniList.co](https://anilist.co) — a fast, offline-first way to track your anime and manga, discover new stuff, and keep up with the community. No browser tab required.

> [!NOTE]
> AniSync is not affiliated with AniList. It's a third-party client built for the AniList community.

---

## :camera: Screenshots

<p align="center">
  <img src=".github/Screenshots.svg" width="100%" alt="AniSync app screenshots">
</p>

### Shareable cards

<p align="center">
  <img src=".github/Shareable-cards.svg" width="100%" alt="AniSync shareable cards">
</p>

---

## :sparkles: Features

Everything you'd want from an AniList client, in one app:

- **Tracking** — obviously. Statuses, scores, progress, notes, and custom lists for your anime and manga.
- **Search & Discovery** — trending, seasonal, and upcoming titles, plus proper filters for when you know exactly what you're after.
- **Media Details** — characters, voice actors, staff, trailers, reviews, recommendations, and where to watch.
- **Feed** — see what your friends are watching and judge them accordingly.
- **Forums** — browse, post, like, and subscribe without ever leaving the app.
- **Stats** — nice charts that prove you watch too much anime.
- **Widgets & Notifications** — airing schedules on your home screen and reminders before episodes drop.
- **Offline-First** — your library works without internet and syncs when you're back online.

---

## :scroll: Changelog

Curious what's new (or what broke and got fixed)? Check the [CHANGELOG](docs/CHANGELOG.md) or the [releases page](https://github.com/Marco-9456/AniSync/releases).

---

## Contributing

Contributions are welcome! Read [CONTRIBUTING.md](docs/CONTRIBUTING.md), fork, branch, and open a PR.

---

## License

This project's source code is licensed under the **GNU General Public License v3.0** — see the [LICENSE](LICENSE) file for details.

> [!WARNING]
> **Brand & Naming Guidelines**
>
> While the source code is freely available under the GPLv3, the **AniSync** name and brand identity are protected. Any derivative works — including forks and unofficial builds — are strictly prohibited from using "AniSync" as the name for an AniList client application.

---

## Acknowledgments

- [AniList](https://anilist.co) for the excellent GraphQL API
- [Material Design 3](https://m3.material.io) for the design system
- [Seal](https://github.com/JunkFood02/Seal) and [ReadYou](https://github.com/ReadYouApp/ReadYou) for UI/UX inspiration
- The Android and Kotlin communities for amazing tools and libraries

---

## The FAQ

* *Why isn't the app on the Play Store?*
  * I need MONEY <img src="https://files.catbox.moe/uqa4ad.jpg" width="50" alt="DUTCH"> — and a new laptop or rent matters more to me than handing it to Google.
* *Where do I actually get it, then?*
  * F-Droid or the GitHub releases page — the badges up top link straight to both. That's it. No sketchy APK mirror sites, please.
* *Is it free? Any ads or tracking?*
  * Completely free and open source under GPLv3. No ads, no trackers, no analytics, no telemetry — I don't collect anything about you.
* *Is it safe to log in?*
  * Yep. Login goes through AniList's official OAuth, so the app never sees your password. Your access token is encrypted on-device with AES-256-GCM and only ever talks to AniList's API — nothing else.
* *Why isn't the app on the AniList clients list?*
  * It's a bit complicated, but long story short: a mod told me the app wasn't getting added because the release thread was AI-generated. I rewrote it, let them know, and then didn't hear anything for quite a while. When I followed up recently, the same mod now says the app is vibecoded, so they won't add it. I asked right away how they reached that conclusion — and never heard back again.

  Getting told your first open-source project is "vibecoded" stung, not gonna lie. But whatever. If a mod with actual front-end and back-end experience reviews the app down the line and changes their mind, great; if not, no hard feelings either. I still love AniList :)
* *Why another AniList client? Aren't there enough?*
  * Maybe! But I wanted one that felt genuinely native and looked like a proper Material 3 app. Building it turned out to be way more fun than I expected, so here we are.
* *Found a bug or want a feature?*
  * Open an issue on GitHub. I actually read them and usually reply fast (see the vibecoded saga above). No promises on every feature, but good ideas tend to make it in.
* *Can I help translate?*
  * Please do — AniSync is on Weblate (badge's up top). Pick your language and go, no coding required.
* *Will it ever come to iOS?*
  * Probably not. Compose Multiplatform exists, but I don't want to touch it — every multiplatform app I've tried ran noticeably worse than native Android, since the iOS side wasn't native. Unless I actually learn Swift and SwiftUI (not happening any time soon), it's staying Android-only.