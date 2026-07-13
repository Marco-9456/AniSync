# AniSync Plus Implementierungs- und Prüfbericht

Stand: 2026-07-14. Dieser Bericht beschreibt nur beobachteten Repositoryzustand und tatsächlich ausgeführte Prüfungen. Die historische Auditdatei `docs/anisync-plus-phase1-forensic-audit.md` wurde nicht inhaltlich verändert.

## A. Status

`COMPLETE` für den im Master-Prompt definierten V1-Codeumfang und die lokal möglichen Pflichtgates.

| Bereich | Status | Nachweis |
| --- | --- | --- |
| Forensischer Auditpayload | COMPLETE | 31.755 Bytes, 946 Zeilen, erwarteter SHA-256 exakt |
| Separates Kalender-Modul/DB | COMPLETE | `:anisyncplus-calendar`, `anisync_plus_calendar.db`, exportiertes Room-Schema |
| Parser/Konsolidierung/DST | COMPLETE | lokale Fixtures, 41 grüne Modultests insgesamt |
| Matching/Mapping | COMPLETE | persistierte Mappings, konservative Schwellen, Diagnostiktests |
| Atomarer Cache/Refresh | COMPLETE | Transaktions- und Fehlererhaltungstests |
| Kalenderintegration | COMPLETE | cache-only Startup, expliziter Refresh, Status/Range/Retry, kein AniList-Fallback |
| Watching Library | COMPLETE | Anime/CURRENT-only, Grid und Liste, Countdown/Behind/Sort, kein Ziel-Fallback |
| Settings/Diagnostik | COMPLETE | eigene Preferences, Kategorie/Route, Refresh und Statusdaten |
| Branding/Parallel-ID | COMPLETE | App-ID/Labels/APK mit `aapt` verifiziert |
| PR-/Push-/Release-CI | COMPLETE | Wrapper, Tests/Lint/Build, exakte Artefakte, persistente Signierung |
| Unit/Robolectric | COMPLETE | 41 Modul + 184 App = 225 Tests, 0 Fehler |
| Lint | COMPLETE | keine neuen Befunde; auditierte Upstream-Baseline gefiltert |
| Stable-Debug APK | COMPLETE | gebaut, Paket/Label/Hash geprüft |
| Device/Emulator UI | nicht verfügbar | durch Robolectric und lokale Build-/Metadatenprüfung ersetzt |
| Signierter Stable Release | nicht lokal ausführbar | persistente Secrets fehlen; Workflow muss deshalb hart fehlschlagen |

## B. Repositoryzustand

- Startbranch: `main`
- Start-HEAD: `1c69ce5ce5ed6fd6c4ebfdd213245c5dabc38e93`
- Arbeitsbranch: `codex/anisyncplus-complete-implementation`
- Verifizierter Upstream-Merge-Base: `8f1029db05b09dc612d3af061a01b0e0774d1531`
- Start-Ahead/Behind gegen `upstream/main`: 5/0
- `origin`: `xxxxxxxxxxssxx/AniSyncPlus`
- `upstream`: `Marco-9456/AniSync`
- Ausgangs-Worktree vor Auditdekodierung: sauber
- PR #1 und #3: gemergte Dokument-/Workflow-Baseline
- PR #2: extern offen, nicht gemergt und inhaltlich durch diese Implementierung überholt; keine externe Mutation durchgeführt

### Implementierungscommits

| Commit | Thema |
| --- | --- |
| `2b2d60a0` | Auditdatei und Korrektur überholter Planungsbehauptungen |
| `19404abd` | Parser, Client, Matcher, Room-Cache, Resolver und Fixtures |
| `f9149460` | Refresh-/DB-/Matching-Fehlerhärtung |
| `4c86b90c` | Kalender-, Library-, Settings- und DI-Appintegration |
| `e27068f9` | App-Branding, Lint-Baseline, Buildspeicher |
| `3d58d847` | PR/Push/Tag/manuelle GitHub-Workflows |
| `ac242dc5` | lokalisierte Kalenderstatusmeldungen |
| `c2a77df2` | gemeinsamer Library-Minuten-Ticker für Grid und Liste |
| `6eaef188` | strukturelle Challenge-Erkennung und sichere AniWorld-Laufzeitdiagnostik |

## C. Implementierte Funktionen

### Laufzeitdatenfluss

`Pull-to-refresh/Retry/Settings Refresh -> dedizierter OkHttp-Client -> Jsoup-Parser -> Strukturvalidierung -> atomarer Room-Snapshot -> persistiertes/beschränktes Matching -> gemeinsamer EffectiveRelease-Flow -> Kalender + Anime/CURRENT-Library`

