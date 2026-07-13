# AniSync Plus – Forensisches Audit nach Phase 1

**Auditdatum:** 13. Juli 2026  
**Prüfobjekt:** `xxxxxxxxxxssxx/AniSyncPlus`, veröffentlichter Stand von `main`  
**Upstream:** `Marco-9456/AniSync`  
**Auditstatus:** **NICHT FÜR DIE FEATURE-IMPLEMENTIERUNG FREIGEGEBEN**, bis die in Abschnitt 12 genannten Entscheidungen und Planfehler korrigiert sind.  
**Produktive Änderungen:** Keine.

---

## 1. Management Summary

Der zentrale Befund ist eindeutig:

> Die getestete APK enthält die gewünschten AniSync-Plus-Funktionen nicht, weil im veröffentlichten `main`-Branch kein AniWorld-Featurecode implementiert wurde.

Zwischen dem Upstream-Baseline-Commit `8f1029db05b09dc612d3af061a01b0e0774d1531` und dem geprüften Fork-Commit `1c69ce5ce5ed6fd6c4ebfdd213245c5dabc38e93` wurden ausschließlich zwei GitHub-Actions-Workflows sowie sechs Dokumentationsdateien geändert oder hinzugefügt. Es gibt keine Änderung an Kotlin-, Compose-, Room-, Hilt-, GraphQL-, Manifest- oder App-Gradle-Dateien.

Die Aussage, alle Pull Requests enthielten die Funktionen, ist anhand der Pull-Request-Dateilisten widerlegt:

- PR #1: ausschließlich `AGENTS.md`, `ProjectContext.md` und vier Dateien unter `docs/`
- PR #2: zwei Workflow-Dateien und `ProjectContext.md`
- PR #3: zwei Workflow-Dateien und `ProjectContext.md`

Der aktuelle Code verwendet weiterhin AniList für Kalender, Library-Countdown, Episodennummer, Airing-Soon-Sortierung und WorkManager-Airing-Daten.

Der Beta-Log bestätigt keinen Featurefehler im engeren Sinn. Er zeigt vielmehr:

- Start der unveränderten Debug-App `com.anisync.android.debug`
- erfolgreiche Ausführung des vorhandenen AniList-basierten `AiringScheduleWorker`
- keine AniWorld-, Parser-, Matching-, Snapshot- oder Cache-Protokolle
- keinen App-Absturz
- wiederholte Main-Thread-Blockaden mit bis zu 263 übersprungenen Frames

Phase 1 war laut eigener Dokumentation absichtlich dokumentationsorientiert. Das Problem ist daher nicht, dass implementierte AniWorld-Funktionen defekt sind, sondern dass eine Dokumentations-/CI-Phase als Feature-Beta interpretiert oder ausgeliefert wurde.

---

## 2. Prüfgrundlage und Einschränkungen

### 2.1 Geprüfte Quellen

- GitHub-Repository-Metadaten
- Commit-Historie und Vergleich Baseline → Fork-`main`
- Pull Requests #1, #2 und #3 einschließlich geänderter Dateien
- Phase-1-Dokumente:
  - `AGENTS.md`
  - `ProjectContext.md`
  - `docs/anisync-plus-forensic-analysis.md`
  - `docs/anisync-plus-architecture.md`
  - `docs/upstream-integration-points.md`
  - `docs/aniworld-parser-contract.md`
- relevanter Android-Code in Kalender, Library, Room, Hilt, Gradle und GitHub Actions
- bereitgestellter Logcat:
  - `AniSync Debug log b38cbd29d12e.txt`
- aktuell ausgelieferter Inhalt des AniWorld-Animekalenders am 13.07.2026

### 2.2 Nicht reproduzierbarer lokaler Git-Worktree

Der verlangte lokale Git-Block wurde versucht, konnte in der Auditumgebung aber nicht abgeschlossen werden:

```bash
git clone --no-tags https://github.com/xxxxxxxxxxssxx/AniSyncPlus.git /tmp/AniSyncPlusAudit
```

Fehler:

```text
fatal: unable to access 'https://github.com/xxxxxxxxxxssxx/AniSyncPlus.git/':
Could not resolve host: github.com
```

Folgen:

- `git status`
- `git branch --show-current`
- `git remote -v`
- `git log -n 10 --oneline --decorate`
- `git diff --stat`
- `git diff`

konnten nicht an einem lokalen Checkout ausgeführt werden.

Der veröffentlichte Remote-Stand wurde stattdessen über GitHub-Repository-, Commit-, Compare-, PR- und Datei-APIs verifiziert. Ein unbekannter lokaler, nicht gepushter Worktree-Zustand kann damit nicht bewertet werden.

**Klassifikation:** fehlende Information / Umgebungsbeschränkung, kein Repositoryfehler.

---

## 3. Repository- und Pull-Request-Zustand

### 3.1 Verifizierter Remote-Stand

