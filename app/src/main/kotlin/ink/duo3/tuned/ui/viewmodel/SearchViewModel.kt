package ink.duo3.tuned.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.exception.HttpException
import com.prof18.rssparser.exception.RssParsingException
import ink.duo3.tuned.ui.state.SearchUIState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.UnknownHostException

enum class SearchState {
    IDLE,
    LOADING,
    SEARCHSUCCEEDED,
    RSSFETCHSUCCEEDED,
    SEARCHNOTFOUND,
    RSSNOTFOUND,
    NETWORKERROR,
}

class SearchViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUIState())
    val uiState = _uiState.asStateFlow()

    val rssParser = RssParser()

    fun onSearchFieldValueChanged(value: String) {
        _uiState.update {
            it.copy(
                searchFieldValue = value
            )
        }
    }

    fun onSearch() {
//        if (Patterns.WEB_URL.matcher(_uiState.value.searchFieldValue).matches()) {
//            searchWithRSSURL(_uiState.value.searchFieldValue)
//        } else {
//            searchWithText(_uiState.value.searchFieldValue)
//        }
        searchWithRSSURL(_uiState.value.searchFieldValue)
    }

    fun searchWithText(
        query: String
    ) {
        TODO()
    }

    private fun searchWithRSSURL(
        url: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { state ->
                state.copy(
                    searchState = SearchState.LOADING,
                )
            }

            val podcast = runCatching {
                rssParser.getRssChannel(url)
            }

            podcast.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        searchState = SearchState.RSSFETCHSUCCEEDED,
                        rssResult = it,
                    )
                }
            }.onFailure {
                when (it) {
                    is RssParsingException -> {
                        _uiState.update { state ->
                            state.copy(
                                searchState = SearchState.RSSNOTFOUND,
                            )
                        }
                    }

                    is HttpException -> {
                        if (it.code == 404)
                            _uiState.update { state ->
                                state.copy(
                                    searchState = SearchState.RSSNOTFOUND,
                                )
                            }
                        else
                            _uiState.update { state ->
                                state.copy(
                                    searchState = SearchState.NETWORKERROR,
                                    networkErrorMessage = "Unknown HTTP error"
                                )
                            }
                    }

                    is UnknownHostException -> {
                        _uiState.update { state ->
                            state.copy(
                                searchState = SearchState.NETWORKERROR,
                                networkErrorMessage = "No network connection"
                            )
                        }
                    }

                    else -> {
                        _uiState.update { state ->
                            state.copy(
                                searchState = SearchState.NETWORKERROR,
                                networkErrorMessage = "Unknown error"
                            )
                        }
                    }
                }
            }
        }
    }
}