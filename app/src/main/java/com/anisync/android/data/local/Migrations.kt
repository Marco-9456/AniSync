package com.anisync.android.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database migrations for AniSync.
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 *                              MIGRATION GUIDELINES
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 1. AUTO-MIGRATIONS (Simple Changes)
 *    ─────────────────────────────────
 *    Use Room's auto-migration for straightforward schema changes:
 *    - Adding new nullable columns (must have default value)
 *    - Adding new tables
 *    - Adding new indices
 *
 *    How to use:
 *    ```kotlin
 *    @Database(
 *        ...
 *        autoMigrations = [
 *            AutoMigration(from = 1, to = 2),
 *        ]
 *    )
 *    ```
 *
 * 2. MANUAL MIGRATIONS (Complex Changes)
 *    ────────────────────────────────────
 *    Write manual migrations for changes that Room can't auto-handle:
 *    - Renaming columns or tables
 *    - Changing column types
 *    - Data transformations
 *    - Removing columns (SQLite limitation - requires table recreation)
 *    - Splitting or merging tables
 *
 *    How to use:
 *    1. Create a Migration object below
 *    2. Add it to ALL_MIGRATIONS array
 *    3. Write a migration test in MigrationTest.kt
 *
 * 3. AUTO-MIGRATION WITH SPEC (Renames/Deletes)
 *    ──────────────────────────────────────────
 *    For auto-migrations that involve renames or deletes, create a spec:
 *    ```kotlin
 *    @RenameColumn(tableName = "table", fromColumnName = "old", toColumnName = "new")
 *    class AutoMigration2To3Spec : AutoMigrationSpec
 *    ```
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 *                              MIGRATION CHECKLIST
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Before adding a migration:
 * □ Increment database version in AppDatabase.kt
 * □ Decide: Auto-migration or Manual migration?
 * □ If manual: Add migration to ALL_MIGRATIONS array
 * □ Write migration test in MigrationTest.kt
 * □ Test on device with existing data
 * □ Update version history in AppDatabase.kt
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 */
object Migrations {