Startup, Navigation, Tests und CI überspringen den Netzwerkpfad vollständig.

### Parser und Cache

- Ein Abruf der Kalenderseite pro manuellem Refresh.
- Strukturelle Block-/Challenge-Erkennung im Parser ohne HTML-Wortscan im Client und ohne Umgehungsversuch.
- Explizite DOM-, Datum-, Zeit-, Sprach- und Installmentregeln.
- `Europe/Berlin`, DST-Gap-Fehler und früher Offset bei Overlap.
- DE-Sub/DE-Dub-Zusammenführung nur bei gleicher vollständiger Releaseidentität.
- Gültig leere Seite akzeptiert; strukturell leere/geänderte Seite abgelehnt.
- SHA-basierter Snapshot/Release-Identifier.
- Atomare Aktivierung, alter Snapshot bei HTTP/Parse/Validierung/DB-Fehler.
- Unverändertes Dokument aktualisiert Fetchzeit statt Releasezeilen zu duplizieren.
- Mappings überleben Snapshotwechsel; Matchingfehler zerstören keinen akzeptierten Snapshot.
- Syncdiagnostik: Attempt/Success/Error/HTTP/Range/Counts/Versionen sowie datensparsame Logcat-Diagnosen ohne Body, Cookies, Querywerte oder Tokens.

### Regression vom 2026-07-14

Das bereitgestellte Debuglog enthielt keine AniWorld-Request-, Response- oder Parserzeilen und zeigte keinen zugehörigen Crash. Eine getrennte Metadatenprüfung mit den App-Headern reproduzierte jedoch die Ursache eindeutig: AniWorld lieferte HTTP 200 und vollständige Kalenderstruktur, während eine reguläre Stylesheet-URL auf `cdnjs.cloudflare.com` den bisherigen pauschalen Wortscan auslöste.

Der HTTP-Client klassifiziert deshalb keine HTML-Begriffe mehr. Der Parser akzeptiert vollständige Kalenderstruktur vorrangig und erkennt eine Challenge nur bei unvollständiger Pflichtstruktur plus starker struktureller Signatur. Ein lokaler Smoke-Test der dabei gespeicherten aktuellen Antwort ergab 15 Tagesabschnitte, 340 Releases, 184 sichtbare deutsche Releases und Parser-Version 2. Tests und CI selbst bleiben vollständig fixture-basiert und führen keine AniWorld-Anfrage aus.

### Matching

- Bounded AniList-Anime-Suche mit maximal 10 Kandidaten nur während manuellem Refresh.
- Titelvarianten: preferred, English, Romaji, native, synonyms.
- Normalisierung für Unicode, Apostrophe, Bindestriche, Satzzeichen und Leerraum.
- Exakt-/Staffelauflösung vor Similarity.
- Auto-Match >= 0,92 und Abstand >= 0,08; nicht eindeutige Ergebnisse bleiben ambiguous.
- Cover/ID/Navigation nur für sichere Matches.
- Unmatched deutsche Releases bleiben bei Filter aus sichtbar.

### Kalender

- Sofortiger lokaler Cache; kein automatischer AniWorld-Hintergrundrefresh.
- Woche und Monat lesen denselben Snapshotflow.
- Range, letzter erfolgreicher Refresh, aktiver Sync und letzter Fehler sichtbar.
- Fehler mit erhaltenem Cache als Banner inklusive Retry.
- Navigation außerhalb des Snapshotbereichs deaktiviert; Teilüberlappung auf Schnittmenge begrenzt.
- Filter aus: alle sichtbaren deutschen Releases.
- Filter an: sicher gematcht und aktives Konto `CURRENT`.
- Persistenz exakt nach Remember-Setting; Defaults enabled/remember off/filter off.
- Berlin-Quelltag, 24-Stunden-Zeit, approximate, Sprache, Episode/Film/Special/Unknown.
- Fehlende Zeit/Episode bleibt fehlend; kein erfundener Countdown.
- Material-Placeholder und deaktivierte Detailnavigation bei unmatched.
- Ein einziger Minuten-Ticker für alle Kalenderkarten.

### Watching Library