- Fork: `xxxxxxxxxxssxx/AniSyncPlus`
- Default-Branch: `main`
- Sichtbarkeit: **public**
- geprüfter `main`-Commit: `1c69ce5ce5ed6fd6c4ebfdd213245c5dabc38e93`
- Upstream-`main`: `8f1029db05b09dc612d3af061a01b0e0774d1531`
- Fork ist gegenüber dieser Baseline fünf Commits voraus und nicht dahinter.

### 3.2 Verifizierte Änderungen gegenüber Upstream-Baseline

Nur folgende Dateien unterscheiden sich:

```text
.github/workflows/build-manual-release.yml
.github/workflows/build-release.yml
AGENTS.md
ProjectContext.md
docs/anisync-plus-architecture.md
docs/anisync-plus-forensic-analysis.md
docs/aniworld-parser-contract.md
docs/upstream-integration-points.md
```

Keine der folgenden Kategorien wurde geändert:

- Kotlin-/Compose-Code
- Hilt-Module
- Room-Entities, DAOs oder Datenbank
- GraphQL-Operationen
- AndroidManifest
- `settings.gradle.kts`
- `app/build.gradle.kts`
- Ressourcen oder Strings
- Tests
- AniWorld-Client oder Parser

### 3.3 PR-Inventar

| PR | Status | Tatsächlicher Inhalt | Featurecode |
|---|---|---|---|
| #1 | gemergt | sechs Dokumentationsdateien | nein |
| #2 | offen, nicht gemergt | zwei Workflows, `ProjectContext.md` | nein |
| #3 | gemergt | zwei Workflows, `ProjectContext.md` | nein |

**Verifizierter Fehler:** Die Annahme, die Pull Requests enthielten die gewünschten Features, ist falsch.

**Zusatzrisiko:** PR #2 ist ein nicht gemergter, weitgehend überholter Parallelansatz zu PR #3. Er sollte vor weiterer Arbeit geschlossen oder ausdrücklich als verworfen dokumentiert werden, um versehentliche Doppelmerges zu verhindern.

---

## 4. Hauptursache der nicht funktionierenden Beta

### 4.1 Kein AniSync-Plus-Modul

`settings.gradle.kts`, Zeilen 18–19:

```kotlin
rootProject.name = "AniSync"
include(":app")
```

Das in Phase 1 vorgeschlagene Modul `:anisyncplus-calendar` existiert nicht.

### 4.2 Keine AniSync-Plus-Identität

`app/build.gradle.kts`:

- Zeile 65: `namespace = "com.anisync.android"`
- Zeile 69: `applicationId = "com.anisync.android"`
- Zeile 80: Appname `AniSync`
- Zeile 126: APK-Basisname `AniSync`
- Zeilen 177–182: Debug-App `com.anisync.android.debug`, Name `AniSync Debug`

Die Beta ist damit technisch weiterhin eine AniSync-Debugvariante, nicht eine separat installierbare AniSync-Plus-App mit eigener Basis-ID.

### 4.3 Kalender verwendet weiterhin AniList

`app/src/main/java/com/anisync/android/data/CalendarRepositoryImpl.kt`:

- Klasse `CalendarRepositoryImpl`, Zeilen 17–20
- Apollo-Abfrage `AiringScheduleQuery`, Zeilen 33–42
- Mapping von AniList-Feldern, Zeilen 44–71
- Sortierung nach AniList-`airingAt`, Zeile 78

`app/src/main/java/com/anisync/android/di/RepositoryModule.kt`, Zeilen 102–105:

```kotlin
@Binds
abstract fun bindCalendarRepository(
    impl: CalendarRepositoryImpl
): CalendarRepository
```

Es existiert keine alternative AniWorld-Bindung.

### 4.4 Kalenderfilter ist weiterhin „auf irgendeiner AniList-Liste“

`CalendarViewModel.kt`, Zeilen 244–254:

```kotlin
val visible = if (followingOnly) episodes.filter { it.isOnList } else episodes
```

Das ist nicht das Zielverhalten „nur sicher gematchte CURRENT/Watching-Einträge“.

### 4.5 Kein Offline-Kalendercache

`CalendarViewModel.loadWeek()` ruft direkt `calendarRepository.getWeekSchedule()` auf. Bei Fehlern werden `rawEpisodes` und sichtbare Tage geleert, statt den letzten erfolgreichen AniWorld-Snapshot beizubehalten:

- Erfolgsweg: Zeilen 115–125
- Fehlerweg: Zeilen 128–137

Für den 42-Tage-Monatsbereich gilt dasselbe:

- Abruf: Zeilen 172–176
- Fehler löscht Daten: Zeilen 195–205

### 4.6 Library verwendet weiterhin AniList-Airing-Daten

`LibraryRepositoryImpl.kt`, Zeilen 163–166:

```kotlin
nextAiringEpisode = media?.nextAiringEpisode?.episode
timeUntilAiring = media?.nextAiringEpisode?.timeUntilAiring
nextAiringEpisodeTime = media?.nextAiringEpisode?.airingAt?.toLong()
```

`LibraryViewModel.kt`, Zeilen 250–266:

