package com.anisync.android.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
