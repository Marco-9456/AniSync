# AniWorld Parser Contract (Observed 2026-07-13)

Source inspected: `https://aniworld.to/animekalender` via one metadata-only `curl` request on 2026-07-13. No video, hoster, player, authentication, or bypass behavior was inspected.

## Page Structure
- The page is German HTML with `<html lang="de">` and the calendar content under `#seriesContainer`.
- Week tabs are an unordered list `ul.calendar.row`; each tab calls `scrollToDate('<weekday>')` for `montag` through `sonntag`.
- Each day is a `section.calendarList` with id equal to the German weekday (`montag`, `dienstag`, etc.).
- Each day heading is `h3` text like `Montag, 13.07.2026 (heute)` or `Dienstag, 14.07.2026`.
- Entries are direct cards inside `div.seriesListContainer.row`, each card generally `div.col-md-15.col-sm-3.col-xs-6` containing an `a`.

## Entry Fields
- Link: anchor `href` examples:
  - `/anime/stream/love-unseen-beneath-the-clear-night-sky/staffel-1/episode-2`
  - `/anime/stream/the-oblivious-saint-cant-contain-her-power`
  - Some links include explicit `/staffel-N/episode-M`; some omit it even when title attributes contain `S1E3`.
- Cover: first image has `data-src="/public/img/cover/..._200x300.png"`; `alt` contains `<title> Cover`; `noscript img title` can contain `Title S1E3 als Stream`.
- Title: `h3.seriesTitle` visible text, with a child `span.paragraph-end` to ignore.
- Season/episode: first `small` contains compact text like `S01E03` and language flag image(s).
- Time: second `small` contains text like `~ 15:40 Uhr`; observed `~` approximate marker and 24-hour `HH:mm Uhr` text.
- Online marker: some current/past entries include `span.listTag.bigListTag.grey` title `Stream online!` with check icon.

## Language Markers Observed
Language must be inferred from explicit flag image attributes, not duplicate count.

| Internal kind | Observed marker(s) | Visible V1 label |
| --- | --- | --- |
| GERMAN_SUB | `img.flag[data-src="/public/img/japanese-german.svg"]`; `alt="Deutscher Untertitel"`; `title="Episode N mit deutschem Untertitel"`; noscript alt/title mentions `German Subtitle` / `Untertitel Deutsch` | `DE-Sub` |
| GERMAN_DUB | `img.flag[data-src="/public/img/german.svg"]`; `alt="Deutsche Flagge, German Flag"`; `title="Episode N auf Deutsch"` | `DE-Dub` |
| ENGLISH_SUB | `img.flag[data-src="/public/img/japanese-english.svg"]`; `alt="Englischer Untertitel Flagge, English Subtitle Flag"`; `title="Episode N auf Englisch"` | not visible |
| ENGLISH_DUB | No unambiguous separate English-dub marker found in the inspected document. If future DOM uses an English-only flag, classify only after explicit marker confirmation. | not visible |
| UNKNOWN | Missing or unrecognized marker | not visible unless diagnostic |

Specific markers such as `japanese-german.svg` and `Deutscher Untertitel` must be checked before generic `german.svg` or `Deutsch`.

## Reference Case Observed
`The Oblivious Saint Can’t Contain Her Power` appeared under `Dienstag, 14.07.2026` as two separate cards with the same title, same time `~ 15:40 Uhr`, same `S01E03`, and link `/anime/stream/the-oblivious-saint-cant-contain-her-power`. One card had `japanese-german.svg` / `Deutscher Untertitel`; one had `japanese-english.svg` / English subtitle. V1 visible result should be one German release line: `Episode 3 · DE-Sub`.

## Consolidation Rules
- Consolidate cards only when AniWorld title, slug/link identity, calendar date, time, season, and episode are identical.
- Merge German DE-Sub and DE-Dub at the same timestamp into one visible release label `DE-Sub / DE-Dub`.
- Keep DE-Sub and DE-Dub as separate release times if their times differ.
- Never merge different episode numbers, even with same title/time.
- Multiple episodes at the same time for the same anime must remain separate releases.
- Ignore English languages for visible V1 release labels but retain internally for diagnostics/matching confidence.

## Date / Time Contract
- Date heading format observed: German weekday, comma, `dd.MM.yyyy`, optional `(heute)`.
- Time text observed: approximate marker `~`, then `HH:mm Uhr`.
- The document does not state an explicit timezone. Because the site and dates are German, `Europe/Berlin` is the likely source timezone, but this remains unconfirmed until validated with screenshots or a documented site statement.

## DOM Change Handling
- Parser should fail safely if `#seriesContainer` or day sections disappear.
- A successful refresh with an unexpectedly empty parse result must not replace a non-empty cache.
- Unknown language markers should be captured in diagnostics and should not produce visible German release labels.
- Tests must use local HTML fixtures only; CI must not fetch AniWorld.
