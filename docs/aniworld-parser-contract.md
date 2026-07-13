# AniWorld Parser Contract

Observed source contract recorded on 2026-07-13 and implemented by `JsoupAniWorldCalendarParser`. The original observation used one metadata-only request to `https://aniworld.to/animekalender`; no video, hoster, player, authentication, CAPTCHA, or bypass behavior was inspected. Production fetches occur only at explicit app runtime refresh. Tests and CI use local fixtures exclusively.

## Required Document Structure

- Calendar root: `#seriesContainer`.
- Day sections: `section.calendarList`; at least one is required.
- Day heading: an `h3` containing a `dd.MM.yyyy` token.
- Entry anchor: each `h3.seriesTitle` represents a source card.
- The card is resolved through `[data-calendar-entry]`, then observed Bootstrap card classes, then the title parent.
- Source identity prefers the slug following `/anime/stream/`; otherwise it uses the normalized source title.

Missing root/day structure is a hard parse failure. Known block/challenge text such as Cloudflare, CAPTCHA, Access Denied, or Just a moment is a distinct block-page failure. The client never attempts to solve or bypass it.

## Fields

| Field | Accepted source | Result |
| --- | --- | --- |
| Title | own text of `h3.seriesTitle`, falling back to full text | raw and normalized title |
| Source link/slug | first `a[href]` in card | optional URL, slug, stable source-series key |
| Date | `dd.MM.yyyy` in day heading | `LocalDate` |
| Installment | first `small` own text | Episode `SxxExx`, `Film N`, `Special N`, or UNKNOWN |
| Time | later `small` text | optional `~`, one/two digit hour, two digit minute, `Uhr` |
| Language | explicit flag `data-src`, `src`, `alt`, and `title` | DE_SUB, DE_DUB, EN_SUB, or UNKNOWN |
| Order | card traversal order | stable `sourceOrder` |
| Diagnostics | missing/invalid time or installment, language, DST | stored release diagnostic |

A time-like value containing `Uhr` but not matching the contract is rejected. Missing time is retained as null. Unknown installment text becomes UNKNOWN; it does not invent an episode.

## Language Markers

Specific markers are evaluated before generic German markers.

| Internal language | Confirmed markers | V1 visibility |
| --- | --- | --- |
| `DE_SUB` | `japanese-german`, `Deutscher Untertitel`, `German Subtitle` | `DE-Sub` |
| `DE_DUB` | path ending `/german.svg`, `Deutsche Flagge`, `auf Deutsch` | `DE-Dub` |
| `EN_SUB` | `japanese-english`, `Englischer Untertitel`, `English Subtitle` | diagnostic only |
| `UNKNOWN` | missing or unrecognized marker | diagnostic only |

The effective repository exposes only visible German languages. Unknown/English-only rows stay stored for diagnostics and do not become calendar/library releases.

## Consolidation

Exact duplicate rows are removed by source identity, day, local time, kind, season, episode/installment, and language.

DE-Sub and DE-Dub rows merge to `DE_SUB_AND_DUB` only when source identity, day, local time, kind, season, episode, and installment all match. The merged row keeps the earliest source order and both marker sets.

The parser must not merge:

- different release times;
- different episode or installment numbers;
- film/special/episode kinds;
- different source identities.

Final releases are stable-sorted by original source order.

## Date, Time, and DST

`Europe/Berlin` is the fixed AniSync Plus source-zone policy. This is a product decision based on the German calendar source; it is not presented as a timezone declaration found in the HTML.

- Exact and approximate local times both resolve to an instant for sorting/countdown.
- Approximate remains explicitly marked in the domain/UI.
- A DST gap is rejected because the local time does not exist.
- A DST overlap selects the earlier valid offset and records `DST_OVERLAP_EARLIER_OFFSET`.
- Missing time has no instant and no invented countdown.

Calendar grouping always uses the original source date even if instant conversion in another device timezone would cross a day boundary.

## Snapshot Identity and Validation

The parser computes SHA-256 over the HTML. Snapshot ID is `aw-` plus the first 24 hash characters. Each release ID hashes its complete source identity.

Repository validation additionally requires:

- at least one day section;
- ordered non-inverted range;
- 64-character lowercase document SHA-256;
- current parser version;
- every release using the document snapshot ID;
- every release inside the document range;
- unique local release IDs.

A valid page may contain zero releases and replace the cache. Missing required structure, invalid values, mixed IDs, out-of-range rows, or duplicate IDs reject the refresh and preserve the old snapshot.

## Local Fixture Matrix

Checked-in fixtures cover reference parsing, bilingual same-time merge, different-time split, multiple episodes, film/special, unknown language, missing fields, valid empty, block page, missing structure, invalid date/time, and Berlin DST gap/overlap. The fixtures contain synthetic metadata only and make no network request.