- `AIRING_SOON` sortiert nach `entry.nextAiringEpisodeTime`.

`LibraryMediaCard.kt`, Zeilen 324–340:

- Rückstandsbadge wird aus `nextAiringEpisode - 1` berechnet.
- Countdown wird aus `dynamicTimeUntilAiring` und `nextAiringEpisode` erzeugt.

Keine dieser Stellen nutzt AniWorld.

### 4.7 Kein AniWorld-Room-Schema

`AppDatabase.kt`, Zeilen 90–121:

- Version 22
- ausschließlich bestehende Upstream-Entities
- keine Tabellen:
  - `aniworld_release_entries`
  - `aniworld_media_mappings`
  - `aniworld_sync_state`

`DatabaseModule.kt`, Zeilen 29–48:

- Datenbankname bleibt `anisync.db`.
- keine zweite Room-Datenbank.

### 4.8 Keine AniSync-Plus-Einstellungen

Die Settings-Liste enthält Look & Feel, AniList, Notifications, Storage, Media Upload, App Links, Updates, Sponsors, About und ggf. Developer Tools. Eine AniSync-Plus-Kategorie existiert nicht.

---

## 5. Auswertung des Beta-Logs

### 5.1 Verifizierte Laufzeitidentität

Log-Header:

```text
package: com.anisync.android.debug:19, targetSdk 36
```

Das stimmt exakt mit der unveränderten Gradle-Konfiguration überein.

### 5.2 Kein Absturz

Im vollständigen Log wurden keine Treffer gefunden für:

- `FATAL EXCEPTION`
- `AndroidRuntime`
- App-seitige unbehandelte Exception
- Room-Migrationsfehler
- Hilt-Bindingfehler

Die Aktivität wird am Ende regulär pausiert, gestoppt und zerstört.

### 5.3 Kein AniWorld-Datenpfad

Keine Treffer für:

- `AniWorld`
- `animekalender`
- `DE-Sub`
- `DE-Dub`
- Parser
- Snapshot
- Mapping
- Effective Release

Dies ist mit dem Repositorycode konsistent: Ein entsprechender Datenpfad existiert nicht.

### 5.4 Worker-Erfolg beweist nur AniList-Funktion

Logzeilen 129 und 139:

```text
Starting work for com.anisync.android.worker.AiringScheduleWorker
```

Logzeilen 196 und 210:

```text
Worker result SUCCESS ... AiringScheduleWorker
```

`AiringScheduleWorker.kt` führt jedoch `AiringScheduleQuery` gegen AniList aus und ersetzt die vorhandene Tabelle `airing_schedule`. Der Erfolg dieses Workers sagt nichts über AniWorld aus.

### 5.5 Performanceproblem

Der Log enthält zahlreiche Meldungen:

```text
Skipped 30–51 frames
```

Am Ende:

```text
Skipped 263 frames!
```

**Bewertung:** reales Laufzeit-/Responsiveness-Risiko. Der Log beweist aber nicht, dass AniWorld die Ursache ist; AniWorld-Code existiert nicht. Debug-Build, LeakCanary, Compose-Recomposition, Bildverarbeitung oder bestehende Netzwerk-/UI-Pfade sind mögliche Ursachen. Dafür ist ein separater Perfetto-/Macrobenchmark-/Compose-Recomposition-Audit erforderlich.

### 5.6 Nicht ursächliche Chromium-Warnung

Die einzige klassische `E`-Zeile betrifft Chromium PartitionAlloc und deaktiviertes MTE. Das ist kein Beleg für den fehlenden AniWorld-Funktionsumfang.

---

## 6. Fehler und Widersprüche in der Phase-1-Dokumentation

### F-01 – Falsche Aussage zu GitHub Actions

`docs/anisync-plus-forensic-analysis.md`, Zeile 21:

> keine CI-Build-Workflow-Datei gefunden

`docs/upstream-integration-points.md`, Zeile 30:

> `.github/workflows/*` – Issue templates only; no CI workflow

Tatsächlich existierten `build-manual-release.yml` und `build-release.yml` bereits und wurden in Phase 1 modifiziert.

**Klassifikation:** verifizierter Dokumentationsfehler.  
**Auswirkung:** falsche Baseline, erschwert Ursachenzuordnung und Upstream-Diff.  
**Korrektur:** „vorhandene, aber ungetestete Build-Workflows“ dokumentieren und Baseline-Diff beilegen.

### F-02 – Falscher Dateipfad `SettingsCategory.kt`

`docs/upstream-integration-points.md`, Zeile 28 nennt:

```text
app/src/main/java/com/anisync/android/presentation/settings/SettingsCategory.kt
```

Diese Datei existiert im geprüften Branch nicht.

**Klassifikation:** verifizierter falscher Pfad.  
**Korrektur:** tatsächlichen Deklarationsort von `SettingsCategory` ermitteln und dokumentieren; bis dahin nicht als Integration Point behaupten.

### F-03 – Settings-Reihenfolge unvollständig

