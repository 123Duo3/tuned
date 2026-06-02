package ink.duo3.tuned.data.local

import ink.duo3.tuned.data.local.dao.RECENT_EPISODES_QUERY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.sql.Connection
import java.sql.DriverManager

/**
 * Verifies MIGRATION_1_2 against a real SQLite engine on the JVM.
 *
 * Room's [androidx.room.testing.MigrationTestHelper] needs an Android `Instrumentation`
 * (it's published only for the Android target), so it can't run as a plain unit test.
 * Instead we stand up the exact v1 schema with the xerial JDBC driver, seed a row, run
 * the production [MIGRATION_1_2_STATEMENTS], and assert the new columns appear, the
 * existing data survives the episodes-table recreate, and `enclosureUrl` is now nullable
 * (a text-only item with no audio must be storable) — the things this migration guarantees.
 */
class MigrationTest {
    // The v1 CREATE statements, copied verbatim from the checked-in schema export
    // (app/schemas/.../1.json) so the test starts from the real v1 shape.
    private val v1Podcasts =
        "CREATE TABLE IF NOT EXISTS `podcasts` (`id` TEXT NOT NULL, `canonicalFeedUrl` TEXT NOT NULL, " +
            "`currentFeedUrl` TEXT NOT NULL, `etag` TEXT, `lastModified` TEXT, " +
            "`lastFetchedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
    private val v1Episodes =
        "CREATE TABLE IF NOT EXISTS `episodes` (`id` TEXT NOT NULL, `podcastId` TEXT NOT NULL, " +
            "`guid` TEXT, `enclosureUrl` TEXT NOT NULL, `publishedAt` INTEGER NOT NULL, " +
            "`durationMs` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`podcastId`) REFERENCES " +
            "`podcasts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"

    @Test
    fun `migration 1 to 2 adds display columns and preserves existing rows`() {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { db ->
            db.createStatement().use {
                it.execute(v1Podcasts)
                it.execute(v1Episodes)
            }
            db.createStatement().use {
                it.execute(
                    "INSERT INTO podcasts(id, canonicalFeedUrl, currentFeedUrl, etag, lastModified, " +
                        "lastFetchedAt) VALUES('p1', 'https://feed', 'https://feed', '\"v1\"', null, 42)",
                )
                it.execute(
                    "INSERT INTO episodes(id, podcastId, guid, enclosureUrl, publishedAt, durationMs) " +
                        "VALUES('e1', 'p1', 'g1', 'https://audio.mp3', 100, 600)",
                )
            }

            MIGRATION_1_2_STATEMENTS.forEach { sql -> db.createStatement().use { it.execute(sql) } }

            assertTrue(db.columns("podcasts").containsAll(listOf("title", "author", "description", "artworkUrl")))
            assertTrue(db.columns("episodes").containsAll(listOf("title", "description")))

            db.createStatement().use { stmt ->
                stmt
                    .executeQuery("SELECT canonicalFeedUrl, etag, lastFetchedAt, title FROM podcasts WHERE id='p1'")
                    .use { rs ->
                        assertTrue(rs.next())
                        assertEquals("https://feed", rs.getString("canonicalFeedUrl"))
                        assertEquals("\"v1\"", rs.getString("etag"))
                        assertEquals(42L, rs.getLong("lastFetchedAt"))
                        assertNull(rs.getString("title"))
                    }
            }
            db.createStatement().use { stmt ->
                stmt.executeQuery("SELECT enclosureUrl, description FROM episodes WHERE id='e1'").use { rs ->
                    assertTrue(rs.next())
                    assertEquals("https://audio.mp3", rs.getString("enclosureUrl"))
                    assertNull(rs.getString("description"))
                }
            }

            // enclosureUrl is now nullable: a text-only item with no audio must store.
            assertEquals(0, db.notNull("episodes", "enclosureUrl"))
            db.createStatement().use {
                it.execute(
                    "INSERT INTO episodes(id, podcastId, guid, enclosureUrl, publishedAt, durationMs) " +
                        "VALUES('e2', 'p1', 'g2', NULL, 200, NULL)",
                )
            }
            db.createStatement().use { stmt ->
                stmt.executeQuery("SELECT enclosureUrl FROM episodes WHERE id='e2'").use { rs ->
                    assertTrue(rs.next())
                    assertNull(rs.getString("enclosureUrl"))
                }
            }
        }
    }