    // ═══════════════════════════════════════════════════════════════════════════
    //                           MIGRATION DEFINITIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * v22 to v23: account-scope `airing_schedule`.
     *
     * The table was global while `isWatching` was denormalised from whichever account happened to
     * be active, so switching accounts had to wipe it. `ownerId` joins the primary key, matching
     * how `library_entries` has been scoped since v18.
     *
     * Existing rows are carried over under [NO_OWNER] rather than dropped.
     *
     * Dropping them is tempting, since the table is a rebuildable cache and the old rows carry no
     * owner. It is also wrong: `AiringScheduleWorker` needs a network round trip over three pages
     * to refill it, so every upgrading user would open the app to blank widgets and wait. The rows
     * are kept and `AccountManager.reconcileActiveAccount` re-tags them to the signed-in account on
     * first start, which is the same thing v18 did for legacy `library_entries`.
     */
    val MIGRATION_22_23 = object : Migration(22, 23) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `airing_schedule` RENAME TO `airing_schedule_old`")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `airing_schedule` (" +
                    "`id` INTEGER NOT NULL, " +
                    "`ownerId` INTEGER NOT NULL, " +
                    "`mediaId` INTEGER NOT NULL, " +
                    "`airingAt` INTEGER NOT NULL, " +
                    "`episode` INTEGER NOT NULL, " +
                    "`titleUserPreferred` TEXT NOT NULL, " +
                    "`coverUrl` TEXT, " +
                    "`format` TEXT, " +
                    "`isWatching` INTEGER NOT NULL, " +
                    "`streamingSeriesUrl` TEXT, " +
                    "PRIMARY KEY(`id`, `ownerId`))"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_airing_schedule_ownerId_airingAt` " +
                    "ON `airing_schedule` (`ownerId`, `airingAt`)"
            )
            db.execSQL(
                "INSERT INTO `airing_schedule` (" +
                    "`id`, `ownerId`, `mediaId`, `airingAt`, `episode`, `titleUserPreferred`, " +
                    "`coverUrl`, `format`, `isWatching`, `streamingSeriesUrl`) " +
                    "SELECT `id`, $NO_OWNER, `mediaId`, `airingAt`, `episode`, " +
                    "`titleUserPreferred`, `coverUrl`, `format`, `isWatching`, " +
                    "`streamingSeriesUrl` FROM `airing_schedule_old`"
            )
            db.execSQL("DROP TABLE `airing_schedule_old`")
        }
    }

    /** Signed out owner, and where rows from the unscoped table wait. */
    private const val NO_OWNER = -1

    /**
     * Array of all manual migrations.
     * These are automatically applied by Room in order.
     *
     * Declared after the migrations it holds, because an `object` initialises top to bottom and a
     * forward reference would land a null in the array.
     */
    val ALL_MIGRATIONS: Array<Migration> = arrayOf(
        MIGRATION_22_23,
    )

    // ─────────────────────────────────────────────────────────────────────────────
    // EXAMPLE: Adding a new column (uncomment and modify when needed)
    // ─────────────────────────────────────────────────────────────────────────────
    //
    // val MIGRATION_1_2 = object : Migration(1, 2) {
    //     override fun migrate(db: SupportSQLiteDatabase) {
    //         // Add a new nullable column with default value
    //         db.execSQL("ALTER TABLE library_entries ADD COLUMN newColumn TEXT DEFAULT NULL")
    //     }
    // }

    // ─────────────────────────────────────────────────────────────────────────────
    // EXAMPLE: Adding a new table (uncomment and modify when needed)
    // ─────────────────────────────────────────────────────────────────────────────
    //
    // val MIGRATION_2_3 = object : Migration(2, 3) {
    //     override fun migrate(db: SupportSQLiteDatabase) {
    //         db.execSQL("""
    //             CREATE TABLE IF NOT EXISTS new_table (
    //                 id INTEGER PRIMARY KEY NOT NULL,
    //                 name TEXT NOT NULL,
    //                 created_at INTEGER NOT NULL DEFAULT 0
    //             )
    //         """.trimIndent())
    //
    //         // Add index if needed
    //         db.execSQL("CREATE INDEX IF NOT EXISTS index_new_table_name ON new_table (name)")
    //     }
    // }

    // ─────────────────────────────────────────────────────────────────────────────
    // EXAMPLE: Complex migration - recreating table to change column type
    // (SQLite doesn't support ALTER COLUMN, so we need to recreate the table)
    // ─────────────────────────────────────────────────────────────────────────────
    //
    // val MIGRATION_3_4 = object : Migration(3, 4) {
    //     override fun migrate(db: SupportSQLiteDatabase) {
    //         // 1. Create new table with desired schema
    //         db.execSQL("""
    //             CREATE TABLE library_entries_new (
    //                 id INTEGER PRIMARY KEY NOT NULL,
    //                 mediaId INTEGER NOT NULL,
    //                 score REAL DEFAULT 0.0,  -- Changed from INTEGER to REAL
    //                 -- ... other columns
    //             )
    //         """.trimIndent())
    //
    //         // 2. Copy data from old table to new table
    //         db.execSQL("""
    //             INSERT INTO library_entries_new (id, mediaId, score)
    //             SELECT id, mediaId, CAST(score AS REAL) FROM library_entries
    //         """.trimIndent())
    //
    //         // 3. Drop old table
    //         db.execSQL("DROP TABLE library_entries")
    //
    //         // 4. Rename new table to original name
    //         db.execSQL("ALTER TABLE library_entries_new RENAME TO library_entries")
    //
    //         // 5. Recreate indices
    //         db.execSQL("CREATE INDEX IF NOT EXISTS index_library_entries_mediaType ON library_entries (mediaType)")
    //     }
    // }

    // ═══════════════════════════════════════════════════════════════════════════
    //                         AUTO-MIGRATION SPECS
    // ═══════════════════════════════════════════════════════════════════════════

    // ─────────────────────────────────────────────────────────────────────────────
    // EXAMPLE: Spec for renaming a column (use with AutoMigration annotation)
    // ─────────────────────────────────────────────────────────────────────────────
    //
    // @RenameColumn(tableName = "library_entries", fromColumnName = "oldName", toColumnName = "newName")
    // class AutoMigration4To5Spec : AutoMigrationSpec

    // ─────────────────────────────────────────────────────────────────────────────
    // EXAMPLE: Spec for deleting a column
    // ─────────────────────────────────────────────────────────────────────────────
    //
    // @DeleteColumn(tableName = "library_entries", columnName = "deprecatedColumn")
    // class AutoMigration5To6Spec : AutoMigrationSpec

    // ─────────────────────────────────────────────────────────────────────────────
    // EXAMPLE: Spec for renaming a table
    // ─────────────────────────────────────────────────────────────────────────────
    //
    // @RenameTable(fromTableName = "old_table", toTableName = "new_table")
    // class AutoMigration6To7Spec : AutoMigrationSpec
}