Die Analyse nennt:

```text
Look & Feel, AniList, Notifications, Storage, Media Upload, Updates, Sponsors, About
```

Der tatsächliche Code enthält zusätzlich **App Links** zwischen Media Upload und Updates (`SettingsScreen.kt`, Zeilen 192–211).

**Klassifikation:** verifizierter Dokumentationsfehler.  
**Auswirkung:** geplanter Einfügepunkt und Screenshots stimmen nicht vollständig mit der UI-Struktur überein.

### F-04 – „Privater Fork“ widerspricht Repositorysichtbarkeit

`ProjectContext.md`, Zeile 6 nennt einen privaten Fork. GitHub meldet das Repository als **public**.

**Klassifikation:** verifizierter Kontextfehler oder nicht umgesetzte Sicherheitsentscheidung.  
**Auswirkung:** Quellcode, Workflowdefinitionen und versehentlich eingecheckte Testfixtures wären öffentlich.

### F-05 – Room-Testabdeckung wird überschätzt

`MigrationTest.kt` ist ein Instrumentation-Test. Aktiv ist im Wesentlichen nur das Erzeugen einer Datenbankversion 1; die eigentlichen Migrationstests sind kommentierte Vorlagen.

**Klassifikation:** verifizierte Testlücke, überwiegend vor Phase 1 vorhanden.  
**Auswirkung:** Hinweise wie „MigrationTest.kt verifies all migrations“ sind faktisch nicht erfüllt.

### F-06 – Airing-Soon-Index missverständlich beschrieben

`LibraryEntryEntity` besitzt einen Index auf `timeUntilAiring`. Die aktuelle `AIRING_SOON`-Sortierung erfolgt aber nach Laden der Daten im ViewModel anhand von `nextAiringEpisodeTime`.

**Klassifikation:** dokumentarische Ungenauigkeit.  
**Auswirkung:** Der Index optimiert die gezeigte Sortierung nicht.

### F-07 – DOM-Selektoren in diesem Audit nicht vollständig unabhängig reproduziert

Die sichtbaren Kalenderinhalte, Datumsbereiche, Titel, Episodentokens, Zeiten und Links wurden verifiziert. Der verfügbare Text-Renderer lieferte jedoch nicht den rohen HTML-Attributbaum für alle behaupteten Selektoren und Flaggenpfade.

**Klassifikation:** unbestätigte Annahme, nicht widerlegter Fehler.  
**Korrektur:** einen lokalen, bereinigten HTML-Snapshot mit Herkunftsdatum und Hash als Testfixture archivieren; Parservertrag dagegen testen.

---

## 7. AniWorld-Befunde und Planlücken

### 7.1 Quellbereich ist nicht eine frei abfragbare Woche oder ein Monat

Am 13.07.2026 liefert die Seite Daten von:

- Montag, 13.07.2026
- bis Montag, 27.07.2026

also einen rollierenden 15-Tage-Bereich.

Die aktuelle AniSync-UI unterstützt dagegen:

- vorherige/nächste Woche
- beliebige Wochen
- vorherigen/nächsten Monat
- einen 42-Tage-Monatsgrid

`ProjectContext.md` fordert zugleich, exakt den aktuell gelieferten AniWorld-Bereich zu speichern und nicht auf AniList zurückzufallen.

Diese drei Anforderungen sind gleichzeitig nicht erfüllbar, ohne ein definiertes Verhalten außerhalb des AniWorld-Bereichs.

**Release-Blocker:** Produktentscheidung erforderlich:

1. Navigation außerhalb des gecachten Bereichs sperren,
2. leere Tage mit expliziter Bereichsmeldung zeigen,
3. Snapshots historisch akkumulieren,
4. oder entgegen dem bisherigen Verbot einen AniList-Fallback zulassen.

### 7.2 Parsermodell kennt Filme/Specials nicht ausreichend

Der Live-Kalender enthält beispielsweise:

```text
SHIBOYUGI ... Film 01 ~ 23:59 Uhr
```

Der Vertrag beschreibt primär `S01E03`.

**Erforderliche Modellkorrektur:**

```text
releaseKind = EPISODE | FILM | SPECIAL | UNKNOWN
seasonNumber: Int?
episodeNumber: Int?
installmentNumber: Int?
rawInstallmentToken: String
```

Ohne diese Trennung gehen Filme verloren oder werden mit künstlichen Episodennummern falsch modelliert.

### 7.3 Gleiche Serie und Episode können mehrere Zeiten haben

Beispiele im Live-Kalender:

- `DIGIMON BEATBREAK S01E39` um 09:10 und 13:10
- `One Piece S23E15` um 18:10 und 23:10

Die Konsolidierung darf deshalb nie nur nach Titel, Staffel und Episode erfolgen. Sprache/Releasevariante und Zeitpunkt müssen Teil der Identität bleiben.

### 7.4 Gleiche Serie und Zeit können unterschiedliche Episoden haben

Beispiele:

