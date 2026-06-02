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
        // podcasts: plain additive columns the library UI renders.
        "ALTER TABLE podcasts ADD COLUMN title TEXT",
        "ALTER TABLE podcasts ADD COLUMN author TEXT",
        "ALTER TABLE podcasts ADD COLUMN description TEXT",
        "ALTER TABLE podcasts ADD COLUMN artworkUrl TEXT",
        // episodes: a plain ADD COLUMN can't relax enclosureUrl from NOT NULL to
        // nullable, so recreate the table with the v2 shape and copy rows across.
        "CREATE TABLE IF NOT EXISTS `episodes_new` (`id` TEXT NOT NULL, `podcastId` TEXT NOT NULL, " +
            "`guid` TEXT, `enclosureUrl` TEXT, `publishedAt` INTEGER NOT NULL, `durationMs` INTEGER, " +
            "`title` TEXT, `description` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`podcastId`) REFERENCES " +
            "`podcasts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "INSERT INTO `episodes_new` (`id`, `podcastId`, `guid`, `enclosureUrl`, `publishedAt`, `durationMs`) " +
            "SELECT `id`, `podcastId`, `guid`, `enclosureUrl`, `publishedAt`, `durationMs` FROM `episodes`",
        "DROP TABLE `episodes`",
        "ALTER TABLE `episodes_new` RENAME TO `episodes`",
        "CREATE INDEX IF NOT EXISTS `index_episodes_podcastId` ON `episodes` (`podcastId`)",
    )

/**
 * v1 -> v2 adds the display columns the library UI renders. The podcasts columns are
 * plain additive TEXT. The episodes table is recreated so `enclosureUrl` can become
 * nullable (a text-only item with no audio should still be listed) and the new
 * title/description columns appear; existing rows are copied across unchanged.
 */
val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MIGRATION_1_2_STATEMENTS.forEach(db::execSQL)
        }
    }

/** The DDL v2 -> v3 runs (a single additive column), exposed for the JVM migration test. */
internal val MIGRATION_2_3_STATEMENTS =
    listOf(
        // episodes.artworkUrl holds item-level <itunes:image> art; plain additive TEXT.
        "ALTER TABLE episodes ADD COLUMN artworkUrl TEXT",
    )

/**
 * v2 -> v3 adds the per-episode artwork URL. A plain additive nullable column, so
 * existing rows simply get NULL (the UI falls back to the podcast's artwork).
 */
val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MIGRATION_2_3_STATEMENTS.forEach(db::execSQL)
        }
    }
