# AniWorld parser fixtures

These fixtures are small, synthetic, sanitized HTML documents derived only from the DOM contract observed on 2026-07-13 and rechecked on 2026-07-14 at `https://aniworld.to/animekalender`. They contain no player, video, streamhoster, authentication, cookie, or user data. Tests never contact AniWorld.

- Observation dates: 2026-07-13 and 2026-07-14
- Source URL: `https://aniworld.to/animekalender`
- Parser version: 2
- Sanitization: titles, links, covers, unrelated navigation, scripts, trackers, and all streaming/player sections were removed or replaced with fictional values.

| Fixture | Purpose | SHA-256 |
| --- | --- | --- |
| `aniworld_calendar_block_page.html` | Sanitized parser case for `block_page` | `103a5539255452c16f58e1258a1a9a4c1fe48d2c386fe2ef41b9928956b2afbc` |
| `aniworld_calendar_dom_changed.html` | Sanitized parser case for `dom_changed` | `90229db6b27afe7d317769b07340f10e04f81950620b386eda503a3bf21bb1e6` |
| `aniworld_calendar_dst_gap.html` | Sanitized parser case for `dst_gap` | `a32a1d41bf116814eaa12e602f5bab04099da9adb1f1d7517766f0bd94c97b96` |
| `aniworld_calendar_dst_overlap.html` | Sanitized parser case for `dst_overlap` | `18c32899c6a76ccaa86da59937265b7888cac44f74d407805133b2c71a1ad405` |
| `aniworld_calendar_empty_valid.html` | Sanitized parser case for `empty_valid` | `4e9e266602de0269fbf426e91caba4c428a0766c084aa4a3ee76f3370faca7aa` |
| `aniworld_calendar_film_special.html` | Sanitized parser case for `film_special` | `f4a6281eff06ff86bda48a3f5e73b5d429799c6f2d30e477c1d92667091789d6` |
| `aniworld_calendar_invalid_date.html` | Sanitized parser case for `invalid_date` | `d4ab62ac6d87a830b5997a41a43483f0c35988ce66690891e79c6a4fd8bb53d3` |
| `aniworld_calendar_invalid_time.html` | Sanitized parser case for `invalid_time` | `e04c934085250c64ed15f40b991129616d2b2359ac250fd79e0cb72dd514d027` |
| `aniworld_calendar_missing_fields.html` | Sanitized parser case for `missing_fields` | `fea806874492b96a69c543ecbc6fcd89b23b4cadb2dabcb5a4781387846664af` |
| `aniworld_calendar_multi_episode.html` | Sanitized parser case for `multi_episode` | `46dfa24f63935787f37145be7eb696c2ea6eca966c206227100dcb2b99febe3e` |
| `aniworld_calendar_multi_time.html` | Sanitized parser case for `multi_time` | `0d7d061d15280816ee89621d4898c8fc317a39d262be65c147652dd35f33c385` |
| `aniworld_calendar_reference.html` | Sanitized parser case for `reference` | `a5f5ecc2c5271d7ed8a37a7af3adb4e7705dcea3387a7d28dfab0162c606f59e` |
| `aniworld_calendar_security_words_valid.html` | Valid calendar despite unrelated security words and CDN markers | `e780f315daac4f21f6e00eafb0eec41e2296e2a771bb4ad7fb6bf05a059e591e` |
| `aniworld_calendar_same_time_bilingual.html` | Sanitized parser case for `same_time_bilingual` | `a06c44d4a2c1fa9dde8747d2ed57e12f5625ae3215d5721709563ba1f65ba695` |
| `aniworld_calendar_unknown_language.html` | Sanitized parser case for `unknown_language` | `990f2f7e1032bd50e2bbd4b55f99755a106f7c7823f3af14167928dcd7390333` |