- `Crowned in a Hundred Days` E07 und E08 um 06:10
- `That Time I Got Reincarnated as a Slime` E15 und E12 um 17:10

Der Parservertrag erwähnt diesen Fall, benötigt aber zwingend Fixture-Tests mit realen Varianten.

### 7.5 Kalenderlink ist keine verlässliche Episodenidentität

Ein Link kann:

- Staffel und Episode enthalten,
- nur zur Serienwurzel zeigen,
- oder nicht mit der im Kalender gezeigten Episodenzahl übereinstimmen.

Beispiel: Der Kalender nennt `DIGIMON BEATBREAK S01E39`; die verlinkte Serienseite zeigt im sichtbaren Episodenindex nur 1–12. Der Link darf daher nur als Quellslug/Navigationseigenschaft, nicht als alleinige Staffel-/Episodenwahrheit verwendet werden.

### 7.6 „Alle AniWorld-Einträge“ widerspricht der Sprachregel

Die Filterdefinition „Filter aus = alle AniWorld-Einträge“ kollidiert mit „sichtbar nur DE-Sub/DE-Dub“.

Korrekte Formulierung:

> Filter aus = alle **für V1 sichtbaren deutschen Releases**, einschließlich ungematchter Titel.  
> Filter an = nur sicher gematchte deutsche Releases mit aktivem AniList-Status CURRENT.

Englisch-only- oder UNKNOWN-Einträge bleiben diagnostisch gespeichert, aber nicht Teil der sichtbaren V1-Liste.

### 7.7 Unklare Semantik des `~`

Das Zeichen `~` kennzeichnet eine ungefähre Uhrzeit. Der Plan definiert nicht:

- ob Countdown sekundengenau angezeigt werden darf,
- wie Airing-Soon-Sortierung bei ungefähren Zeiten behandelt wird,
- ob verspätete Releases nach Überschreiten sofort verschwinden,
- oder ob eine Toleranz-/Nachlaufzeit gilt.

Das muss als Produkt- und Domänenregel festgelegt werden.

---

## 8. Titel-Matching: fehlender Datenpfad und falsche Schlüssigkeit

### 8.1 Fehlender AniList-Kandidatenbestand

AniWorld liefert keinen AniList-Media-ID-Wert. Für Kalenderkarten werden aber AniList-ID, Cover, Titelpräferenz und Detailnavigation benötigt.

Die vorgeschlagene Architektur benennt einen Matcher, aber nicht eindeutig, woher die Kandidaten für **alle** Kalenderreleases kommen.

Nur die lokale Library reicht nicht:

- Filter aus soll auch Titel außerhalb der Library zeigen.
- Unmatched-Titel sollen sichtbar bleiben.
- Cover und Navigation benötigen trotzdem eine Identitätsstrategie.

Erforderliche Entscheidung:

- AniList-AiringSchedule darf verdeckt als Identitäts-/Metadatenkandidat genutzt werden, ohne dessen Termine sichtbar zu verwenden,
- oder AniList-Suche wird titelweise/batchweise ausgeführt,
- oder es gibt eine kuratierte lokale Mappingbasis.

Ohne diesen Pfad ist die Ziel-UI nicht vollständig realisierbar.

### 8.2 Normalisierter Titel/Slug ist kein stabiler Primärschlüssel

Der vorgeschlagene Mappingkey aus normalisiertem Titel oder Slug kollidiert bei:

- Remakes
- mehreren Staffeln unter einem Serienroot
- gleichen oder wiederverwendeten Titeln
- Filmen/Specials
- Varianten mit Jahreszusatz
- AniWorld-Seiten, deren Kalenderstaffel nicht der Seitenstaffel entspricht

Empfehlung:

```text
sourceSeriesKey
sourceSeasonToken
normalizedTitleFingerprint
releaseKind
```

als Quellidentität; AniList-Mapping getrennt davon.

### 8.3 Fuzzy-Matching darf nicht sofort autoritativ werden

Ein vorsichtiges, unambiges Similarity-Matching ist im Plan zu unspezifisch.

Mindestens erforderlich:

- definierte Unicode-/Interpunktionsnormalisierung
- Artikel-/Untertitelregeln
- Staffel- und Jahresprüfung
- Kandidatenzahl und Score-Abstand
- Ambiguitätsschwelle
- Grund/Version des Matchers
- keine automatische dauerhafte `MATCHED`-Zuordnung bei bloßer Titelnähe

### 8.4 Accountabhängigkeit

Mappings AniWorld → AniList sind global. `CURRENT` ist dagegen kontoabhängig.

Der Resolver muss daher trennen:

- globaler Medienmatch
- aktives Benutzerkonto
- aktueller Library-Status aus dem account-scoped Room-Flow

Ein in der AniWorld-Datenbank gespeichertes `isWatching` wäre falsch und würde bei Kontowechsel veralten.

---

## 9. Zeitzonenmodell

### 9.1 Aktueller Upstream-Code

`CalendarViewModel` verwendet:

```kotlin
ZoneId.systemDefault()
```

