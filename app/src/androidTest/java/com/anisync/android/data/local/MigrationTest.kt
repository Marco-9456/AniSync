package com.anisync.android.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for database migrations.
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 *                              HOW TO USE THIS FILE
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 1. When you add a new migration, create a test method following the pattern below
 * 2. Run these tests on an actual device/emulator (they're instrumented tests)
 * 3. The tests verify:
 *    - Migration SQL is syntactically correct
 *    - Data survives the migration
 *    - Final schema matches what Room expects
 *
 * Run with: ./gradlew connectedAndroidTest
 * Or run individual test from Android Studio
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    /**
     * MigrationTestHelper creates a real SQLite database for testing.
     * It validates that:
     * - The migration runs without errors
     * - The resulting schema matches Room's expected schema
     */
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    /**
     * Verifies that a fresh database can be created at version 1.
     * This is the baseline test - should always pass.
     */
    @Test
    fun createDatabase_version1() {
        // Create database at version 1
        helper.createDatabase(TEST_DB, 1).apply {
            // Database created successfully
            close()
        }
    }

    /**
     * v22 to v23 gave `airing_schedule` an `ownerId` in its primary key.
     *
     * This one matters more than most: `DatabaseModule` still builds with
     * `fallbackToDestructiveMigration`, so a migration that does not produce exactly the schema
     * Room expects wipes the user's data rather than crashing. `runMigrationsAndValidate` is the
     * check that would otherwise never happen.
     */
    @Test
    fun migrate22To23_scopesAiringScheduleByOwner() {
        helper.createDatabase(TEST_DB, 22).apply {
            execSQL(
                """
                INSERT INTO airing_schedule (
                    id, mediaId, airingAt, episode, titleUserPreferred,
                    coverUrl, format, isWatching, streamingSeriesUrl
                ) VALUES (1, 100, 1700000000, 3, 'Test Anime', NULL, 'TV', 1, NULL)
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            23,
            true,
            Migrations.MIGRATION_22_23
        )

        // The existing row survives, parked under the signed-out owner until the first reconcile
        // claims it. Dropping it instead would leave upgrading users with blank widgets until a
        // full network refresh completed.
        db.query("SELECT ownerId, titleUserPreferred FROM airing_schedule WHERE id = 1").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(-1, cursor.getInt(0))
            assertEquals("Test Anime", cursor.getString(1))
        }

        // Two accounts have to be able to hold the same schedule id with different watching flags.
        db.execSQL(
            """
            INSERT INTO airing_schedule (
                id, ownerId, mediaId, airingAt, episode, titleUserPreferred,
                coverUrl, format, isWatching, streamingSeriesUrl
            ) VALUES
                (1, 10, 100, 1700000000, 3, 'Test Anime', NULL, 'TV', 1, NULL),
                (1, 20, 100, 1700000000, 3, 'Test Anime', NULL, 'TV', 0, NULL)
            """.trimIndent()
        )

        db.query(
            "SELECT ownerId, isWatching FROM airing_schedule WHERE id = 1 AND ownerId > 0 ORDER BY ownerId"
        ).use { cursor ->
            assertEquals(2, cursor.count)
            cursor.moveToFirst()
            assertEquals(10, cursor.getInt(0))
            assertEquals(1, cursor.getInt(1))
            cursor.moveToNext()
            assertEquals(20, cursor.getInt(0))
            assertEquals(0, cursor.getInt(1))
        }
    }

    /**
     * v24 to v25 gave `library_entries` its volume progress and per-category scores.
     *
     * Both columns are auto-migrated with defaults, so the point of the test is that the defaults
     * actually land: an entry saved before the upgrade has to come back with no volume progress and
     * an empty score map rather than a null the JSON converter would throw on.
     */
    @Test
    fun migrate24To25_defaultsVolumeProgressAndAdvancedScores() {
        helper.createDatabase(TEST_DB, 24).apply {
            execSQL(
                """
                INSERT INTO library_entries (
                    id, mediaId, titleUserPreferred, progress, status, rewatches, lastUpdated
                ) VALUES (1, 100, 'Test Anime', 5, 'CURRENT', 0, 1700000000)
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 25, true)

        db.query(
            "SELECT progressVolumes, advancedScores, progress FROM library_entries WHERE id = 1"
        ).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(true, cursor.isNull(0))
            assertEquals("{}", cursor.getString(1))
            assertEquals(5, cursor.getInt(2))
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //                         MIGRATION TEST TEMPLATES
    // ═══════════════════════════════════════════════════════════════════════════

    // ─────────────────────────────────────────────────────────────────────────────
    // TEMPLATE: Basic migration test (uncomment and modify when needed)
    // ─────────────────────────────────────────────────────────────────────────────
    //
    // @Test
    // fun migrate1To2() {
    //     // Step 1: Create database at the starting version
    //     helper.createDatabase(TEST_DB, 1).apply {
    //         // Insert test data at version 1
    //         execSQL("""
    //             INSERT INTO library_entries (
    //                 id, mediaId, titleUserPreferred, progress, status, lastUpdated
    //             ) VALUES (
    //                 1, 100, 'Test Anime', 5, 'WATCHING', ${System.currentTimeMillis()}
    //             )
    //         """.trimIndent())
    //         close()
    //     }
    //
    //     // Step 2: Run the migration
    //     helper.runMigrationsAndValidate(
    //         TEST_DB,
    //         2,                          // Target version
    //         true,                       // Validate dropped tables
    //         Migrations.MIGRATION_1_2    // The migration to test
    //     )
    //
    //     // Step 3: Verify data survived (optional but recommended)
    //     // You can open the database and query the data
    // }

    // ─────────────────────────────────────────────────────────────────────────────
    // TEMPLATE: Test with data verification (uncomment when needed)
    // ─────────────────────────────────────────────────────────────────────────────
    //
    // @Test
    // fun migrate2To3_dataPreserved() {
    //     // Create database with test data
    //     helper.createDatabase(TEST_DB, 2).apply {
    //         execSQL("""
    //             INSERT INTO library_entries (id, mediaId, titleUserPreferred, score)
    //             VALUES (1, 100, 'Test', 8.5)
    //         """.trimIndent())
    //         close()
    //     }
    //
    //     // Run migration
    //     val db = helper.runMigrationsAndValidate(TEST_DB, 3, true, Migrations.MIGRATION_2_3)
    //
    //     // Verify data
    //     val cursor = db.query("SELECT * FROM library_entries WHERE id = 1")
    //     cursor.use {
    //         assert(it.moveToFirst()) { "Data should exist after migration" }
    //         val scoreIndex = it.getColumnIndex("score")
    //         val score = it.getDouble(scoreIndex)
    //         assert(score == 8.5) { "Score should be preserved. Got: $score" }
    //     }
    // }

    // ─────────────────────────────────────────────────────────────────────────────
    // TEMPLATE: Test entire migration chain (useful before releases)
    // ─────────────────────────────────────────────────────────────────────────────
    //
    // @Test
    // fun migrateAll_from1ToLatest() {
    //     // Start at version 1
    //     helper.createDatabase(TEST_DB, 1).apply {
    //         // Insert baseline data
    //         execSQL("INSERT INTO library_entries (...) VALUES (...)")
    //         close()
    //     }
    //
    //     // Run all migrations to latest version
    //     helper.runMigrationsAndValidate(
    //         TEST_DB,
    //         LATEST_VERSION,  // Define this constant
    //         true,
    //         *Migrations.ALL_MIGRATIONS
    //     )
    // }

    companion object {
        private const val TEST_DB = "migration-test"

        // Update this when you increment the database version
        // private const val LATEST_VERSION = 1
    }
}
