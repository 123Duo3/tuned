package ink.duo3.tuned.data.local

import ink.duo3.tuned.data.local.entity.EpisodeEntity
import ink.duo3.tuned.data.model.ParsedEpisode

/**
 * Turns raw [ParsedEpisode]s into persistable [EpisodeEntity]s for one podcast.
 * The mapper is the chokepoint where feed noise is rejected:
 * - items with no identity signal at all (see [FeedIdentity.episodeId]) are dropped —
 *   without a guid, enclosure, or title+date there is nothing stable to key on;
 * - an item with no audio (`enclosureUrl == null`) is still kept as long as it has
 *   some identity: a text-only announcement should surface, not silently vanish;
 * - duplicate ids collapse to one entity, keeping the newer by `publishedAt` (RSS
 *   does not guarantee item order, so position can't be trusted); ties keep the
 *   first seen. Each dropped duplicate is counted in [Mapping.skipped].
 *
 * Output preserves first-seen order; [Mapping.skipped] lets the caller log how many
 * items were discarded.
 */
object EpisodeMapper {
    data class Mapping(
        val episodes: List<EpisodeEntity>,
        val skipped: Int,
    )

    fun map(
        podcastId: String,
        items: List<ParsedEpisode>,
    ): Mapping {
        val byId = linkedMapOf<String, EpisodeEntity>()
        var skipped = 0
        for (item in items) {
            val entity = toEntity(podcastId, item)
            if (entity == null) {
                skipped++
                continue
            }
            if (byId.containsKey(entity.id)) skipped++
            byId.keepNewer(entity)
        }
        return Mapping(byId.values.toList(), skipped)
    }

    private fun MutableMap<String, EpisodeEntity>.keepNewer(entity: EpisodeEntity) {
        val existing = this[entity.id]
        if (existing == null || entity.publishedAt > existing.publishedAt) {
            this[entity.id] = entity
        }
    }

    private fun toEntity(
        podcastId: String,
        item: ParsedEpisode,
    ): EpisodeEntity? {
        val id =
            FeedIdentity.episodeId(
                podcastId = podcastId,
                guid = item.guid,
                enclosureUrl = item.enclosureUrl,
                title = item.title,
                publishedAtMs = item.publishedAtMs,
            )
        return id?.let {
            EpisodeEntity(
                id = it,
                podcastId = podcastId,
                guid = item.guid,
                enclosureUrl = item.enclosureUrl,
                publishedAt = item.publishedAtMs ?: 0L,
                durationMs = item.durationMs,
                title = item.title,
                description = item.description,
                artworkUrl = item.artworkUrl,
                chaptersUrl = item.chaptersUrl,
            )
        }
    }
}
