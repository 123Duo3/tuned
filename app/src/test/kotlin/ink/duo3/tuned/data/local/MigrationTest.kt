package ink.duo3.tuned.data.local

import org.junit.Assert.assertEquals
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
 * the production [MIGRATION_1_2_STATEMENTS], and assert the new columns appear while the
 * existing data survives — the two things this migration must guarantee.
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
        }
    }

    private fun Connection.columns(table: String): List<String> =
        createStatement().use { stmt ->
            stmt.executeQuery("PRAGMA table_info(`$table`)").use { rs ->
                buildList { while (rs.next()) add(rs.getString("name")) }
            }
        }
}
