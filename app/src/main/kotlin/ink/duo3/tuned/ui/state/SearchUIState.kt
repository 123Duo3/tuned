package ink.duo3.tuned.ui.state

import com.prof18.rssparser.model.RssChannel

data class SearchUIState(
    val isLoading: Boolean = false,
    val searchFieldValue: String = "",
    val searchResult: List<RssChannel> = emptyList(),
    val rssResult: RssChannel? = null,
    val rssNotFound: Boolean = false
)