Das ist für AniList-UTC-Zeitstempel sinnvoll als Anzeigezone, aber nicht automatisch korrekt für AniWorld-Quelltext.

### 9.2 Erforderliche Trennung

Persistiert werden sollten:

```text
sourceDate
sourceLocalTime
sourceZoneId
isApproximate
resolvedInstant
resolutionPolicyVersion
```

Empfehlung für V1, sofern bestätigt:

```text
sourceZoneId = Europe/Berlin
```

### 9.3 Noch zu definieren

- Gruppierung nach AniWorld-Quelltag oder Benutzerlokaltag?
- Verhalten in DST-Lücken
- Verhalten bei doppelten Uhrzeiten während der Zeitumstellung
- Umgang mit fehlender Uhrzeit
- Neuberechnung bestehender Instants bei Parser-/Zeitzonenregeländerung

Für den aktuellen Nutzer in Europe/Berlin fallen Quell- und Anzeigezone zusammen. Das darf aber nicht implizit im Datenmodell versteckt werden.

---

## 10. Offline-Cache und Refresh-Semantik

### 10.1 Positive Plananteile

- separate Room-Datenbank reduziert Änderungen am Upstream-Schema
- Snapshot-Ersatz in einer Transaktion ist richtig
- alter Snapshot darf vor erfolgreicher Validierung nicht gelöscht werden
- Parser-Tests dürfen keine Live-Daten abrufen

### 10.2 Kritischer Planfehler: Matching als Voraussetzung für Snapshotannahme

`ProjectContext.md` formuliert die Ersetzung erst nach erfolgreichem Parsing, Validieren und Matching.

Ein einzelner ambiger Titel darf aber nicht verhindern, dass alle anderen aktuellen Releases gecacht werden.

Korrekte Pipeline:

```text
HTTP-Erfolg
→ strukturelles Parsing
→ Bereichs- und Plausibilitätsvalidierung
→ Konsolidierung
→ Snapshot speichern
→ Matching/enrichment separat und wiederholbar
```

Unmatched und ambiguous sind gültige Snapshotdaten, keine Parserfehler.

### 10.3 Sync-State außerhalb der Snapshottransaktion

`lastAttempt`, Fehlerdetails und HTTP-/Parserdiagnostik müssen auch dann aktualisiert werden können, wenn der Snapshot-Ersatz abgebrochen wird. Sie sollten nicht vollständig in derselben Rollback-Transaktion verschwinden.

### 10.4 „Absichtlich leer“ braucht eine formale Regel

Ein leerer Snapshot darf nur akzeptiert werden, wenn mindestens:

- erwarteter Kalendercontainer erkannt wurde,
- gültige Datumssektionen erkannt wurden,
- der HTTP-Inhalt nicht als Block-/Fehlerseite klassifiziert wurde,
- Parserversion und Strukturprüfung erfolgreich sind,
- und die Abwesenheit deutscher Releases plausibel ist.

„HTTP 200 + null Einträge“ reicht nicht.

### 10.5 Mapping-Lebensdauer

Mappings müssen Snapshotwechsel überleben. Releasezeilen dürfen gelöscht/ersetzt werden; bestätigte oder manuell reservierte Mappings nicht.

---

## 11. Gradle-, Hilt-, Room- und CI-Risiken

### 11.1 Neues Modul ist noch nicht konfigurierbar

Die Version Catalog enthält nur:

```toml
android-application = ...
```

Keinen Alias für `com.android.library`.

Für `:anisyncplus-calendar` fehlen mindestens:

- Android-Library-Plugin
- Namespace
- compileSdk/minSdk
- Kotlin-Android-Konfiguration
- KSP-/Room-Schemaausgabe
- Consumer-ProGuard-Regeln, falls nötig
- Testdependencies
- Hilt-Modul- und Aggregationsstrategie

Die Aussage „Modul hinzufügen“ ist daher ein Architekturvorschlag, kein kleiner mechanischer Schritt.

### 11.2 Risiko eines zu schweren Moduls

Ein einziges Android-Library-Modul mit Parser, Matcher, Room, Hilt, Settings und Repository ist stark gekoppelt.

Zwei sinnvolle Alternativen:

1. **Pure-Kotlin-Core** für Parser, Konsolidierung, Normalisierung und Matching; Android-Library für Room/Hilt/Netzwerk.
2. Zunächst sauber isolierte Packages in `:app`, danach Modulabtrennung, sobald Verträge stabil sind.

Für Upstream-Mergefähigkeit ist nicht automatisch „mehr Module“ gleich „weniger Konflikte“. Die Root-/Version-Catalog-/Buildkonfiguration wird dauerhaft fork-spezifisch.

### 11.3 Hilt-Mehrdeutigkeit

Wenn ein neuer Repositorytyp ebenfalls `CalendarRepository` bereitstellt, entsteht eine Duplicate-Binding-Gefahr. Besser:

- neuer eindeutiger Vertrag, z. B. `EffectiveReleaseRepository`
- oder Qualifier
- Upstream-`CalendarRepositoryImpl` unverändert lassen
- Umschaltung im ViewModel/Adapter hinter klarer Featurekonfiguration

### 11.4 Room-Teststrategie passt nicht zum Browserworkflow

Bestehende Room-Migrationstests sind Instrumentation-Tests und benötigen Emulator/Gerät.

Der aktuelle Workflow führt ausschließlich `assembleStableDebug` oder `assembleStableRelease` aus. Er führt weder aus:

- Unit-Tests
- Lint
- Instrumentation-Tests
- Room-Migrationstests

Erforderlich:

- entweder Robolectric-/JVM-Room-Tests,
- oder Emulatorjob mit `connectedStableDebugAndroidTest`,
- plus Unit- und Lint-Job.

### 11.5 Gradle-Versionswiderspruch im Workflow

Repository-Wrapper:

```text
Gradle 9.3.1
```

Workflow-Setup:

```yaml
gradle-version: 8.13
```

Da anschließend `./gradlew` ausgeführt wird, bestimmt normalerweise der Wrapper die Buildversion. Die zusätzliche 8.13-Konfiguration ist daher mindestens irreführend und kann Caching/Diagnose unnötig verkomplizieren.

### 11.6 Signierung verhindert zuverlässige Beta-Upgrades

Bei fehlenden Release-Secrets erzeugt jeder Workflowlauf einen neuen temporären PKCS12-Key.

Folge:

- eine APK aus Lauf A kann nicht zuverlässig durch eine APK aus Lauf B aktualisiert werden,
- Android verlangt bei gleicher Application ID denselben Signaturschlüssel,
- Testende müssen sonst deinstallieren und verlieren lokale Daten.

Auch Debug-Signing auf kurzlebigen Runnern ist ohne persistierten Debug-/Beta-Key nicht als stabiler Updatekanal spezifiziert.

**Planänderung:** ein persistenter privater Beta-Signierschlüssel und eine feste Beta-Application-ID müssen vor einer updatefähigen Beta festgelegt werden.

### 11.7 Signierprüfung ist unvollständig

Der Workflow validiert Storepass und Alias über `keytool -list`, aber nicht sicher den separaten Keypass. Ein falscher `KEY_PASSWORD` kann daher erst beim Gradle-Signierschritt auffallen.

### 11.8 Fehlende Artefaktprovenienz

Artefaktname:

```text
AniSyncPlus-debug-apk
```

enthält weder Commit-SHA noch Buildnummer. Der Logdateiname `b38cbd29d12e` konnte nicht als Commit des geprüften Repositorys aufgelöst werden.

Für reproduzierbare Betatests erforderlich:

- Commit-SHA in APK/Artifact-Name oder BuildConfig
- Workflow-Run-ID
- Buildtyp
- Signaturquelle/Fingerprint
- Quellbranch

---

## 12. Blockierende Produktentscheidungen

Vor Phase 2 müssen diese Fragen verbindlich beantwortet und in `ProjectContext.md` aufgenommen werden:

1. **Kalenderbereich:** Was zeigt die UI außerhalb des aktuell von AniWorld gelieferten 15-Tage-Bereichs?
2. **Navigation:** Bleiben Vor-/Zurück-Woche und 42-Tage-Monatsansicht bestehen, werden sie begrenzt oder mit Bereichshinweis leer?
3. **AniList-Matchingquelle:** Darf AniList AiringSchedule oder AniList Search verdeckt als Identitäts-/Coverquelle verwendet werden, solange AniList-Zeitwerte nicht sichtbar oder sortierend verwendet werden?
4. **Zeitzone:** Wird die AniWorld-Quelle fest als `Europe/Berlin` interpretiert?
5. **Anzeigegruppierung:** Nach AniWorld-Quelltag oder Benutzerlokaltag?
6. **Ungefähre Zeit:** Welche Countdown-/Sortiersemantik gilt für `~ HH:mm`?
7. **Filme/Specials:** Sind `Film 01`, Specials und Einträge ohne SxxExx Bestandteil von V1?
8. **Finale Application ID:** `de.mrxxxxx.anisyncplus` oder ein anderer endgültiger Wert?
9. **Repositorysichtbarkeit:** Soll der Fork tatsächlich privat sein?
10. **Beta-Signierung:** Welcher dauerhafte Schlüssel/Fingerprint definiert den Updatekanal?

---

## 13. Korrigierter Implementierungsplan

### Gate 0 – Phase-1-Dokumentation berichtigen

- falsche CI-Baseline korrigieren
- falschen `SettingsCategory.kt`-Pfad korrigieren
- App-Links-Kategorie ergänzen
- Repositorysichtbarkeit korrigieren
- PR #2 als überholt schließen
- aktuellen Stand ausdrücklich als „Phase-1-Baseline / keine Feature-Beta“ markieren

### Gate 1 – Produktverträge einfrieren