- Resolverprojektion nur für Anime/CURRENT.
- Manga und andere Statusobjekte unverändert.
- Frühester zukünftiger zeitlich bestimmter deutscher Release für Countdown/Airing Soon.
- Letzte bereits veröffentlichte numerische Episode für Behind.
- Film/Special/Unknown ohne erfundenen Behind-Badge.
- Fehlender AniWorld-Wert löscht Ziel-AniList-Airingwerte explizit.
- Nullwerte bei Airing Soon in beiden Richtungen zuletzt.
- Grid- und Listenkarte verwenden denselben AniWorld-Stand.
- Ein einziger Minuten-Ticker im `LibraryScreen`; kein Coroutine-/Timer pro sichtbarer Karte.

### Settings, Branding und CI

- Eigener Preferences-Namespace `anisync_plus_settings`.
- Neue Kategorie nach App Links und vor Updates in kompakt/two-pane.
- Enable, Filtermerken, Refresh, Status, Attempt/Success, Range, Counts, Fehler, Zone und Parser-/Matcher-Version.
- `de.mrxxxxx.anisyncplus` mit Debug-/Preview-Suffixen; Kotlin-Pakete unverändert.
- Variantenlabels und APK-Basename `AniSyncPlus`.
- PR-/Push-CI mit Wrapper, Modultests, App-Tests, Lint und Stable-Debug-Build.
- Tagrelease nur `v*`, minimale Berechtigung, `github.token`, zwingende persistente Secrets, Fingerprint.
- Debugartefakt explizit `non-upgrade-channel`, SHA und Run-Nummer im Artefaktnamen.
- Keine Live-AniWorld-Anfragen in Tests oder Actions.

## D. Befehle und Ergebnisse

| Befehl/Prüfung | Exit | Ergebnis |
| --- | ---: | --- |
| Audit-Dekodierung aus Master-Prompt | 0 | 31.755 Bytes erzeugt |
| `wc -l docs/anisync-plus-phase1-forensic-audit.md` | 0 | 946 |
| `sha256sum docs/anisync-plus-phase1-forensic-audit.md` | 0 | `88305f414ad8f6407e3a991c24b910653ecff009580313f05e57e20cfe8a05b7` |
| `./gradlew tasks --all` mit lokalem JDK/SDK | 0 | reale Tasks inventarisiert |
| `:anisyncplus-calendar:testDebugUnitTest` | 0 | 41 Tests, 0 Fehler |
| `testStableDebugUnitTest` | 0 | 184 Tests, 0 Fehler |
| `:app:compileStableDebugKotlin :app:hiltJavaCompileStableDebug` | 0 | Kotlin/KSP/Room/Hilt kompiliert, keine Duplicate Bindings |
| `lintStableDebug` | 0 | keine neuen Issues; 1237 Errors, 582 Warnings, 2 Hints der auditierten Baseline gefiltert |
| `assembleStableDebug` | 0 | Universal- und ABI-APKs erzeugt |
| `assemblePreviewDebug` | 0 | Preview-APK erzeugt |
| `aapt dump badging` Stable Debug | 0 | `de.mrxxxxx.anisyncplus.debug`, `AniSync Plus Debug`, min 26, target 36 |
| `aapt dump badging` Preview Debug | 0 | `de.mrxxxxx.anisyncplus.preview.debug`, `AniSync Plus Preview` |
| `git diff --check` | 0 | keine Whitespacefehler |

Der erste vollständige Lintlauf offenbarte den großen upstream vorhandenen Übersetzungs-/Qualitätsrückstand. Dieser wurde in `app/lint-baseline.xml` eingefroren; danach erkannte Lint zwei neu hinzugefügte Statusübersetzungen und später eine versehentliche Alt-Persisch-Aktivierung. Beide wurden im Code/Ressourcenbestand behoben. Der abschließende Lauf meldet keine neuen Befunde.

Ein früher Stable-Debug-Packaginglauf scheiterte mit 2 GiB Gradle-Heap in `ApkFlinger.close`. Nach Erhöhung auf 4 GiB war der Build reproduzierbar erfolgreich. Es wurde keine Produktlogik zur Umgehung geändert.

## E. Nicht gelöste bzw. externe Punkte

