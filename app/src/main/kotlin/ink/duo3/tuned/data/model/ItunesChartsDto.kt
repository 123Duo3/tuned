package ink.duo3.tuned.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire shape of the iTunes "top podcasts" RSS-as-JSON charts endpoint. It only carries
 * collection ids (and display fluff the app ignores); the feed URL needed to subscribe is
 * resolved in a second `lookup` call. Kept isolated so the chart's idiosyncratic nested
 * `id.attributes.im:id` shape never leaks into the domain — the repository maps it to ids.
 *
 * Note: the endpoint collapses `feed.entry` to a single object (not an array) when the limit
 * is 1; the app always requests many, so a list is safe in practice.
 */
@Serializable
data class ItunesChartsResponse(
    val feed: ItunesChartsFeed = ItunesChartsFeed(),
)

@Serializable
data class ItunesChartsFeed(
    val entry: List<ItunesChartsEntry> = emptyList(),
)

@Serializable
data class ItunesChartsEntry(
    val id: ItunesChartsId = ItunesChartsId(),
)

@Serializable
data class ItunesChartsId(
    val attributes: ItunesChartsIdAttributes = ItunesChartsIdAttributes(),
)

@Serializable
data class ItunesChartsIdAttributes(
    @SerialName("im:id") val imId: String? = null,
)