- Bereichs-/Navigationsverhalten
- Zeitzonenregel
- Film-/Special-Regel
- AniList-Metadatenquelle
- Approximate-Time-Regel
- finale App-ID und Signierstrategie

### Phase 2A – Parser-Core ohne Appintegration

- lokales, bereinigtes HTML-Fixture
- Fixture-Hash und Beobachtungsdatum
- keine Live-Anfrage in Tests/CI
- Tests für:
  - DE-Sub
  - DE-Dub
  - gemeinsame DE-Sub/DE-Dub-Zeit
  - unterschiedliche Sprachzeiten
  - gleiche Episode mit zwei Zeiten
  - zwei Episoden zur gleichen Zeit
  - Film/Special
  - fehlende Staffel/Episode
  - unbekannte Sprache
  - Strukturbruch/Blockseite
  - 15-Tage-Bereich

### Phase 2B – Identität und Matching

- Quellidentität getrennt vom AniList-Mapping
- definierter Kandidatenprovider
- deterministischer Normalizer
- Ambiguitäts- und Confidence-Regeln
- Matcher-Versionierung
- account-unabhängiges Mapping

### Phase 2C – Cache

- separate Room-Datenbank oder begründete Alternative
- Snapshot-Pipeline
- Fehler-/Sync-State
- Mappingpersistenz
- Transaktions- und Erhaltungstests
- formale Empty-Snapshot-Regel

### Phase 2D – Read-only-Integration

- AniWorld-Kalender zunächst hinter Featureflag
- sichtbare Cache-/Fehler-/Letzte-Aktualisierung-Diagnostik
- keine Änderung an Library-Sortierung in demselben Commit
- UI auf tatsächlich verfügbaren Quellbereich begrenzen

### Phase 2E – Accountfilter und Library

- CURRENT-Filter gegen aktives Konto
- effektiver nächster deutscher Release
- Countdown
- Rückstandsbadge mit definierter Episode-/Filmsemantik
- Airing-Soon-Sortierung
- keine AniList-Airing-Fallbacks in den Zieloberflächen

### Phase 2F – Branding und Releasekanal

- feste Application ID
- Name AniSync Plus
- Deep-Link-/Provider-/OAuth-Audit
- persistenter Beta-Signierschlüssel
- Upgrade-Test von Beta N auf Beta N+1

### Phase 2G – CI-Freigabe

Mindestens:

```text
testStableDebugUnitTest
lintStableDebug
assembleStableDebug
```

Zusätzlich eine reale Room-Teststrategie:

```text
connectedStableDebugAndroidTest
```

oder getestete Robolectric-Alternative.

Jedes APK-Artefakt erhält Commit-SHA, Run-ID, Buildtyp und Signaturfingerprint.

---

## 14. Freigabeentscheidung

### Nicht freigegeben

Die geplante Phase 2 darf nicht unverändert begonnen werden.

### Begründung

- Die aktuelle Beta enthält keinen Featurecode.
- Phase-1-Dokumentation enthält mehrere verifizierte Baseline-/Pfadfehler.
- Der AniWorld-Quellbereich widerspricht der bestehenden Kalendernavigation.
- Die AniList-Kandidatenquelle für Matching/Cover/Navigation ist nicht definiert.
- Filme/Specials und ungefähre Zeiten sind im Modell nicht ausreichend erfasst.
- Cacheannahme ist zu eng an erfolgreiches Matching gekoppelt.
- CI führt keine Tests oder Lint aus.
- die aktuelle Signierstrategie ist kein stabiler Beta-Updatekanal.

### Freigabebedingung

Phase 2A darf nach Korrektur der Dokumentation und verbindlicher Beantwortung der Blockerfragen als isolierte Parser-/Vertragsphase beginnen. Eine sichtbare Appintegration sollte erst nach Parser-, Matching- und Cachetests erfolgen.

---

## 15. Ursachenzuordnung

| Befund | Vor Phase 1 vorhanden | Durch Phase 1 verursacht | Endgültige Bewertung |
|---|---:|---:|---|
| AniWorld-Features fehlen | ja, weil nicht implementiert | nein; Phase 1 war dokumentationsorientiert | erwartbarer Stand, aber falsch als Feature-Beta verstanden |
| Kalender nutzt AniList | ja | nein | Upstream-Baseline |
| Library nutzt AniList-Airing | ja | nein | Upstream-Baseline |
| UI-Jank im Log | wahrscheinlich | kein Beleg | separate Performanceanalyse nötig |
| falsche CI-Baseline in Docs | nein | ja | Phase-1-Dokumentationsfehler |
| falscher Settings-Pfad | nein | ja | Phase-1-Dokumentationsfehler |
| temporärer CI-Signierschlüssel | Workflow in Phase 1 geändert | ja/verschärft | Beta-Upgrade-Risiko |
| fehlende Tests im Workflow | bereits keine Ausführung | in Phase 1 nicht behoben | Freigabelücke |
| Repository public statt „private“ | externer Zustand | Dokument nicht angepasst | Sicherheits-/Kontextwiderspruch |

---

**Ende des Audits.**
