package ink.duo3.tuned.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The DDL v1 -> v2 runs, kept as data so the JVM migration test can execute the exact
 * statements the production [MIGRATION_1_2] applies (MigrationTestHelper is Android-only
 * and won't run as a plain unit test).
 */
internal val MIGRATION_1_2_STATEMENTS =
    listOf(
        "ALTER TABLE podcasts ADD COLUMN title TEXT",
        "ALTER TABLE podcasts ADD COLUMN author TEXT",
        "ALTER TABLE podcasts ADD COLUMN description TEXT",
        "ALTER TABLE podcasts ADD COLUMN artworkUrl TEXT",
        "ALTER TABLE episodes ADD COLUMN title TEXT",
        "ALTER TABLE episodes ADD COLUMN description TEXT",
    )

/**
 * v1 -> v2 adds the display columns the library UI renders. All new columns are
 * nullable TEXT, so existing rows survive untouched; the next feed refresh backfills
 * them. Identity/sync columns are unchanged.
 */
val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MIGRATION_1_2_STATEMENTS.forEach(db::execSQL)
        }
    }
