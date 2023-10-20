package ink.duo3.tuned.ui.state

import com.prof18.rssparser.model.RssChannel
import ink.duo3.tuned.ui.viewmodel.SearchState

data class SearchUIState(
    val searchFieldValue: String = "",
    val searchResult: List<RssChannel> = emptyList(),
    val rssResult: RssChannel? = null,
    val searchState: SearchState = SearchState.IDLE,
    val networkErrorMessage: String = "",
)