    @Test
    fun `migration 2 to 3 adds episode artwork column and preserves rows`() {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { db ->
            db.createStatement().use {
                it.execute(v1Podcasts)
                // The v2 episodes shape: enclosureUrl nullable, plus title/description.
                it.execute(
                    "CREATE TABLE IF NOT EXISTS `episodes` (`id` TEXT NOT NULL, `podcastId` TEXT NOT NULL, " +
                        "`guid` TEXT, `enclosureUrl` TEXT, `publishedAt` INTEGER NOT NULL, `durationMs` INTEGER, " +
                        "`title` TEXT, `description` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`podcastId`) REFERENCES " +
                        "`podcasts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                )
            }
            db.createStatement().use {
                it.execute(
                    "INSERT INTO podcasts(id, canonicalFeedUrl, currentFeedUrl, etag, lastModified, " +
                        "lastFetchedAt) VALUES('p1', 'https://feed', 'https://feed', null, null, 7)",
                )
                it.execute(
                    "INSERT INTO episodes(id, podcastId, guid, enclosureUrl, publishedAt, durationMs, title) " +
                        "VALUES('e1', 'p1', 'g1', 'https://audio.mp3', 100, 600, 'Episode One')",
                )
            }

            MIGRATION_2_3_STATEMENTS.forEach { sql -> db.createStatement().use { it.execute(sql) } }

            assertTrue(db.columns("episodes").contains("artworkUrl"))
            db.createStatement().use { stmt ->
                stmt.executeQuery("SELECT title, artworkUrl FROM episodes WHERE id='e1'").use { rs ->
                    assertTrue(rs.next())
                    assertEquals("Episode One", rs.getString("title"))
                    // Pre-existing rows get NULL art; the UI falls back to podcast artwork.
                    assertNull(rs.getString("artworkUrl"))
                }
            }
        }
    }

    @Test
    fun `migration 3 to 4 indexes recent episodes query`() {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { db ->
            db.createStatement().use {
                it.execute(
                    "CREATE TABLE podcasts (`id` TEXT NOT NULL, `title` TEXT, `artworkUrl` TEXT, " +
                        "PRIMARY KEY(`id`))",
                )
                it.execute(
                    "CREATE TABLE episodes (`id` TEXT NOT NULL, `podcastId` TEXT NOT NULL, `guid` TEXT, " +
                        "`enclosureUrl` TEXT, `publishedAt` INTEGER NOT NULL, `durationMs` INTEGER, `title` TEXT, " +
                        "`description` TEXT, `artworkUrl` TEXT, PRIMARY KEY(`id`))",
                )
                it.execute("INSERT INTO podcasts(id, title, artworkUrl) VALUES('p1', 'Podcast', 'podcast.jpg')")
                it.execute(
                    "INSERT INTO episodes(id, podcastId, publishedAt, title, artworkUrl) " +
                        "VALUES('older', 'p1', 100, 'Older', 'episode.jpg'), ('newer', 'p1', 200, 'Newer', NULL)",
                )
            }

            MIGRATION_3_4_STATEMENTS.forEach { sql -> db.createStatement().use { it.execute(sql) } }

            assertTrue(db.indexes("episodes").contains("index_episodes_publishedAt"))
            db.createStatement().use { stmt ->
                stmt.executeQuery(RECENT_EPISODES_QUERY.replace(":limit", "1")).use { rs ->
                    assertTrue(rs.next())
                    assertEquals("newer", rs.getString("id"))
                    assertNull(rs.getString("artworkUrl"))
                    assertEquals("podcast.jpg", rs.getString("podcastArtworkUrl"))
                    assertFalse(rs.next())
                }
            }
            assertTrue(
                db
                    .queryPlan(RECENT_EPISODES_QUERY.replace(":limit", "30"))
                    .any { it.contains("index_episodes_publishedAt") },
            )
        }
    }

    private fun Connection.columns(table: String): List<String> =
        createStatement().use { stmt ->
            stmt.executeQuery("PRAGMA table_info(`$table`)").use { rs ->
                buildList { while (rs.next()) add(rs.getString("name")) }
            }
        }

    // PRAGMA table_info exposes a `notnull` flag (1 = NOT NULL, 0 = nullable) per column.
    private fun Connection.notNull(
        table: String,
        column: String,
    ): Int =
        createStatement()
            .use { stmt ->
                stmt.executeQuery("PRAGMA table_info(`$table`)").use { rs ->
                    buildMap { while (rs.next()) put(rs.getString("name"), rs.getInt("notnull")) }
                }
            }.getValue(column)

    private fun Connection.indexes(table: String): List<String> =
        createStatement().use { stmt ->
            stmt.executeQuery("PRAGMA index_list(`$table`)").use { rs ->
                buildList { while (rs.next()) add(rs.getString("name")) }
            }
        }

    private fun Connection.queryPlan(query: String): List<String> =
        createStatement().use { stmt ->
            stmt.executeQuery("EXPLAIN QUERY PLAN $query").use { rs ->
                buildList { while (rs.next()) add(rs.getString("detail")) }
            }
        }
}