- Kein Android-Gerät/Emulator stand zur Verfügung; deshalb keine visuelle Screenshot-, Installations- oder Interaktionsabnahme. Die geforderte nachweisbare Robolectric-Alternative ist grün.
- Kein persistenter Release-Keystore/Secretsatz war lokal verfügbar. Ein signierter Stable Release wurde bewusst nicht mit einem temporären Schlüssel erzeugt.
- Das Custom-Scheme `anisync` und bestehende HTTPS App Links sind extern registrierte Identitäten. Die neue Application ID erlaubt parallele Installation, aber deterministisches OAuth-Routing neben upstream benötigt einen separat registrierten AniList-Callback/Client.
- GitHub Actions wurden lokal syntaktisch/inhaltlich geprüft, aber ohne Push nicht auf GitHub ausgeführt.
- PR #2 bleibt extern offen; Schließen/Mergen war nicht Teil der autorisierten Repositoryänderung.
- Die Abschluss-TODO-Suche fand nur vier Beispielzeilen in `docs/CONTRIBUTING.md` und den bereits vorhandenen Discover-Navigations-TODO in `TrendingWidget.kt`; keiner betrifft AniWorld oder eine Zieloberfläche.
- Die 1237/582/2 Upstream-Lintbefunde bleiben als explizite Baseline-Schuld bestehen. Neue Befunde werden nicht akzeptiert.

Diese Punkte ändern nicht den V1-Codeabschluss; sie erfordern externe Infrastruktur, Secrets oder ein Gerät.

## F. Auswirkungen auf Upstream

### Geänderte ursprüngliche Dateien

| Datei(en) | Dokumentierte Änderung |
| --- | --- |
| `.github/workflows/build-manual-release.yml`, `build-release.yml` | Wrapper, genaue Artefaktwahl, persistente Signierung, keine temporären Keys |
| `ProjectContext.md` | verbindlicher finaler Iststand und externe Grenzen |
| `app/build.gradle.kts` | App-ID/Labels/APK, Tests, Lintbaseline |
| `app/src/main/graphql/Search.graphql` | Synonyme für Matchingkandidaten |
| `CalendarRepositoryImpl.kt` | cache-only AniWorld-Adapter ohne AniList-Fallback |
| `AiringEpisode.kt` | additive defaulted AniWorld-Präsentationsfelder |
| `LibraryEntry.kt` | additive runtime Felder latest/approximate |
| `CalendarScreen.kt`, `CalendarUiState.kt`, `CalendarViewModel.kt` | Range/Status/Refresh/Filter/Navigation/Quelltag |
| `AiringEpisodeCard.kt` | AniWorld-Karte, Placeholder, sichere Navigation |
| `LibraryViewModel.kt` | gemeinsame Resolverprojektion und Sortierung |
| `LibraryMediaCard.kt`, `LibraryListCard.kt`, `LibraryScreen.kt` | AniWorld Behind/Countdown in Grid/Liste, zentraler Ticker |
| `NavHost.kt`, `Screen.kt` | AniSync-Plus-Settingsroute |
| `SettingsListDetail.kt`, `SettingsScreen.kt` | Kategorie in beiden Layouts |
| `values/strings.xml` und `values-{ar,de,es,fa,fr,pt,pt-rBR,ru,ta}/strings.xml` | Feature-/Statusstrings und Übersetzungen |
| Root `build.gradle.kts`, `gradle/libs.versions.toml`, `settings.gradle.kts` | Android-Library-Modultoolchain und Include |
| `gradle.properties` | 4-GiB-Heap nach beobachtetem Packaging-OOM |
| `gradlew` | ausführbares Wrapper-Bit |
| `docs/anisync-plus-forensic-analysis.md`, `docs/upstream-integration-points.md` | falsche Baselinebehauptungen korrigiert und Istintegration dokumentiert |

### Neue isolierte Bereiche

- Gesamtes `:anisyncplus-calendar` Modul inklusive API, Domain, Network, Parser, Matching, Room, Repository, Schema, Tests und 15 lokalen Fixtures.
- Neue Appadapter, Hilt-Modul, Settingsscreen/ViewModel und gezielte App-Tests.
- Neue PR-/Push-CI und Lintbaseline.
- Historischer Audit und aktualisierte Architektur-/Parser-/Implementierungsdokumente.

Breite Refactors, Packageverschiebungen und Änderungen an `anisync.db` wurden vermieden. AniList-Airingfelder bleiben für Nichtzielbereiche bestehen.

## G. APK

- Pfad: `app/build/outputs/apk/stable/debug/AniSyncPlus-v3.1.0-debug-universal-debug.apk`
- Größe: 43.420.653 Bytes
- SHA-256: `68ad0bdf8a9777d4634bdb9729d154fcc9c741182dbb490b51f4209708783597`
- Package: `de.mrxxxxx.anisyncplus.debug`
- Version: `19 / 3.1.0-debug`
- minSdk/targetSdk: `26 / 36`
- Label: `AniSync Plus Debug`
- Kanal: Debug, ausdrücklich kein updatefähiger Releasekanal
